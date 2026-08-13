package dev.vaadin.usecases.uc001_list_and_filter_talks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;

import dev.vaadin.talk.domain.Talk;
import dev.vaadin.talk.domain.TalkRepository;
import dev.vaadin.talk.domain.TalkType;
import dev.vaadin.talk.ui.TalkCard;
import dev.vaadin.talk.ui.TalkFormats;
import dev.vaadin.talk.ui.TalkListView;
import dev.vaadin.talk.ui.TalkTypeFilter;

/**
 * Tests for UC-001: List and Filter Talks.
 *
 * @see <a href="../../../../../../spec/use-cases/use-case-001-list-and-filter-talks.md">
 *      spec/use-cases/use-case-001-list-and-filter-talks.md</a>
 */
@SpringBootTest
@ActiveProfiles("test")
class UC001ListAndFilterTalks extends SpringBrowserlessTest {

    @Autowired
    private TalkRepository repository;

    /**
     * Five talks: three presentations and two workshops. Each search term used
     * below deliberately matches exactly one field of one talk, so a passing
     * test cannot be an accident of overlapping text.
     */
    @BeforeEach
    void seedCatalog() {
        repository.deleteAll();
        LocalDateTime base = LocalDateTime.now().plusDays(7).withNano(0);
        repository.saveAll(List.of(
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
    @DisplayName("Main Flow 1-2: opening the listing shows every talk")
    void mainFlow_openListing_showsAllTalks() {
        navigate(TalkListView.class);

        assertEquals(5, cards().size(), "all seeded talks should be listed");
        assertEquals("5 talks", summaryText());
    }

    @Test
    @DisplayName("Main Flow 3-4: search text filters the list")
    void mainFlow_searchText_filtersList() {
        navigate(TalkListView.class);

        search("Aura");

        assertIterableEquals(List.of("Aura Theming Deep Dive"), cardTitles());
    }

    @Test
    @DisplayName("Main Flow 5-6: the type filter shows only the selected type")
    void mainFlow_typeFilter_showsOnlySelectedType() {
        navigate(TalkListView.class);

        selectType(TalkTypeFilter.WORKSHOP);

        assertIterableEquals(List.of("Hands-on Full-Stack Build", "Accessibility Clinic"),
                cardTitles());
        assertTrue(cards().stream()
                .allMatch(card -> card.getTalk().getType() == TalkType.WORKSHOP));
    }

    @Test
    @DisplayName("Main Flow 7: combined filters are applied together and shown in the summary")
    void mainFlow_combinedFilters_areAppliedAndIndicated() {
        navigate(TalkListView.class);

        selectType(TalkTypeFilter.PRESENTATION);
        search("Testing");

        assertIterableEquals(List.of("Testing Without a Real UI"), cardTitles());
        // The summary makes the active filtering visible to the visitor.
        assertEquals("Showing 1 of 5 talks", summaryText());
    }

    // ---------------------------------------------------- Alternative Flows

    @Test
    @DisplayName("AF-1: no matches shows the empty state instead of the list")
    void af1_noMatches_showsEmptyState() {
        navigate(TalkListView.class);

        search("quantum tunnelling");

        assertEquals(0, cards().size());
        assertTrue(emptyStateIsShown(), "empty state should be shown");
        assertEquals("Showing 0 of 5 talks", summaryText());
    }

    @Test
    @DisplayName("AF-2: clearing the filters restores the complete list")
    void af2_clearFilters_restoresFullList() {
        navigate(TalkListView.class);
        search("Aura");
        selectType(TalkTypeFilter.PRESENTATION);
        assertEquals(1, cards().size());

        test(clearFiltersButton()).click();

        assertEquals(5, cards().size(), "every talk should be listed again");
        assertEquals("", searchField().getValue());
        assertEquals(TalkTypeFilter.ALL, typeFilter().getValue());
        assertEquals("5 talks", summaryText());
        assertFalse(emptyStateIsShown(), "empty state should be gone once filters are cleared");
    }

    // ------------------------------------------------------- Business Rules

    @Test
    @DisplayName("BR-01: every talk shows title, speaker, type and scheduled date")
    void br01_everyTalkShowsTitleSpeakerTypeAndDate() {
        navigate(TalkListView.class);

        List<TalkCard> cards = cards();
        assertEquals(5, cards.size());
        for (TalkCard card : cards) {
            Talk talk = card.getTalk();
            // The title is rendered by the card itself as its heading, so it is
            // read back from the card rather than from the child element text.
            assertEquals(talk.getTitle(), card.getTitleAsText(),
                    "card should show the title as its heading");

            String rendered = card.getElement().getTextRecursively();
            assertTrue(rendered.contains(talk.getSpeakerName()),
                    () -> "card should show the speaker: " + rendered);
            assertTrue(rendered.contains(talk.getType().getDisplayName()),
                    () -> "card should show the type: " + rendered);
            assertTrue(rendered.contains(TalkFormats.formatDateTime(talk.getScheduledDate())),
                    () -> "card should show the scheduled date: " + rendered);
        }
    }

    @Test
    @DisplayName("BR-02: search is case-insensitive across title, description and speaker")
    void br02_searchIsCaseInsensitiveAcrossTitleDescriptionAndSpeaker() {
        navigate(TalkListView.class);

        // Title, in the wrong case.
        search("aURA tHEMING");
        assertIterableEquals(List.of("Aura Theming Deep Dive"), cardTitles());

        // Speaker name only — the term appears in no title or description.
        search("AMARA");
        assertIterableEquals(List.of("Signals in Vaadin"), cardTitles());

        // Description only — "browserless" appears in no title or speaker name.
        search("BROWSERLESS");
        assertIterableEquals(List.of("Testing Without a Real UI"), cardTitles());
    }

    @Test
    @DisplayName("BR-03: the type filter offers PRESENTATION, WORKSHOP and ALL")
    void br03_typeFilterOffersPresentationWorkshopAndAll() {
        navigate(TalkListView.class);

        List<TalkTypeFilter> options = typeFilter().getListDataView().getItems().toList();

        assertIterableEquals(
                List.of(TalkTypeFilter.ALL, TalkTypeFilter.PRESENTATION, TalkTypeFilter.WORKSHOP),
                options);
        assertEquals(TalkTypeFilter.ALL, typeFilter().getValue(), "ALL is the default");
    }

    @Test
    @DisplayName("BR-04: an empty-state message is shown when nothing matches")
    void br04_emptyStateMessageIsShownWhenNothingMatches() {
        navigate(TalkListView.class);

        search("quantum tunnelling");

        Div emptyState = emptyState();
        assertTrue(emptyState.getElement().getTextRecursively()
                        .contains("No talks match your filters"),
                () -> "expected an explanatory message, got: "
                        + emptyState.getElement().getTextRecursively());
    }

    // -------------------------------------------------------------- Helpers

    private void search(String text) {
        test(searchField()).setValue(text);
    }

    private void selectType(TalkTypeFilter option) {
        test(typeFilter(), TalkTypeFilter.class).selectItem(option.getDisplayName());
    }

    private List<TalkCard> cards() {
        return find(TalkCard.class).all();
    }

    private List<String> cardTitles() {
        return cards().stream().map(card -> card.getTalk().getTitle()).toList();
    }

    private String summaryText() {
        return find(Span.class).testId("result-summary").getText();
    }

    private TextField searchField() {
        return find(TextField.class).testId("talk-search");
    }

    @SuppressWarnings("unchecked")
    private RadioButtonGroup<TalkTypeFilter> typeFilter() {
        return find(RadioButtonGroup.class).testId("talk-type-filter");
    }

    private Button clearFiltersButton() {
        return find(Button.class).testId("clear-filters");
    }

    private Div emptyState() {
        return find(Div.class).testId("empty-state");
    }

    /**
     * Component queries only match visible components, so the absence of a hit
     * is exactly what "the empty state is not shown" means.
     */
    private boolean emptyStateIsShown() {
        return find(Div.class).withTestId("empty-state").exists();
    }
}
