# Architecture

> Technology stack and application structure. `pom.xml` is the source of truth for versions. Do not modify `pom.xml`, `vite.config.ts`, or `architecture.md` without asking.

---

## 1. Technology Stack

- Vaadin — server-side Java UI (Flow) for **all** views, public and admin alike. React views are
  not used: `hilla-spring-boot-starter` is deliberately absent, and there is no `src/main/frontend`.
- Spring Boot — auto-configuration, embedded Tomcat
- Java
- Maven (wrapper included)
- Database: **H2**, file-based at `./data/talks` (`data/` is git-ignored). Persistence via
  Spring Data JPA with `ddl-auto=update`. Tests use a throwaway in-memory H2 under the
  `test` profile.
- Routing: Vaadin Flow views use `@Route` with `layout = MainLayout.class`.
- Testing: JUnit 5, Vaadin Browserless Tests (`browserless-test-spring`). Tests are organized per
  use case, not per view — see `/use-case-tests` for the convention. Because that convention names
  classes `UC001…`, `maven-surefire-plugin` carries an explicit `<includes>` for `**/UC*.java`;
  without it Surefire's default patterns match nothing and the suite silently runs zero tests.

---

## 2. Application Structure

Feature-based packaging, one package per feature with `domain` / `service` / `ui` inside it:

```
dev.vaadin/
  Application.java              — Spring Boot entry point
  talk/
    domain/
      Talk.java                 — @Entity
      TalkType.java             — PRESENTATION | WORKSHOP
      TalkRepository.java       — Spring Data JPA + Specifications
    service/
      TalkService.java          — Business logic and business rules
      TalkDataSeeder.java       — Demo data, only when the table is empty
      InvalidTalkException.java
    ui/
      MainLayout.java           — AppLayout shell with navigation
      TalkListView.java         — @Route("")      — UC-001
      TalkCard.java             — one talk in the public listing
      TalkAdminView.java        — @Route("admin") — UC-002
      TalkFormDialog.java       — create/edit form with Binder
      TalkTypeFilter.java       — ALL | PRESENTATION | WORKSHOP
      TalkFormats.java          — shared date/duration formatting
```

Tests mirror use cases rather than classes:

```
src/test/java/dev/vaadin/usecases/
  uc001_list_and_filter_talks/UC001ListAndFilterTalks.java
  uc002_admin_crud_talks/UC002AdminCrudTalks.java
```

---

## 3. UIState Management

- **Signals** are the primary mechanism for managing UI state
- **Non-shared signals** for standard per-user UI state (e.g., form values, selection state, view-local data)
- **Shared signals** when state must be visible across multiple users/sessions (collaborative or real-time features) — requires **server push** to be enabled
- When using shared signals, enable push on the Application class (i.e. add a `@Push` annotation)

---

## 4. Security & Admin

**This application has no authentication and no authorization.** Spring Security is not on the
classpath, there is no login, and every route is reachable by anyone who can reach the server:

| Route | View | Access |
|-------|------|--------|
| `/` | `TalkListView` | Anonymous |
| `/admin` | `TalkAdminView` | Anonymous — anyone can create, edit and delete talks |

This is a deliberate choice for this project, not an oversight. It means the admin view must not be
exposed to an untrusted network as-is.

To add authentication later, the pieces would be:

- Add `spring-boot-starter-security`
- Configure `VaadinSecurityConfigurer` (not the deprecated `VaadinWebSecurity`)
- `@AnonymousAllowed` on `TalkListView`, `@RolesAllowed("ADMIN")` on `TalkAdminView`
- A Vaadin `LoginForm` at `/login`
- An access-control test in `UC002AdminCrudTalks` using `@WithAnonymousUser`
