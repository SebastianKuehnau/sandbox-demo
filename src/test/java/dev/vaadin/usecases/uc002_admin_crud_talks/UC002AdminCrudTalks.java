package dev.vaadin.usecases.uc002_admin_crud_talks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import dev.vaadin.talk.domain.Talk;
import dev.vaadin.talk.domain.TalkRepository;
import dev.vaadin.talk.domain.TalkType;
import dev.vaadin.talk.service.InvalidTalkException;
import dev.vaadin.talk.service.TalkService;
import dev.vaadin.talk.ui.TalkAdminView;
import dev.vaadin.talk.ui.TalkFormDialog;
import jakarta.validation.ConstraintViolationException;

/**
 * Tests for UC-002: Admin CRUD Operations for Talks.
 *
 * @see <a href="../../../../../../spec/use-cases/use-case-002-admin-crud-talks.md">
 *      spec/use-cases/use-case-002-admin-crud-talks.md</a>
 */
@SpringBootTest
@ActiveProfiles("test")
class UC002AdminCrudTalks extends SpringBrowserlessTest {

    private static final String EXISTING_TITLE = "Signals in Vaadin";
    private static final String OTHER_TITLE = "Accessibility Clinic";

    @Autowired
    private TalkRepository repository;

    @Autowired
    private TalkService talkService;

    private LocalDateTime future;

    @BeforeEach
    void seedCatalog() {
        repository.deleteAll();
        future = LocalDateTime.now().plusDays(7).withNano(0).withSecond(0);
        repository.saveAll(List.of(
                new Talk(EXISTING_TITLE, "Reactive state management for server-side UIs.",
                        "Amara Osei", TalkType.PRESENTATION, future, 45, "Main Hall"),
                new Talk(OTHER_TITLE, "Keyboard navigation and screen readers.",
                        "Lena Hartmann", TalkType.WORKSHOP, future.plusDays(1), 120,
                        "Workshop Room B")));
    }

    // ------------------------------------------------------------ Main Flow

    @Test
    @DisplayName("Main Flow 1-8: an administrator creates a talk and sees it in the list")
    void mainFlow_createTalk() {
        navigate(TalkAdminView.class);
        assertEquals(2, gridSize(), "the seeded talks should be listed");

        test(createTalkButton()).click();

        LocalDateTime scheduled = future.plusDays(10);
        fillForm("Testing Without a Real UI", "How to run browserless tests on the JVM.",
                "Marcus Feldt", TalkType.PRESENTATION, scheduled, 45, "Room 204");
        test(saveButton()).click();

        // The form closed and the list refreshed with the new talk (step 7).
        assertFalse(formIsOpen(), "the form should close after a successful save");
        assertEquals(3, gridSize());
        assertTrue(gridTitles().contains("Testing Without a Real UI"));

        // Step 8: a success notification.
        assertEquals("Talk created", latestNotificationText());

        // Postcondition: the talk really is in the catalog, with all its fields.
        Talk saved = repository.findAll().stream()
                .filter(talk -> talk.getTitle().equals("Testing Without a Real UI"))
                .findFirst().orElseThrow();
        assertEquals("Marcus Feldt", saved.getSpeakerName());
        assertEquals(TalkType.PRESENTATION, saved.getType());
        assertEquals(scheduled, saved.getScheduledDate());
        assertEquals(45, saved.getDuration());
        assertEquals("Room 204", saved.getLocation());
    }

    // ---------------------------------------------------- Alternative Flows

    @Test
    @DisplayName("AF-1: editing a talk updates it in the list and in the database")
    void af1_editExistingTalk() {
        navigate(TalkAdminView.class);

        test(editButtonFor(EXISTING_TITLE)).click();
        assertTrue(formIsOpen());
        // The form is pre-populated with the selected talk (AF-1 step 1).
        assertEquals(EXISTING_TITLE, titleField().getValue());
        assertEquals("Amara Osei", speakerField().getValue());

        test(titleField()).setValue("Signals in Vaadin 25");
        test(durationField()).setValue(60);
        test(saveButton()).click();

        assertFalse(formIsOpen());
        assertEquals(2, gridSize(), "editing must not add a row");
        assertTrue(gridTitles().contains("Signals in Vaadin 25"));
        assertEquals("Talk updated", latestNotificationText());

        Talk updated = findByTitle("Signals in Vaadin 25").orElseThrow();
        assertEquals(60, updated.getDuration());
    }

    @Test
    @DisplayName("AF-2: deleting a talk after confirming removes it")
    void af2_deleteTalkAfterConfirmation() {
        navigate(TalkAdminView.class);

        test(deleteButtonFor(EXISTING_TITLE)).click();

        ConfirmDialog dialog = find(ConfirmDialog.class).single();
        assertEquals("Are you sure you want to delete this talk?", test(dialog).getText());

        test(dialog).confirm();

        assertEquals(1, gridSize());
        assertIterableEquals(List.of(OTHER_TITLE), gridTitles());
        assertEquals("Talk deleted", latestNotificationText());
        assertTrue(findByTitle(EXISTING_TITLE).isEmpty(), "the talk should be gone");
    }

    @Test
    @DisplayName("AF-3: cancelling the confirmation keeps the talk")
    void af3_cancelDelete() {
        navigate(TalkAdminView.class);

        test(deleteButtonFor(EXISTING_TITLE)).click();
        test(find(ConfirmDialog.class).single()).cancel();

        assertEquals(2, gridSize(), "nothing should have been deleted");
        assertTrue(findByTitle(EXISTING_TITLE).isPresent());
    }

    @Test
    @DisplayName("AF-4: an invalid field keeps the form open and shows its error")
    void af4_validationError_keepsFormOpenAndReportsField() {
        navigate(TalkAdminView.class);
        test(createTalkButton()).click();

        // Everything valid except the title.
        fillForm("", "A description.", "A Speaker", TalkType.WORKSHOP,
                future.plusDays(3), 60, "Room 1");
        test(saveButton()).click();

        assertTrue(formIsOpen(), "the form must stay open while input is invalid");
        assertTrue(titleField().isInvalid(), "the title field should be marked invalid");
        assertEquals("Title is required", titleField().getErrorMessage());
        assertEquals(2, gridSize(), "nothing should have been persisted");

        // AF-4 step 2: correcting the error and saving again succeeds.
        test(titleField()).setValue("A Corrected Title");
        test(saveButton()).click();

        assertFalse(formIsOpen());
        assertEquals(3, gridSize());
        assertTrue(gridTitles().contains("A Corrected Title"));
    }

    @Test
    @DisplayName("AF-5: cancelling the form discards the changes")
    void af5_cancelForm_discardsChanges() {
        navigate(TalkAdminView.class);

        test(editButtonFor(EXISTING_TITLE)).click();
        test(titleField()).setValue("This Should Never Be Saved");
        test(cancelButton()).click();

        assertFalse(formIsOpen());
        assertEquals(2, gridSize());
        assertIterableEquals(List.of(EXISTING_TITLE, OTHER_TITLE), gridTitles());
        assertTrue(findByTitle("This Should Never Be Saved").isEmpty());
    }

    // ------------------------------------------------------- Business Rules

    @Test
    @DisplayName("BR-01: every field is mandatory")
    void br01_allFieldsAreMandatory() {
        navigate(TalkAdminView.class);
        test(createTalkButton()).click();

        // Save an entirely empty form.
        test(saveButton()).click();

        assertTrue(formIsOpen());
        assertEquals(2, gridSize(), "an empty form must not create anything");
        for (HasValidation field : List.of(titleField(), descriptionField(), speakerField(),
                typeField(), scheduledDateField(), durationField(), locationField())) {
            assertTrue(field.isInvalid(),
                    () -> "every mandatory field should be flagged, but one was not: " + field);
        }
    }

    @Test
    @DisplayName("BR-02: title and speaker must contain more than whitespace")
    void br02_titleAndSpeakerMustBeNonEmptyText() {
        navigate(TalkAdminView.class);
        test(createTalkButton()).click();

        fillForm("   ", "A description.", "   ", TalkType.PRESENTATION,
                future.plusDays(3), 30, "Room 1");
        test(saveButton()).click();

        assertTrue(formIsOpen());
        assertTrue(titleField().isInvalid(), "a whitespace-only title is not text");
        assertTrue(speakerField().isInvalid(), "a whitespace-only speaker name is not text");
        assertEquals(2, gridSize());
    }

    @Test
    @DisplayName("BR-03: duration must be a positive number of minutes")
    void br03_durationMustBePositive() {
        navigate(TalkAdminView.class);
        test(createTalkButton()).click();

        fillForm("Zero Length", "A description.", "A Speaker", TalkType.PRESENTATION,
                future.plusDays(3), 0, "Room 1");
        test(saveButton()).click();

        assertTrue(durationField().isInvalid());
        assertEquals("Duration must be a positive number of minutes",
                durationField().getErrorMessage());
        assertEquals(2, gridSize());

        // The service rejects it too, independently of the form.
        Talk invalid = new Talk("Zero Length", "A description.", "A Speaker",
                TalkType.PRESENTATION, future.plusDays(3), -5, "Room 1");
        assertThrows(ConstraintViolationException.class, () -> talkService.save(invalid));
    }

    @Test
    @DisplayName("BR-04: a new talk must be scheduled in the future")
    void br04_scheduledDateMustBeInTheFuture() {
        navigate(TalkAdminView.class);
        test(createTalkButton()).click();

        fillForm("Yesterday's Talk", "A description.", "A Speaker", TalkType.PRESENTATION,
                LocalDateTime.now().minusDays(1).withNano(0), 30, "Room 1");
        test(saveButton()).click();

        assertTrue(scheduledDateField().isInvalid());
        assertEquals("Scheduled date must be in the future",
                scheduledDateField().getErrorMessage());
        assertEquals(2, gridSize());

        // The service enforces the same rule for a new talk.
        Talk past = new Talk("Yesterday's Talk", "A description.", "A Speaker",
                TalkType.PRESENTATION, LocalDateTime.now().minusDays(1), 30, "Room 1");
        assertThrows(InvalidTalkException.class, () -> talkService.save(past));
    }

    @Test
    @DisplayName("BR-04: an already-past talk stays editable as long as its date is untouched")
    void br04_pastTalkRemainsEditableWhenDateIsUnchanged() {
        Talk past = repository.save(new Talk("Last Year's Keynote", "A description.",
                "A Speaker", TalkType.PRESENTATION,
                LocalDateTime.now().minusMonths(3).withNano(0), 60, "Main Hall"));

        past.setSpeakerName("A Different Speaker");
        Talk saved = talkService.save(past);

        assertEquals("A Different Speaker", saved.getSpeakerName());

        // Moving that date to another past instant is still rejected.
        saved.setScheduledDate(LocalDateTime.now().minusDays(2));
        assertThrows(InvalidTalkException.class, () -> talkService.save(saved));
    }

    @Test
    @DisplayName("BR-05: the type can only be PRESENTATION or WORKSHOP")
    void br05_typeMustBeOneOfTheTwoEnumValues() {
        navigate(TalkAdminView.class);
        test(createTalkButton()).click();

        assertIterableEquals(List.of(TalkType.PRESENTATION, TalkType.WORKSHOP),
                test(typeField(), TalkType.class).getSuggestionItems());

        // Leaving it unselected is rejected.
        fillForm("No Type", "A description.", "A Speaker", null,
                future.plusDays(3), 30, "Room 1");
        test(saveButton()).click();

        assertTrue(typeField().isInvalid());
        assertEquals(2, gridSize());
    }

    @Test
    @DisplayName("BR-06: a talk is never deleted without confirmation")
    void br06_deleteRequiresConfirmation() {
        navigate(TalkAdminView.class);

        test(deleteButtonFor(EXISTING_TITLE)).click();

        // The click alone must not delete anything — only the confirmation does.
        assertTrue(find(ConfirmDialog.class).exists(), "a confirmation must be requested");
        assertEquals(2, gridSize(), "the talk must survive until the deletion is confirmed");
        assertTrue(findByTitle(EXISTING_TITLE).isPresent());
    }

    @Test
    @DisplayName("BR-07: validation errors carry a clear message per field")
    void br07_validationErrorsHaveClearMessages() {
        navigate(TalkAdminView.class);
        test(createTalkButton()).click();

        test(saveButton()).click();

        assertEquals("Title is required", titleField().getErrorMessage());
        assertEquals("Description is required", descriptionField().getErrorMessage());
        assertEquals("Speaker name is required", speakerField().getErrorMessage());
        assertEquals("Type is required", typeField().getErrorMessage());
        assertEquals("Scheduled date is required", scheduledDateField().getErrorMessage());
        assertEquals("Duration is required", durationField().getErrorMessage());
        assertEquals("Location is required", locationField().getErrorMessage());
    }

    // -------------------------------------------------------------- Helpers

    private void fillForm(String title, String description, String speaker, TalkType type,
            LocalDateTime scheduled, Integer duration, String location) {
        test(titleField()).setValue(title);
        test(descriptionField()).setValue(description);
        test(speakerField()).setValue(speaker);
        if (type != null) {
            test(typeField(), TalkType.class).selectItem(type.getDisplayName());
        }
        test(scheduledDateField()).setValue(scheduled);
        test(durationField()).setValue(duration);
        test(locationField()).setValue(location);
    }

    private Optional<Talk> findByTitle(String title) {
        return repository.findAll().stream()
                .filter(talk -> talk.getTitle().equals(title))
                .findFirst();
    }

    @SuppressWarnings("unchecked")
    private Grid<Talk> grid() {
        return find(Grid.class).testId("talk-grid");
    }

    private int gridSize() {
        return test(grid(), Talk.class).size();
    }

    private List<String> gridTitles() {
        var tester = test(grid(), Talk.class);
        return java.util.stream.IntStream.range(0, tester.size())
                .mapToObj(tester::getRow)
                .map(Talk::getTitle)
                .toList();
    }

    /** Finds a row action button by the accessible name it carries. */
    private Button rowActionButton(String ariaLabel) {
        var tester = test(grid(), Talk.class);
        for (int row = 0; row < tester.size(); row++) {
            Component actions =
                    tester.getCellComponent(row, TalkAdminView.ACTIONS_COLUMN_KEY);
            Optional<Button> match = find(Button.class, actions)
                    .withAriaLabel(ariaLabel).all().stream().findFirst();
            if (match.isPresent()) {
                return match.get();
            }
        }
        throw new AssertionError("No row action button labelled '" + ariaLabel + "'");
    }

    private Button editButtonFor(String talkTitle) {
        return rowActionButton("Edit " + talkTitle);
    }

    private Button deleteButtonFor(String talkTitle) {
        return rowActionButton("Delete " + talkTitle);
    }

    private Button createTalkButton() {
        return find(Button.class).testId("create-talk");
    }

    private Button saveButton() {
        return find(Button.class).testId("save-talk");
    }

    private Button cancelButton() {
        return find(Button.class).testId("cancel-talk");
    }

    private boolean formIsOpen() {
        return find(TalkFormDialog.class).exists();
    }

    private TextField titleField() {
        return find(TextField.class).testId("form-title");
    }

    private TextArea descriptionField() {
        return find(TextArea.class).testId("form-description");
    }

    private TextField speakerField() {
        return find(TextField.class).testId("form-speaker");
    }

    @SuppressWarnings("unchecked")
    private Select<TalkType> typeField() {
        return find(Select.class).testId("form-type");
    }

    private DateTimePicker scheduledDateField() {
        return find(DateTimePicker.class).testId("form-scheduled-date");
    }

    private IntegerField durationField() {
        return find(IntegerField.class).testId("form-duration");
    }

    private TextField locationField() {
        return find(TextField.class).testId("form-location");
    }

    private String latestNotificationText() {
        List<Notification> notifications = find(Notification.class).all();
        assertFalse(notifications.isEmpty(), "expected a notification");
        Notification latest = notifications.get(notifications.size() - 1);
        assertNotNull(latest);
        return test(latest).getText();
    }
}
