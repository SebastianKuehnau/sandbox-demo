# Project Context

> High-level context for the project: the problem being solved, who it's for, what's in scope, and what constraints apply.

## 1. Vision

A web application for managing presentations and workshops. The application provides event organizers and administrators with a centralized platform to organize, display, and manage their talk content. Users can browse and filter presentations and workshops, while administrators can create, update, and delete talks to keep the catalog current.

## 2. Users

- **Visitor**: Can view the list of presentations and workshops, filter by type, and search for talks of interest.
- **Administrator**: Can create new talks, edit existing ones, delete talks, and manage the complete catalog of presentations and workshops.

## 3. Constraints

- All functionality runs within the application itself — no external integrations.
- Two distinct views: one public listing/filtering view and one admin CRUD view.
- Data persists within the application database.

> For technology stack and application structure details, see [`architecture.md`](architecture.md).

---

# Related Documents

- [Spec README](README.md) — process overview and workflow
- [Architecture](architecture.md) — technology stack and application structure
- [Design System](design-system.md) — theme, component usage, and visual standards
- [Use Case Template](use-cases/use-case-template.md) — template for feature specifications
