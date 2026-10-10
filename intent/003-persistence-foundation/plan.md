# Plan: Persistence foundation (intent 003)

## Context
The app (intent 001) has no datasource, so domain intents have nothing to build on. Per `intent/003-persistence-foundation/spec.md`, add an in-memory H2 database through Spring Data JDBC whose health feeds `/actuator/health` (status-only response). No tables, entities, repositories, seed data or config changes. `spring-boot-h2console` goes on the classpath but stays disabled.

Finding: the spec says intent 001's health and "no other actuator endpoint" assertions must keep passing, but `PetClinicApplicationTests` only has `contextLoads()`, so those tests don't exist. No tests are added in this change (user decision). The spec records this as a deviation, and verification is manual.

## Steps
1. **Branch**: `git checkout -b feat/003-persistence-foundation` (never commit to `main`).
2. **Dependencies** (`pom.xml`, no versions, Boot BOM manages them): add `spring-boot-starter-data-jdbc`, `com.h2database:h2` (`runtime`), `spring-boot-h2console`, and `spring-boot-starter-data-jdbc-test` (`test`). Main deps go next to the existing starters and the test starter next to the other `*-test` starters. Before editing, check the Boot 4.1 artifact names against start.spring.io (dependency ids `data-jdbc`, `h2`). If `spring-boot-h2console` or `spring-boot-starter-data-jdbc-test` doesn't resolve, stop and report it.
3. **Config**: leave `application.yaml` unchanged. Only add settings if a test shows a gap, and record any as a spec deviation.
4. **No new tests** (your decision). Leave `PetClinicApplicationTests` as it is. Its `contextLoads()` still shows that the context starts with the H2 and Data JDBC auto-configuration.
5. **Doc sync**:
   - In spec section 5, add an explicit **Deviation** note: the planned automated tests (health UP with no details, a single `jdbc:h2:mem:` DataSource, an empty `PUBLIC` schema, the fault-injected DOWN check for F5, and `/h2-console` returning 404) were left out by decision. F2, F3, F4, F6 and F7 are checked by the manual smoke run instead, and F5 (DB health feeding the overall status) stays unverified. Concerns 2 and 5 remain open.
   - Update the "Out of scope" section of `intent.md` so the H2 console exclusion matches spec section 4.1 (module present, disabled), as spec assumption 5 asks.
   - Save this plan as `intent/003-persistence-foundation/plan.md`.

## Verification
- `./mvnw clean verify` passes (run `sdk env` first so Java 25 is used).
- `spring-tools:validate`: no diagnostics.
- Manual smoke via the `run` skill:
  - The startup log shows H2 at a `jdbc:h2:mem:` URL with no errors.
  - `curl -i localhost:8080/actuator/health` returns 200 with a body of only `status` UP (plus `groups`), no `components` or `details`.
  - `curl -i localhost:8080/h2-console`, `/actuator/info` and `/actuator/env` all return 404.
  - No `*.mv.db` file appears in the repo root or `~`.
  - Stop the app afterwards.
- `./mvnw dependency:tree | grep -Ei 'h2|data-jdbc|h2console'` to get the resolved H2 and Boot versions for the PR description.
- After pushing, `build.yml` and `snyk.yml` pass. If Snyk reports `high` or above on H2, escalate instead of suppressing.

## Handoff
Commit and open a PR only when asked. Put the H2 and Boot versions in the PR description.
