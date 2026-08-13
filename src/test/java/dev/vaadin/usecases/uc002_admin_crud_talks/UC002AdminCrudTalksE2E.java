package dev.vaadin.usecases.uc002_admin_crud_talks;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;

import dev.vaadin.talk.domain.Talk;
import dev.vaadin.talk.domain.TalkType;
import dev.vaadin.usecases.e2e.PlaywrightE2ETest;

/**
 * Browser-driven tests for UC-002: Admin CRUD Operations for Talks.
 *
 * <p>Mirrors {@link UC002AdminCrudTalks}. The browserless class proves the view
 * logic and the business rules on the JVM; this one drives the same flows through
 * a real browser, including the grid, the modal form, the confirmation dialog and
 * the notifications.
 *
 * @see <a href="../../../../../../spec/use-cases/use-case-002-admin-crud-talks.md">
 *      spec/use-cases/use-case-002-admin-crud-talks.md</a>
 */
class UC002AdminCrudTalksE2E extends PlaywrightE2ETest {

    private static final String EXISTING_TITLE = "Signals in Vaadin";
    private static final String OTHER_TITLE = "Accessibility Clinic";

    private static final DateTimeFormatter DATE_INPUT =
            DateTimeFormatter.ofPattern("M/d/yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_INPUT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private LocalDateTime future;

    @BeforeEach
    void seedCatalog() {
        future = futureDate(7);
        givenTalks(List.of(
                new Talk(EXISTING_TITLE, "Reactive state management for server-side UIs.",
                        "Amara Osei", TalkType.PRESENTATION, future, 45, "Main Hall"),
                new Talk(OTHER_TITLE, "Keyboard navigation and screen readers.",
                        "Lena Hartmann", TalkType.WORKSHOP, future.plusDays(1), 120,
                        "Workshop Room B")));
    }

    // ------------------------------------------------------------ Main Flow

    @Test
    @DisplayName("Main Flow 1-8: an administrator creates a talk and sees it in the grid")
    void mainFlow_createTalk() {
        open("/admin");
        assertEquals(2, gridRowCount());

        page.getByTestId("create-talk").click();
        assertThat(formDialog()).isVisible();

        LocalDateTime scheduled = future.plusDays(10).withHour(10).withMinute(30);
        fillForm("Observability for Vaadin Apps", "Tracing, metrics and structured logging.",
                "Nadia Brennan", "Presentation", scheduled, 50, "Room 301");
        page.getByTestId("save-talk").click();

        // Step 7: the form closes and the list shows the new talk.
        assertThat(formDialog()).isHidden();
        assertThat(gridCell("Observability for Vaadin Apps")).isVisible();
        assertEquals(3, gridRowCount());

        // Step 8: a success notification.
        assertThat(notification("Talk created")).isVisible();

        // Postcondition: persisted with every field.
        Talk saved = findByTitle("Observability for Vaadin Apps").orElseThrow();
        assertEquals("Nadia Brennan", saved.getSpeakerName());
        assertEquals(TalkType.PRESENTATION, saved.getType());
        assertEquals(scheduled, saved.getScheduledDate());
        assertEquals(50, saved.getDuration());
        assertEquals("Room 301", saved.getLocation());
    }

    // ---------------------------------------------------- Alternative Flows

    @Test
    @DisplayName("AF-1: editing a talk updates the grid and the database")
    void af1_editExistingTalk() {
        open("/admin");

        editButtonFor(EXISTING_TITLE).click();
        assertThat(formDialog()).isVisible();
        // AF-1 step 1: the form is pre-populated.
        assertThat(field("form-title")).hasValue(EXISTING_TITLE);
        assertThat(field("form-speaker")).hasValue("Amara Osei");

        field("form-title").fill("Signals in Vaadin 25");
        field("form-duration").fill("60");
        page.getByTestId("save-talk").click();

        assertThat(formDialog()).isHidden();
        assertThat(gridCell("Signals in Vaadin 25")).isVisible();
        assertEquals(2, gridRowCount(), "editing must not add a row");
        assertThat(notification("Talk updated")).isVisible();

        assertEquals(60, findByTitle("Signals in Vaadin 25").orElseThrow().getDuration());
    }

    @Test
    @DisplayName("AF-2: deleting a talk after confirming removes it")
    void af2_deleteTalkAfterConfirmation() {
        open("/admin");

        deleteButtonFor(EXISTING_TITLE).click();

        // AF-2 step 1: the administrator is asked to confirm.
        assertThat(confirmDialog()).isVisible();

        confirmDeleteButton().click();

        assertThat(gridCell(EXISTING_TITLE)).isHidden();
        assertEquals(1, gridRowCount());
        assertThat(notification("Talk deleted")).isVisible();
        assertTrue(findByTitle(EXISTING_TITLE).isEmpty(), "the talk should be gone");
    }

    @Test
    @DisplayName("AF-3: cancelling the confirmation keeps the talk")
    void af3_cancelDelete() {
        open("/admin");

        deleteButtonFor(EXISTING_TITLE).click();
        assertThat(confirmDialog()).isVisible();

        cancelDeleteButton().click();

        assertThat(confirmDialog()).isHidden();
        assertEquals(2, gridRowCount(), "nothing should have been deleted");
        assertThat(gridCell(EXISTING_TITLE)).isVisible();
        assertTrue(findByTitle(EXISTING_TITLE).isPresent());
    }

    @Test
    @DisplayName("AF-4: an invalid field keeps the form open and shows its error")
    void af4_validationError_keepsFormOpenAndReportsField() {
        open("/admin");
        page.getByTestId("create-talk").click();

        // Everything valid except the title.
        fillForm("", "A description.", "A Speaker", "Workshop",
                future.plusDays(3).withHour(9).withMinute(0), 60, "Room 1");
        page.getByTestId("save-talk").click();

        assertThat(formDialog()).isVisible();
        assertThat(fieldError("form-title", "Title is required")).isVisible();
        assertEquals(2, gridRowCount(), "nothing should have been persisted");

        // AF-4 step 2: correcting the error and saving again succeeds.
        field("form-title").fill("A Corrected Title");
        page.getByTestId("save-talk").click();

        assertThat(formDialog()).isHidden();
        assertThat(gridCell("A Corrected Title")).isVisible();
        assertEquals(3, gridRowCount());
    }

    @Test
    @DisplayName("AF-5: cancelling the form discards the changes")
    void af5_cancelForm_discardsChanges() {
        open("/admin");

        editButtonFor(EXISTING_TITLE).click();
        field("form-title").fill("This Should Never Be Saved");
        page.getByTestId("cancel-talk").click();

        assertThat(formDialog()).isHidden();
        assertEquals(2, gridRowCount());
        assertThat(gridCell(EXISTING_TITLE)).isVisible();
        assertTrue(findByTitle("This Should Never Be Saved").isEmpty());
    }

    // ------------------------------------------------------- Business Rules

    @Test
    @DisplayName("BR-01: every field is mandatory")
    void br01_allFieldsAreMandatory() {
        open("/admin");
        page.getByTestId("create-talk").click();

        page.getByTestId("save-talk").click();

        assertThat(formDialog()).isVisible();
        Map.of("form-title", "Title is required",
                        "form-description", "Description is required",
                        "form-speaker", "Speaker name is required",
                        "form-type", "Type is required",
                        "form-scheduled-date", "Scheduled date is required",
                        "form-duration", "Duration is required",
                        "form-location", "Location is required")
                .forEach((testId, message) ->
                        assertThat(fieldError(testId, message)).isVisible());
        assertEquals(2, gridRowCount(), "an empty form must not create anything");
    }

    @Test
    @DisplayName("BR-02: title and speaker must contain more than whitespace")
    void br02_titleAndSpeakerMustBeNonEmptyText() {
        open("/admin");
        page.getByTestId("create-talk").click();

        fillForm("   ", "A description.", "   ", "Presentation",
                future.plusDays(3).withHour(9).withMinute(0), 30, "Room 1");
        page.getByTestId("save-talk").click();

        assertThat(formDialog()).isVisible();
        assertThat(fieldError("form-title", "Title is required")).isVisible();
        assertThat(fieldError("form-speaker", "Speaker name is required")).isVisible();
        assertEquals(2, gridRowCount());
    }

    @Test
    @DisplayName("BR-03: duration must be a positive number of minutes")
    void br03_durationMustBePositive() {
        open("/admin");
        page.getByTestId("create-talk").click();

        fillForm("Zero Length", "A description.", "A Speaker", "Presentation",
                future.plusDays(3).withHour(9).withMinute(0), 0, "Room 1");
        page.getByTestId("save-talk").click();

        assertThat(fieldError("form-duration", "Duration must be a positive number of minutes"))
                .isVisible();
        assertEquals(2, gridRowCount());
    }

    @Test
    @DisplayName("BR-04: a new talk must be scheduled in the future")
    void br04_scheduledDateMustBeInTheFuture() {
        open("/admin");
        page.getByTestId("create-talk").click();

        fillForm("Yesterday's Talk", "A description.", "A Speaker", "Presentation",
                LocalDateTime.now().minusDays(1).withHour(9).withMinute(0), 30, "Room 1");
        page.getByTestId("save-talk").click();

        assertThat(fieldError("form-scheduled-date", "Scheduled date must be in the future"))
                .isVisible();
        assertEquals(2, gridRowCount());
    }

    @Test
    @DisplayName("BR-04: an already-past talk stays editable as long as its date is untouched")
    void br04_pastTalkRemainsEditableWhenDateIsUnchanged() {
        LocalDateTime past = LocalDateTime.now().minusMonths(3).withSecond(0).withNano(0);
        givenTalks(List.of(new Talk("Last Year's Keynote", "A description.", "A Speaker",
                TalkType.PRESENTATION, past, 60, "Main Hall")));

        open("/admin");
        editButtonFor("Last Year's Keynote").click();
        assertThat(formDialog()).isVisible();

        // Change only the speaker, leaving the past date exactly as stored.
        field("form-speaker").fill("A Different Speaker");
        page.getByTestId("save-talk").click();

        assertThat(formDialog()).isHidden();
        assertThat(notification("Talk updated")).isVisible();
        assertEquals("A Different Speaker",
                findByTitle("Last Year's Keynote").orElseThrow().getSpeakerName());
        assertEquals(past, findByTitle("Last Year's Keynote").orElseThrow().getScheduledDate());
    }

    @Test
    @DisplayName("BR-05: the type can only be Presentation or Workshop")
    void br05_typeMustBeOneOfTheTwoEnumValues() {
        open("/admin");
        page.getByTestId("create-talk").click();

        page.getByTestId("form-type").click();
        assertThat(typeOptions()).hasCount(2);
        assertEquals(List.of("Presentation", "Workshop"),
                typeOptions().allInnerTexts().stream().map(String::trim).toList());
        page.keyboard().press("Escape");

        // Leaving it unselected is rejected.
        fillForm("No Type", "A description.", "A Speaker", null,
                future.plusDays(3).withHour(9).withMinute(0), 30, "Room 1");
        page.getByTestId("save-talk").click();

        assertThat(fieldError("form-type", "Type is required")).isVisible();
        assertEquals(2, gridRowCount());
    }

    @Test
    @DisplayName("BR-06: a talk is never deleted without confirmation")
    void br06_deleteRequiresConfirmation() {
        open("/admin");

        deleteButtonFor(EXISTING_TITLE).click();

        // The click alone must not delete anything — only the confirmation does.
        assertThat(confirmDialog()).isVisible();
        assertEquals(2, gridRowCount(), "the talk must survive until the deletion is confirmed");
        assertTrue(findByTitle(EXISTING_TITLE).isPresent());
    }

    @Test
    @DisplayName("BR-07: validation errors are shown next to the field they belong to")
    void br07_validationErrorsAreShownPerField() {
        open("/admin");
        page.getByTestId("create-talk").click();

        page.getByTestId("save-talk").click();

        // Each message must render inside its own field, not as a shared summary.
        assertThat(fieldError("form-title", "Title is required")).isVisible();
        assertThat(fieldError("form-speaker", "Speaker name is required")).isVisible();
        assertThat(fieldError("form-duration", "Duration is required")).isVisible();
        assertThat(fieldError("form-location", "Location is required")).isVisible();
    }

    // -------------------------------------------------------------- Helpers

    /**
     * Whether the form is open, judged by its first field.
     *
     * <p>{@code vaadin-dialog} itself is a non-rendering host, so its own
     * visibility says nothing about whether the user can see the form.
     */
    private Locator formDialog() {
        return page.getByTestId("form-title");
    }

    private Locator field(String testId) {
        return page.getByTestId(testId).locator("input, textarea").first();
    }

    /**
     * A validation message shown on a specific field.
     *
     * <p>Scoped to the field on purpose: Vaadin also mirrors the message into a
     * page-level {@code aria-live} region, so an unscoped text match is ambiguous
     * — and "the message appears next to the field it belongs to" is the actual
     * rule (BR-07) anyway.
     */
    private Locator fieldError(String testId, String message) {
        return page.getByTestId(testId).getByText(message);
    }

    /**
     * The confirmation prompt as the administrator sees it. Like
     * {@code vaadin-dialog}, {@code vaadin-confirm-dialog} is a non-rendering
     * host, so the message itself is what proves the dialog is on screen.
     */
    private Locator confirmDialog() {
        return page.getByText("Are you sure you want to delete this talk?");
    }

    private Locator confirmDeleteButton() {
        return page.locator("vaadin-confirm-dialog vaadin-button[slot='confirm-button']");
    }

    private Locator cancelDeleteButton() {
        return page.locator("vaadin-confirm-dialog vaadin-button[slot='cancel-button']");
    }

    /** The selectable type options, excluding the placeholder in the value button. */
    private Locator typeOptions() {
        return page.locator("vaadin-select-list-box vaadin-select-item");
    }

    private void fillForm(String title, String description, String speaker, String type,
            LocalDateTime scheduled, Integer duration, String location) {
        field("form-title").fill(title);
        page.getByTestId("form-description").locator("textarea").fill(description);
        field("form-speaker").fill(speaker);
        if (type != null) {
            page.getByTestId("form-type").click();
            typeOptions().filter(new Locator.FilterOptions().setHasText(type)).first().click();
        }
        page.getByTestId("form-scheduled-date").locator("vaadin-date-picker input")
                .fill(scheduled.format(DATE_INPUT));
        page.keyboard().press("Enter");
        page.getByTestId("form-scheduled-date").locator("vaadin-time-picker input")
                .fill(scheduled.format(TIME_INPUT));
        page.keyboard().press("Enter");
        field("form-duration").fill(String.valueOf(duration));
        field("form-location").fill(location);
        waitForVaadin();
    }

    /** A grid cell showing the given text. Grid cells render into light-DOM slots. */
    private Locator gridCell(String text) {
        return page.locator("vaadin-grid-cell-content")
                .filter(new Locator.FilterOptions().setHasText(text))
                .first();
    }

    private int gridRowCount() {
        return ((Number) page.getByTestId("talk-grid").evaluate("g => g.size")).intValue();
    }

    private Locator editButtonFor(String talkTitle) {
        return page.locator("vaadin-button[aria-label=\"Edit " + talkTitle + "\"]");
    }

    private Locator deleteButtonFor(String talkTitle) {
        return page.locator("vaadin-button[aria-label=\"Delete " + talkTitle + "\"]");
    }

    private Locator notification(String text) {
        return page.locator("vaadin-notification-card")
                .filter(new Locator.FilterOptions().setHasText(text));
    }

    private Optional<Talk> findByTitle(String title) {
        return repository.findAll().stream()
                .filter(talk -> talk.getTitle().equals(title))
                .findFirst();
    }
}
