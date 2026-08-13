# UC-002: Admin CRUD Operations for Talks

> Administrators manage the complete catalog of talks (create, read, update, delete presentations and workshops).

---

**Goal:** As an administrator, I want to create, edit, and delete presentations and workshops so that I can manage the talk catalog.

**Status:** Implemented
**Date:** 2024-01-01

---

## Actors

- **Primary actor:** Administrator

---

## Preconditions

- The admin talk management view is accessible.

> **No authentication.** This application deliberately ships without a login.
> The admin view at `/admin` is reachable by anyone who knows the URL, and
> "Administrator" below describes a role a person plays, not an authenticated
> identity the application checks. Adding authentication would mean introducing
> Spring Security, protecting `/admin`, and adding an access-control test to
> this use case.

---

## Trigger

Administrator navigates to the admin talk management view or initiates a create/edit/delete action.

---

## Main Flow

1. Administrator opens the admin talk management view.
2. System displays a list of all talks in a grid/table with columns for title, speaker, type, date, and actions (Edit, Delete).
3. Administrator clicks the "Create Talk" button to add a new talk.
4. System displays a form with fields for title, description, speaker name, type (dropdown), scheduled date, duration, and location.
5. Administrator fills in all required fields and clicks "Save".
6. System validates the form input and persists the new talk.
7. System refreshes the talks list to show the newly created talk.
8. System displays a success notification.

---

## Alternative Flows

### AF-1: Edit Existing Talk

**Branches from:** Main Flow step 2
**Condition:** Administrator clicks the Edit button on a talk row.

1. System displays the form pre-populated with the selected talk's data.
2. Administrator modifies one or more fields.
3. Administrator clicks "Save".
4. System validates the form input and updates the talk in the database.
5. System refreshes the talks list.
6. System displays a success notification.
7. Returns to Main Flow step 2 (list view).

### AF-2: Delete Talk

**Branches from:** Main Flow step 2
**Condition:** Administrator clicks the Delete button on a talk row.

1. System displays a confirmation dialog asking "Are you sure you want to delete this talk?".
2. Administrator confirms the deletion.
3. System deletes the talk from the database and removes it from the list.
4. System displays a success notification.
5. Returns to Main Flow step 2 (list view).

### AF-3: Cancel Delete

**Branches from:** AF-2 step 1
**Condition:** Administrator clicks Cancel in the confirmation dialog.

1. System closes the confirmation dialog without deleting.
2. Returns to Main Flow step 2 (list view).

### AF-4: Form Validation Error

**Branches from:** Main Flow step 6 or AF-1 step 4
**Condition:** One or more required fields are empty or invalid.

1. System displays validation error messages next to the invalid fields.
2. Administrator corrects the errors and clicks "Save" again.
3. Returns to Main Flow step 6 / AF-1 step 4.

### AF-5: Cancel Form

**Branches from:** Main Flow step 5 or AF-1 step 3
**Condition:** Administrator clicks Cancel while editing/creating.

1. System closes the form without saving.
2. Returns to Main Flow step 2 (list view).

---

## Postconditions

- **On success (Create):** A new talk is added to the catalog and visible in the list.
- **On success (Edit):** The talk is updated with new information and reflected in the list.
- **On success (Delete):** The talk is removed from the catalog and the list is refreshed.
- **On failure:** No changes are persisted; the list remains unchanged and validation errors are shown.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | All fields (title, description, speaker, type, date, duration, location) are mandatory. |
| BR-02 | Title and speaker name must be non-empty text. |
| BR-03 | Duration must be a positive integer (minutes). |
| BR-04 | Scheduled date must be a valid date/time. A new talk must be scheduled in the future; an edit must also be, but only if it changes the date — leaving an already-past date untouched stays allowed, so talks that have happened remain editable. |
| BR-05 | Talk type must be selected from PRESENTATION or WORKSHOP enum. |
| BR-06 | Delete operations require confirmation to prevent accidental loss. |
| BR-07 | Form validation errors must be displayed with clear messages. |

---

## Tests

- [x] Main Flow covered (steps 1–8)
- [x] AF-1 (Edit) covered
- [x] AF-2, AF-3 (Delete and Cancel) covered
- [x] AF-4 (Validation errors) covered
- [x] AF-5 (Cancel form) covered
- [x] BR-01 through BR-07 covered

Implemented by `src/test/java/dev/vaadin/usecases/uc002_admin_crud_talks/UC002AdminCrudTalks.java`
(14 tests: browserless for the view flows, plain `@SpringBootTest` against `TalkService`
for the rules that do not need a UI).

---

## UI Surface

| Page | Route | Access |
|------|-------|--------|
| Admin Talk Management | `/admin` | Anonymous — see Preconditions |

The admin talk management page includes:
- A grid or table listing all talks with columns: title, speaker, type, scheduled date, duration.
- Edit and Delete action buttons on each row.
- A "Create Talk" button above the list.
- A form modal or inline form for creating/editing talks with fields: title, description, speaker name, type (dropdown), scheduled date, duration, location.
- A "Save" and "Cancel" button on the form.
- A confirmation dialog for delete operations.
- Success/error notification messages for all CRUD operations.
