package dev.vaadin.talk.ui;

import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import dev.vaadin.talk.domain.Talk;
import dev.vaadin.talk.service.TalkService;

/**
 * Public catalog of presentations and workshops — UC-001.
 *
 * <p>Visitors search by keyword, narrow by talk type, and clear both filters
 * again. The result summary and the state of the clear button together give the
 * "clear visual indication of applied filters" that Main Flow step 7 calls for.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Talks")
public class TalkListView extends Main {

    private final TalkService talkService;

    private final TextField searchField = new TextField("Search");
    private final RadioButtonGroup<TalkTypeFilter> typeFilter = new RadioButtonGroup<>("Type");
    private final Button clearFiltersButton = new Button("Clear filters");
    private final Span resultSummary = new Span();
    private final Div results = new Div();
    private final Div emptyState = createEmptyState();

    public TalkListView(TalkService talkService) {
        this.talkService = talkService;
        addClassName("talk-list-view");

        add(createHeader(), createFilterBar(), resultSummary, results, emptyState);
        refresh();
    }

    private Div createHeader() {
        H1 heading = new H1("Talks");
        Paragraph subtitle = new Paragraph(
                "Browse the full programme of presentations and workshops.");
        subtitle.addClassName("view-subtitle");

        Div header = new Div(heading, subtitle);
        header.addClassName("view-header");
        return header;
    }

    private Div createFilterBar() {
        searchField.setPlaceholder("Title, description or speaker");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(event -> refresh());
        searchField.setTestId("talk-search");

        typeFilter.setItems(TalkTypeFilter.values());
        typeFilter.setItemLabelGenerator(TalkTypeFilter::getDisplayName);
        typeFilter.setValue(TalkTypeFilter.ALL);
        typeFilter.addValueChangeListener(event -> refresh());
        typeFilter.setTestId("talk-type-filter");

        clearFiltersButton.addThemeVariants(ButtonVariant.TERTIARY);
        clearFiltersButton.setIcon(VaadinIcon.CLOSE_SMALL.create());
        clearFiltersButton.addClickListener(event -> clearFilters());
        clearFiltersButton.setTestId("clear-filters");

        Div filterBar = new Div(searchField, typeFilter, clearFiltersButton);
        filterBar.addClassNames("filter-bar", "aura-surface");
        return filterBar;
    }

    private static Div createEmptyState() {
        Div emptyState = new Div(
                VaadinIcon.SEARCH_MINUS.create(),
                new H2("No talks match your filters"),
                new Paragraph("Try a different search term, or clear the filters "
                        + "to see the whole programme again."));
        emptyState.addClassName("empty-state");
        emptyState.setTestId("empty-state");
        emptyState.setVisible(false);
        return emptyState;
    }

    /** Applies AF-2: drops every filter and shows the full catalog again. */
    private void clearFilters() {
        searchField.clear();
        typeFilter.setValue(TalkTypeFilter.ALL);
        refresh();
    }

    /** Re-queries the service with the current filters and repaints the results. */
    private void refresh() {
        TalkTypeFilter selected = typeFilter.getValue() == null
                ? TalkTypeFilter.ALL
                : typeFilter.getValue();
        List<Talk> talks = talkService.search(searchField.getValue(), selected.getTalkType());

        results.removeAll();
        results.addClassName("talk-results");
        talks.forEach(talk -> results.add(new TalkCard(talk)));

        boolean noMatches = talks.isEmpty();
        results.setVisible(!noMatches);
        emptyState.setVisible(noMatches);

        updateSummary(talks.size());
    }

    private void updateSummary(int shown) {
        boolean filtered = isFiltered();
        clearFiltersButton.setEnabled(filtered);
        resultSummary.addClassName("result-summary");
        resultSummary.setTestId("result-summary");

        if (filtered) {
            resultSummary.setText("Showing %d of %d talks".formatted(shown, talkService.count()));
        } else {
            resultSummary.setText(shown == 1 ? "1 talk" : "%d talks".formatted(shown));
        }
    }

    private boolean isFiltered() {
        boolean hasSearchText = !searchField.getValue().isBlank();
        boolean hasTypeFilter = typeFilter.getValue() != null
                && typeFilter.getValue() != TalkTypeFilter.ALL;
        return hasSearchText || hasTypeFilter;
    }
}
