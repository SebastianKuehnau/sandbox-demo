# Talk Manager — a Spec-Driven Development Demo

A small Vaadin application for managing conference talks: visitors browse and filter presentations
and workshops, administrators keep the catalog up to date.

The application itself is deliberately modest. The point of this repository is **how** it was
built — every line of it was written by an AI agent from the specifications in [`spec/`](spec/),
running inside a Docker Sandbox.

## Try It Out: Docker Sandbox

This is the fastest way to see the whole flow in action, in an isolated environment. The example project is available at https://github.com/SebastianKuehnau/sandbox-demo.

1. **Check out the starting branch:**

   ```bash
   git checkout 01-start
   ```

2. **Install the Docker Sandbox CLI (`sbx`)**, if you haven't already. Follow the official guide: https://docs.docker.com/ai/sandboxes/get-started/

3. **Start the sandbox with this project's kit:**

   ```bash
   sbx run claude --kit .sbx/kit --name sandbox-demo-claude
   ```

4. **In the Claude Code terminal that opens, type:**

   ```
   /implement-use-case
   ```

   It will ask you which use case from `spec/use-cases/` should be implemented. Once you pick one, it drives the spec-driven workflow end-to-end: it writes the code, verifies the UI visually, writes tests, and commits.

See [Running in a Docker Sandbox](#running-in-a-docker-sandbox) below for managing the sandbox (listing, removing) once you're done exploring.

---

## How this project came to be

| | |
|---|---|
| **Scaffolded at** | [start.vaadin.com](https://start.vaadin.com) — spec-driven development project |
| **AI tool** | [Claude Code](https://claude.com/claude-code) |
| **Environment** | Claude Code running sandboxed in a Docker Sandbox |

Everything grew out of a single prompt given to the project generator:

> A talk management app with 2 views. One for listing and filtering the presentations and workshops
> and the other one is CRUD view to admin the presentations and workshops.

That prompt became the specifications in [`spec/`](spec/) — a project context, an architecture, a
data model, a design system and two use cases. Claude Code then implemented those use cases:
writing the code, verifying the result visually in a browser, writing tests on two levels, and
committing each step.

### Why it exists

This repository is a **hands-on demo of running Claude Code sandboxed with Docker Sandbox**. The
agent gets a container with its own filesystem, its own network policy and its own browser, so it
can install packages, start the application, drive a real browser against it and commit — without
touching the host.

What that gave the agent while building this project:

- **Freedom to install.** It added Maven dependencies, downloaded a 300 MB Chromium and started
  servers, all inside the container.
- **A real browser.** Visual verification and the Playwright tests ran against a live server in the
  sandbox, not against mocks.
- **A restricted network.** Outbound access goes through a proxy with an allow/deny policy, so a
  blocked host fails loudly with an explanation instead of silently reaching the internet.
- **Host-controlled Git.** Credentials are injected at the network level, so the agent can push
  without ever holding a token.

The sandbox conventions the agent follows — environment persistence, the network policy, publishing
ports, Git authentication — are kept in a `CLAUDE.md` alongside the sandbox setup, outside this
repository.

If you want to follow along rather than just read the result, the commit history is the story: one
commit per meaningful step, each message explaining what was decided and why.

---

## The application

Two views, no login:

| Route | View | Who | What |
|-------|------|-----|------|
| `/` | Talk listing | Anyone | Browse all talks, search by keyword, filter by type, clear filters |
| `/admin` | Talk administration | Anyone | Create, edit and delete talks |

> **There is no authentication.** `/admin` is reachable by anyone who can reach the server. This was
> a deliberate choice for a demo — see [`spec/architecture.md`](spec/architecture.md) §4, which also
> lists what adding authentication would involve. Do not expose this as-is on an untrusted network.

**Stack:** Vaadin 25 (Flow, Aura theme) · Spring Boot 4.1 · Spring Data JPA · H2 · Java 25+ · Maven

Talks are stored in a file-based H2 database under `./data/` (git-ignored). On the first start with
an empty database, a seeder inserts eight demo talks so the listing isn't blank. Delete `data/` to
get a fresh seed.

---

## Running the application

```bash
./mvnw                            # dev mode (default goal: spring-boot:run) → http://localhost:8080
./mvnw clean package              # production build (JAR in target/)
```

If you are running inside a Docker Sandbox, the app is not reachable from the host until you publish
the port. On the **host**, with `<sandbox>` being the sandbox name (`$SANDBOX_VM_ID` inside it):

```bash
sbx ports <sandbox> --publish 8080:8080/tcp
```

See [DEVELOPMENT.md](DEVELOPMENT.md) for more build and Docker commands.

---

## Tests

The two use cases are each covered **twice**, by the same flows on two different levels. Tests are
organized per use case rather than per class, so a use case can be read as a single unit — the
convention is described in [`.claude/skills/use-case-tests/SKILL.md`](.claude/skills/use-case-tests/SKILL.md).

| Layer | Classes | Tests | What it proves |
|-------|---------|-------|----------------|
| Vaadin browserless | `UC001ListAndFilterTalks`, `UC002AdminCrudTalks` | 24 | View logic and business rules, on the JVM, in milliseconds |
| Playwright | `UC001ListAndFilterTalksE2E`, `UC002AdminCrudTalksE2E` | 24 | The same flows in a real browser: rendering, client-server round trips, web components |

### Commands

```bash
./mvnw test                       # browserless only — the fast loop (~5 s)
./mvnw verify                     # both layers: browserless, then Playwright
```

Running a single Playwright class or method (`it.test` is the Failsafe counterpart of `test`):

```bash
# one E2E class, still running the browserless tests first
./mvnw verify -Dit.test=UC001ListAndFilterTalksE2E -DfailIfNoSpecifiedTests=false

# one E2E method, skipping the browserless layer entirely
./mvnw verify -Dtest=None -Dsurefire.failIfNoSpecifiedTests=false \
  -Dit.test='UC002AdminCrudTalksE2E#af2_deleteTalkAfterConfirmation' \
  -DfailIfNoSpecifiedTests=false
```

### How the Playwright tests work

- **No server to start.** `AbstractPlaywrightE2ETest` boots the real application on a random port
  with `@SpringBootTest`, so `mvn verify` is all you need.
- **Own browser.** Playwright for Java downloads its own Chromium on the first run (~300 MB, cached
  in `~/.cache/ms-playwright`). No npm toolchain is added to the project.
- **Own data.** Each test seeds exactly the talks it asserts on, against an in-memory H2 under the
  `test` profile, with the demo-data seeder switched off.

Three details make them behave the same on every machine. All three were learned the hard way, and
are documented in [`spec/architecture.md`](spec/architecture.md) §1:

- Vaadin **Copilot** renders a viewport-wide overlay in development mode that swallows pointer
  events, so the Failsafe run sets `vaadin.copilot.enable=false`.
- The date field is set through its **ISO value** rather than by typing a formatted string, because
  the picker parses per browser and JVM locale — and the JDK's CLDR data changed the AM/PM separator
  between releases.
- Dialog open/closed is asserted from the dialog's own `opened` property, because a closed Vaadin
  overlay leaves its content in the DOM with a layout box.

---

## How spec-driven development works here

The specifications are the source of truth. If the AI gets something wrong, the fix is to sharpen a
spec file and re-run — not to repeat yourself in chat.

| File | What goes here |
|------|----------------|
| [`spec/project-context.md`](spec/project-context.md) | Vision, users, scope, constraints |
| [`spec/architecture.md`](spec/architecture.md) | Tech stack, application structure, testing strategy |
| [`spec/datamodel/datamodel.md`](spec/datamodel/datamodel.md) | Entities and relationships |
| [`spec/design-system.md`](spec/design-system.md) | Theme, components, visual standards |
| [`spec/use-cases/`](spec/use-cases/) | One file per capability: flows, business rules, UI surface |

The agent's workflow is packaged as skills under [`.claude/skills/`](.claude/skills/):

| Skill | Purpose |
|-------|---------|
| `new-use-case` | Interviews you and writes a filled-in use case file |
| `implement-use-case` | Implements a use case end to end: code, visual verification, tests, commit |
| `visual-verification` | Drives a browser and checks the UI against the use case |
| `use-case-tests` | Writes and runs the tests for a use case |

`implement-use-case` invokes the other two as part of its flow. Different AI tools invoke skills
differently — in Claude Code they are slash commands, e.g. `/implement-use-case use-case-001`.

### Adding a feature

1. Write the use case: run `new-use-case`, or copy
   [`spec/use-cases/use-case-template.md`](spec/use-cases/use-case-template.md) and fill it in.
2. Run `implement-use-case` for it.
3. Click through the result. If something is off, update the use case (or a project-wide rule) and
   re-run — don't patch the code by hand and leave the spec stale.

---

## Project structure

```
spec/                             specifications — the source of truth
.claude/skills/                   the agent's workflow
src/main/java/dev/vaadin/
  Application.java
  talk/
    domain/                       Talk entity, TalkType, repository
    service/                      business logic, business rules, demo-data seeder
    ui/                           MainLayout, the two views, card, form dialog
src/main/resources/
  META-INF/resources/styles.css   custom styles (Aura tokens only)
src/test/java/dev/vaadin/usecases/
  playwright/                     shared Playwright + server setup
  uc001_list_and_filter_talks/    both test layers for UC-001
  uc002_admin_crud_talks/         both test layers for UC-002
```

## Running in a Docker Sandbox

This project ships a sandbox kit (`.sbx/kit`) that sets up an isolated dev environment with Vaadin skills, Playwright browser dependencies, and the network access needed for Maven, Vaadin, and GitHub.

1. **Install the Docker Sandbox CLI (`sbx`).** Follow the official guide: https://docs.docker.com/ai/sandboxes/get-started/
2. **Start the sandbox with this project's kit:**

   ```bash
   sbx run claude --kit .sbx/kit --name demo-app
   ```

3. **List running sandboxes** (to check status or find the name again):

   ```bash
   sbx list
   ```

4. **Remove the sandbox** when you're done:

   ```bash
   sbx rm demo-app
   ```

## More

- [`spec/README.md`](spec/README.md) — full spec structure and workflow
- [DEVELOPMENT.md](DEVELOPMENT.md) — build, run and Docker commands
- [Docker Sandboxes](https://docs.docker.com/ai/sandboxes/) — running coding agents sandboxed
