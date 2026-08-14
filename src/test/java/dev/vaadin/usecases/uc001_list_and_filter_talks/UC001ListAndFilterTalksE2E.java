package dev.vaadin.usecases.uc001_list_and_filter_talks;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;

import dev.vaadin.talk.domain.Talk;
import dev.vaadin.talk.domain.TalkType;
import dev.vaadin.talk.ui.TalkFormats;
import dev.vaadin.usecases.playwright.AbstractPlaywrightE2ETest;

/**
 * Browser-driven tests for UC-001: List and Filter Talks.
 *
 * <p>Mirrors {@link UC001ListAndFilterTalks} method for method. The browserless
 * class proves the view logic on the JVM; this one proves the same behaviour
 * reaches a real browser through actual rendering and client-server round trips.
 *
 * @see <a href="../../../../../../spec/use-cases/use-case-001-list-and-filter-talks.md">
 *      spec/use-cases/use-case-001-list-and-filter-talks.md</a>
 */
class UC001ListAndFilterTalksE2E extends AbstractPlaywrightE2ETest {

    private LocalDateTime base;

    @BeforeEach
    void seedCatalog() {
        base = futureDate(7);
        givenTalks(List.of(
                new Talk("Signals in Vaadin", "Reactive state management for server-side UIs.",
                        "Amara Osei", TalkType.PRESENTATION, base, 45, "Main Hall"),
                new Talk("Aura Theming Deep Dive", "Computed colour and spacing tokens.",
                        "Jonas Lindqvist", TalkType.PRESENTATION, base.plusDays(1), 30, "Room 204"),
                new Talk("Hands-on Full-Stack Build", "Build an application from an empty folder.",
                        "Priya Raghunathan", TalkType.WORKSHOP, base.plusDays(2), 180,
                        "Workshop Room A"),
                new Talk("Testing Without a Real UI", "How to run browserless tests on the JVM.",
                        "Marcus Feldt", TalkType.PRESENTATION, base.plusDays(3), 45, "Room 204"),
                new Talk("Accessibility Clinic", "Keyboard navigation and screen readers.",
                        "Lena Hartmann", TalkType.WORKSHOP, base.plusDays(4), 120,
                        "Workshop Room B")));
    }

    // ------------------------------------------------------------ Main Flow

    @Test
    @DisplayName("Main Flow 1-2: opening the listing renders every talk")
    void mainFlow_openListing_showsAllTalks() {
        open("/");

        assertThat(talkCards()).hasCount(5);
        assertThat(page.getByTestId("result-summary")).hasText("5 talks");
    }

    @Test
    @DisplayName("Main Flow 3-4: typing in the search field filters the rendered cards")
    void mainFlow_searchText_filtersList() {
        open("/");

        search("Aura");

        assertCardTitles(List.of("Aura Theming Deep Dive"));
    }

    @Test
    @DisplayName("Main Flow 5-6: the type filter shows only the selected type")
    void mainFlow_typeFilter_showsOnlySelectedType() {
        open("/");

        selectType("Workshops");

        assertCardTitles(List.of("Hands-on Full-Stack Build", "Accessibility Clinic"));
    }

    @Test
    @DisplayName("Main Flow 7: combined filters are applied and shown in the summary")
    void mainFlow_combinedFilters_areAppliedAndIndicated() {
        open("/");

        selectType("Presentations");
        search("Testing");

        assertCardTitles(List.of("Testing Without a Real UI"));
        assertThat(page.getByTestId("result-summary")).hasText("Showing 1 of 5 talks");
        // The clear-filters action becomes available once filters are active.
        assertThat(page.getByTestId("clear-filters")).isEnabled();
    }

    // ---------------------------------------------------- Alternative Flows

    @Test
    @DisplayName("AF-1: no matches renders the empty state instead of cards")
    void af1_noMatches_showsEmptyState() {
        open("/");

        search("quantum tunnelling");

        // The empty state appearing is the signal that the filter has been applied.
        assertThat(page.getByTestId("empty-state")).isVisible();
        assertThat(talkCards()).hasCount(0);
        assertThat(page.getByTestId("result-summary")).hasText("Showing 0 of 5 talks");
    }

    @Test
    @DisplayName("AF-2: clearing the filters restores the complete list")
    void af2_clearFilters_restoresFullList() {
        open("/");
        search("Aura");
        selectType("Presentations");
        assertThat(talkCards()).hasCount(1);

        page.getByTestId("clear-filters").click();
        waitForVaadin();

        assertThat(talkCards()).hasCount(5);
        assertThat(page.getByTestId("result-summary")).hasText("5 talks");
        assertThat(page.getByTestId("empty-state")).isHidden();
        assertThat(searchInput()).hasValue("");
        assertTrue(isTypeSelected("All"), "the type filter should be back on All");
        // With no filters left there is nothing to clear.
        assertThat(page.getByTestId("clear-filters")).isDisabled();
    }

    // ------------------------------------------------------- Business Rules

    @Test
    @DisplayName("BR-01: each card visibly shows title, speaker, type and scheduled date")
    void br01_everyTalkShowsTitleSpeakerTypeAndDate() {
        open("/");

        Locator card = page.locator("vaadin-card.talk-card[aria-label='Signals in Vaadin']");
        assertThat(card).isVisible();
        // Each of these must be readable on screen, not merely present in the model.
        assertThat(card.getByText("Signals in Vaadin")).isVisible();
        assertThat(card.getByText("Amara Osei")).isVisible();
        assertThat(card.getByText("Presentation")).isVisible();
        assertThat(card.getByText(TalkFormats.formatDateTime(base))).isVisible();
    }

    @Test
    @DisplayName("BR-02: search is case-insensitive across title, description and speaker")
    void br02_searchIsCaseInsensitiveAcrossTitleDescriptionAndSpeaker() {
        open("/");

        // Title, in the wrong case.
        search("aURA tHEMING");
        assertCardTitles(List.of("Aura Theming Deep Dive"));

        // Speaker name only — the term appears in no title or description.
        search("AMARA");
        assertCardTitles(List.of("Signals in Vaadin"));

        // Description only — "browserless" appears in no title or speaker name.
        search("BROWSERLESS");
        assertCardTitles(List.of("Testing Without a Real UI"));
    }

    @Test
    @DisplayName("BR-03: the type filter offers All, Presentations and Workshops")
    void br03_typeFilterOffersPresentationWorkshopAndAll() {
        open("/");

        Locator options = page.getByTestId("talk-type-filter").locator("vaadin-radio-button");
        assertThat(options).hasCount(3);
        assertEquals(List.of("All", "Presentations", "Workshops"),
                options.allInnerTexts().stream().map(String::trim).toList());
        assertTrue(isTypeSelected("All"), "All is selected by default");
    }

    @Test
    @DisplayName("BR-04: an empty-state message explains that nothing matched")
    void br04_emptyStateMessageIsShownWhenNothingMatches() {
        open("/");

        search("quantum tunnelling");

        Locator emptyState = page.getByTestId("empty-state");
        assertThat(emptyState).isVisible();
        assertThat(emptyState).containsText("No talks match your filters");
    }

    // -------------------------------------------------------------- Helpers

    private Locator searchInput() {
        return page.getByTestId("talk-search").locator("input");
    }

    private void search(String text) {
        searchInput().fill(text);
        // The field commits lazily, so the results arrive a round trip later. The
        // assertions below wait for the expected content rather than a fixed delay.
        waitForVaadin();
    }

    /**
     * Waits for exactly the expected cards to be rendered, then checks their order.
     *
     * <p>Waiting on the content matters: consecutive filters can produce the same
     * number of cards, so a count alone would pass against the previous result.
     */
    private void assertCardTitles(List<String> expected) {
        // Each expected card must appear before the ordered comparison is read.
        expected.forEach(title -> assertThat(cardTitled(title)).hasCount(1));
        assertThat(talkCards()).hasCount(expected.size());
        assertIterableEquals(expected, visibleCardTitles());
    }

    private Locator cardTitled(String title) {
        return page.locator("vaadin-card.talk-card[aria-label=\"" + title + "\"]:visible");
    }

    private void selectType(String optionLabel) {
        page.getByTestId("talk-type-filter")
                .locator("vaadin-radio-button")
                .filter(new Locator.FilterOptions().setHasText(optionLabel))
                .click();
        waitForVaadin();
    }

    private boolean isTypeSelected(String optionLabel) {
        return (Boolean) page.getByTestId("talk-type-filter")
                .locator("vaadin-radio-button")
                .filter(new Locator.FilterOptions().setHasText(optionLabel))
                .first()
                .evaluate("n => n.checked");
    }
}
