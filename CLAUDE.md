# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Spring PetClinic built with the AI-native SDLC playbook. Currently a minimal Spring Boot 4.1 baseline (web MVC + Actuator)

## Commands

```bash
./mvnw clean verify                                        # build + all tests
./mvnw spring-boot:run                                     # run on :8080
./mvnw test -Dtest=PetClinicApplicationTests#contextLoads  # single test
curl -i localhost:8080/actuator/health
```

- Java 27 (Eclipse Temurin, locally and in CI), pinned in `pom.xml` and `.sdkmanrc` (`java=27.0.0.0+35-tem`). Run `sdk env install` / `sdk env` before building; a mismatched default JDK breaks the build. Keep SDKMAN current (`sdk selfupdate force`): an outdated install hides new GA builds. The project follows the latest JDK, LTS or not.
- Config is YAML only (`application.yaml`); do not add `application.properties`.
- Favour dependencies whose versions Spring Boot manages. A non-Boot dependency is allowed only with a stated reason in its spec.
- Keep the actuator surface minimal: Boot's default exposes only `health`. Don't add `management.endpoints` settings unless a spec asks for it.
- UI follows the design system in `docs/design-system/` (tokens, components, usage rules); `static/resources/css/petclinic.css` implements it as a hand-written stylesheet, with `static/resources/js/navbar.js` for the collapsing menu; no CSS framework or WebJars.
- Spring Data JDBC quotes explicit `@Table`/`@Column` names, so write them in UPPER CASE to match H2.
- Never mention intents (e.g. "intent 009") in code, YAML, SQL or CSS comments; comments describe the code, and the `intent/` documents carry the history.
- Boot 4 moved test annotations into modular packages (e.g. `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`); check the generated imports rather than assuming Boot 3 packages.

## Design system

UI work follows the Spring PetClinic design system in `docs/design-system/` (a versioned mirror of the published design system artifact, derived from the vets page screenshot in intent 004). Read `README.md` there before building or styling any page.

- `tokens.json` holds the colours, type, spacing, radii and borders; `components/<Name>/README.md` covers the nine components (Navbar, DataTable, DetailList, Pagination, Footer, FormField, Button, InlineMessage, VisitList) plus the OwnerForm, PetForm, VisitForm and ErrorPage page patterns, each with a `preview.html`. `assets/Logos/` has the logo crops. Asset `blob` ids in `design-system.json` are those of the published artifact, https://claude.ai/artifact/2Dg7pbGjKMze7Y8zaoikw2; keep both in step.
- Pages are Thymeleaf, styled by a hand-written `static/resources/css/petclinic.css` that maps the tokens onto `pc-*` classes, plus `navbar.js` for the menu toggle (the only script; every page works without it). No CSS framework, no WebJars, no SCSS build.
- Key rules: square corners everywhere, inputs and buttons included, no shadows/gradients/cards; `spring-green` (#6db33f) is decorative only (accent bar, logo, navbar focus ring) and never text on white; `spring-green-dark` carries links and active/current states; Montserrat for display, Varela Round for body; nav items and table headers are Title Case in markup, UPPERCASE via CSS; empty values render as `none` in `ink-muted`; one light theme only.
- Accessibility is part of the system: landmarks, hidden `<caption>` and `<th scope="col">`, `aria-current="page"`, `aria-label` on pagination, visible 2px focus outlines.
- `intent/004-vets/PetClinic Vets.html` is the vets page rebuilt with the system, the visual reference for intent 004.
- If the implementation needs a token or component the system lacks, update `docs/design-system/` in the same PR rather than hard-coding values in CSS.

## CI

GitHub Actions workflows (intent 002) live in `.github/workflows/`:
- `build.yml`: `./mvnw -B clean verify` on Temurin 27 for pushes to `main` and every PR.
- `snyk.yml`: Snyk Open Source and Code scans that fail on `high` and above, and keep a `snyk-sarif` artifact.
- `sonar.yml`: SonarQube Cloud analysis (`verify` plus the Sonar Maven scanner, JaCoCo coverage) on pushes to `main` and every PR, then a `quality-gate` job (`sonarcloud-actions/sonarcloud-check-status-action`) that fails when the "Sonar way" gate does. Scanner settings are `sonar.*` properties in `pom.xml`. Fix Sonar issues in code; a false positive, won't-fix or safe hotspot is marked in SonarQube Cloud with a reason (a human step). Use `NOSONAR` only where a finding conflicts with a rule in this file (e.g. tag-referenced actions), on the flagged line with the rule key and reason in a comment beside it.
- `claude-code-review.yml`: advisory Claude review following `REVIEW.md`, plus a `/security-review` job, including on fix commits pushed by `@claude`.
- `claude.yml`: `@claude` mentions that push fixes to the PR branch. Bot comments are ignored unless they contain `@claude remove these Spring Boot version overrides` or `@claude update .sdkmanrc to the Temurin identifier`, the requests `REVIEW.md`'s Spring Boot overrides and Java pins rules end their comments with; the Claude run may `curl -s` the SDKMAN Java candidates API to answer the latter. claude-code-action strips HTML comments from the comments it posts, so a hidden marker cannot be used.
- `design-spec.yml`: drafts `spec.md` as a PR when an `intent.md` lands on `main`.
- `triage-findings.yml`: weekly, runs the `/triage-findings` skill on the latest Snyk results and SonarQube Cloud issues and hotspots on `main` (fetched without a token; the project is public) and opens a PR with the `intent.md` files it writes.
- `evals.yml`: runs `/skill-creator eval <skill>` on PRs that change a skill (or `CLAUDE.md`) and on dispatch, and comments the results on the PR.
- `dependency-updates.yml`: when `build` fails on a `renovate/*` PR by `renovate[bot]`, Claude tries a fix on the PR branch, up to `MAX_FIX_ATTEMPTS` (2) per PR, then posts a give-up comment for a human.

Dependency updates come from the hosted Mend Renovate App, configured in `renovate.json5` (JSON5, the format Renovate reads; the YAML-only rule is about Spring config). It opens grouped PRs before 05:00 UTC every day (`spring-boot`, `maven-minor-patch`, `github-actions`, `maven-wrapper`, `java`), never auto-merges and never pins actions to SHAs. The `java` group proposes every new Java major, LTS or not, moving `pom.xml` and the four `setup-java` pins together. Keep `setup-java` on `temurin`: Renovate's github-actions manager reads `java-version` only for `temurin` and `adopt`. It does not touch `.sdkmanrc`: on a Java major PR the review ends its comment with `@claude update .sdkmanrc to the Temurin identifier for Java <major> ...`. To answer it, `curl -s` `https://api.sdkman.io/2/candidates/java/linuxx64/versions/list?installed=` and the same URL with `darwinarm64`, and use the GA `-tem` identifier for that major listed on both (not EA); if there is none, change nothing and say so.

`claude-code-review.yml` and `claude.yml` were generated by `/install-github-app`; keep changes to them minimal so they stay close to the upstream templates.

Lint with `actionlint .github/workflows/*.yml` before pushing workflow changes. Claude workflows skip on a PR that changes them until it is merged, because claude-code-action requires the file to match `main`. Never push to `main`: every change, agent-made or not, goes through a branch and PR. Reference third-party actions by version tag (e.g. `actions/checkout@v7`); do not pin them to a commit SHA.

## Evals

- Each pipeline skill (`/intent`, `/spec`, `/triage-findings`) keeps its eval cases in `.claude/skills/<skill>/evals/evals.json` (skill-creator's format; fixtures in `evals/files/`). Run one locally with `/skill-creator eval <skill>`; the skill-creator plugin is enabled in `.claude/settings.json`.
- The `skill-evals` check fails when a PR's skill scores lower than `main`'s on any eval (or below 100% with nothing to compare against). It blocks merging only if it is made a required check in branch protection.
- When a skill misses something a human later catches, add an eval for it to that skill's `evals.json`. Keep fixtures out of the repo's top-level `intent/` and never name them `pom.xml` or `*.java` (other workflows and Snyk would pick them up); use a `.fixture` suffix.

## Workflow: intent -> spec -> plan

Work follows the AI-native SDLC playbook. Each change lives in `intent/<nnn>-<name>/` with `intent.md`, `spec.md` and `plan.md`; read the spec for requirements and acceptance criteria before changing code, and keep these documents in sync with the implementation when the two diverge (note deviations explicitly in the spec).

- Use the `/intent` and `/spec` skills (in `.claude/skills/`) to create those artifacts; `/triage-findings` turns Snyk and Sonar findings into intents.
- A `PostToolUse` hook on `ExitPlanMode` (`.claude/settings.json`) instructs saving an approved plan as `plan.md` next to the relevant `spec.md`, not in `~/.claude/plans` only.
- Intents and specs do not carry a Status field.
- Generate new Spring Boot projects with `spring-tools:create-spring-boot-project`, and run `spring-tools:validate` to check for diagnostics.
