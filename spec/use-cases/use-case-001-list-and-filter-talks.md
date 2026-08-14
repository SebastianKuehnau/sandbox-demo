# UC-001: List and Filter Talks

> Visitors browse the catalog of presentations and workshops with filtering and search capabilities.

---

**Goal:** As a visitor, I want to browse and filter presentations and workshops so that I can find talks of interest.

**Status:** Pending
**Date:** 2024-01-01

---

## Actors

- **Primary actor:** Visitor

---

## Preconditions

- The application is loaded and displays the public talk listing page.

---

## Trigger

User navigates to the public talk listing view or applies a filter/search.

---

## Main Flow

1. Visitor opens the public talk listing page.
2. System displays a list of all talks (presentations and workshops) with relevant details.
3. Visitor enters search text in the search field (optional).
4. System filters the talk list to show only talks matching the search text in title, description, or speaker name.
5. Visitor selects a talk type filter (PRESENTATION, WORKSHOP, or ALL).
6. System displays only talks of the selected type(s).
7. System displays the filtered list with clear visual indication of applied filters.

---

## Alternative Flows

### AF-1: No talks match the search or filter criteria

**Branches from:** Main Flow step 4 or 6
**Condition:** The search or filter combination yields no results.

1. System displays an empty-state message indicating no talks match the current filters.
2. Visitor may clear the search or adjust filters.
3. Returns to Main Flow step 3.

### AF-2: Clear filters

**Branches from:** Main Flow step 7
**Condition:** Visitor clicks a "Clear filters" or "Reset" button.

1. System clears all search and filter selections.
2. System displays the complete list of all talks.
3. Use case ends.

---

## Postconditions

- **On success:** Visitor views a filtered list of talks matching the applied criteria.
- **On failure:** No talks are displayed (empty state) or filters are cleared, returning to full list view.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | All talks must display title, speaker name, type, and scheduled date. |
| BR-02 | Search is case-insensitive and searches across title, description, and speaker name. |
| BR-03 | Talk type filter includes options: PRESENTATION, WORKSHOP, ALL. |
| BR-04 | Empty state message is shown when no talks match the applied filters. |

---

## Tests

- [ ] Main Flow covered (steps 1–7)
- [ ] AF-1 covered (empty state)
- [ ] AF-2 covered (clear filters)
- [ ] BR-01, BR-02, BR-03, BR-04 covered

---

## UI Surface

| Page | Access |
|------|--------|
| Public Talk Listing | Anonymous |

The public talk listing page includes:
- A list or table view of all talks with columns/fields for title, speaker, type, date, and duration.
- A search input field to filter by keyword.
- A talk type filter dropdown or button group (PRESENTATION, WORKSHOP, ALL).
- A clear filters button or link.
- An empty-state message when no talks match the current filters.
