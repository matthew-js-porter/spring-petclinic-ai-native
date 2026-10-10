# Spec: Persistence foundation
Source intent: [intent.md](./intent.md)
Author: matthew-js-porter

## 1. Summary
Add an in-memory H2 database wired through Spring Boot's Data JDBC starter, so that later domain intents only add their own tables and repositories. The database's health feeds `/actuator/health`; the response stays status-only. No tables, entities, repositories, seed data, migration tooling, or H2 console.

The change is two dependencies plus tests. No new configuration is expected, because Spring Boot auto-configures an embedded database when H2 is on the classpath.

## 2. Existing codebase context
- `pom.xml`: Boot 4.1.1 parent, Java 25, dependencies `spring-boot-starter-webmvc`, `spring-boot-starter-actuator` and their test starters. There is no datasource or persistence dependency.
- `src/main/resources/application.yaml` contains only `spring.application.name: PetClinic`. Config is YAML only.
- `src/test/java/.../petclinic/PetClinicApplicationTests.java` is the existing test class (from intent 001). Intent 001's health and "no other actuator endpoint" assertions must keep passing.
- CI (`build.yml`, `snyk.yml`) runs `./mvnw -B clean verify` and Snyk with a `high` gate; both will see the new dependencies.

## 3. Requirements

### Functional
| ID | Requirement |
|----|-------------|
| F1 | `./mvnw clean verify` succeeds with all tests passing. |
| F2 | The application starts without errors and connects to an in-memory H2 database. |
| F3 | Nothing is written to disk; each start begins with an empty database (no schema, no data). |
| F4 | `GET /actuator/health` returns HTTP 200 with `status` `UP`. |
| F5 | The database health indicator contributes to the overall status: if the datasource is unavailable, `/actuator/health` is not `UP` (Boot default: 503 `DOWN`). |
| F6 | The health response shows only the status (plus the `groups` array Boot 4.1 already adds, accepted in intent 001). No `components` or `details`. |
| F7 | No actuator endpoint other than `health` is exposed; no H2 console. |

### Non-functional / constraints
- H2, in-memory only. Spring Data JDBC (`spring-boot-starter-data-jdbc`), not JPA.
- YAML only; no `application.properties`.
- No `management.*` settings. Boot defaults already give health-only exposure with `show-details: never` and `show-components: never`.
- Dependency versions are managed by the Boot BOM; do not pin versions in `pom.xml`.
- Dependencies must pass the Snyk gate (no `high` or above).

## 4. Design

### 4.1 Dependencies (`pom.xml`)
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-jdbc</artifactId>
</dependency>
<dependency>
  <groupId>com.h2database</groupId>
  <artifactId>h2</artifactId>
  <scope>runtime</scope>
</dependency>
```
Optionally add `spring-boot-starter-data-jdbc-test` (test scope) if tests need `@DataJdbcTest`; this intent has no repositories, so it is not required. Confirm the artifact names against start.spring.io / the `spring-tools:create-spring-boot-project` skill at implementation time (Boot 4 modularised starters). Do not add devtools or any H2 console module.

### 4.2 Configuration
Leave `application.yaml` unchanged. With H2 on the classpath and no `spring.datasource.url`, Boot auto-configures an embedded in-memory `DataSource` (generated unique name, user `sa`, empty password) and, via the actuator, a `DataSourceHealthIndicator`. No file URL is configured, so nothing touches disk. `spring.h2.console.enabled` defaults to false and is not set. The engineer should verify during implementation that the defaults behave as above; only add explicit settings (for example `spring.datasource.generate-unique-name: true`) if verification shows a gap, and record that as a deviation here.

### 4.3 Code
No production Java is added. `PetClinicApplication` is unchanged; Data JDBC repository scanning finds nothing and that is fine.

### 4.4 Out of scope
Domain tables, entities, repositories, seed data, `schema.sql`/`data.sql`, Flyway/Liquibase, H2 console, file or production databases, other actuator endpoints, health details or components.

## 5. Acceptance and verification
- `./mvnw clean verify` succeeds.
- Test: `@SpringBootTest` + MockMvc asserts `/actuator/health` is 200, `status` is `UP`, and the body has no `components` or `details` (`jsonPath("$.components").doesNotExist()`). Keep intent 001's tests passing.
- Test: context has exactly one `DataSource`, whose JDBC URL starts with `jdbc:h2:mem:` (proves in-memory, nothing on disk).
- Test: the database is empty at start (for example, `INFORMATION_SCHEMA.TABLES` has no tables in the `PUBLIC` schema).
- Test: database health feeds overall status. Close or replace the datasource with one that fails (for example a test configuration whose `getConnection()` throws, or close the pool) and assert `/actuator/health` is not 200/`UP`, still without details. This is the only way to prove F5, because the healthy response deliberately hides the `db` component. Check the Boot 4 package names for the test annotations.
- Manual smoke (via the `run` skill): start the app, `curl -i localhost:8080/actuator/health` gives `200` and `"status":"UP"`; no `h2-console` path responds; no `*.mv.db` file appears in the working directory.
- Run `spring-tools:validate`; no diagnostics.
- CI: `build.yml` and `snyk.yml` pass on the PR. If Snyk reports `high` or above on H2 or a transitive dependency, do not suppress it; raise it (see concerns).

## 6. Policy conformance
No brand, security, or UX policy skills or documents exist in this repo or among the available skills (only `intent`, `spec`, and Spring tooling), so none could be checked. General defaults applied, for a policy owner to confirm:
- **Security:** in-memory only, no credentials to manage beyond H2's default `sa` user (not reachable over the network, since no TCP server or console is enabled); health details hidden; no new endpoints; Boot-managed dependency versions; Snyk gate applies.
- **Brand / UX:** not applicable; no UI. The only user-facing output is the standard Boot health JSON.
- Repo conventions from `CLAUDE.md`: YAML-only config, minimal actuator surface, no `management.*` settings, change via a branch and PR.

## 7. Areas of concern
1. **Health as a coupling point.** A failing database makes the whole application report not `UP`. That is the intent's explicit choice, but it also affects Boot 4's readiness group and any orchestrator probing it. Acceptable for an in-memory DB, which should not fail in practice; revisit when a real database is introduced.
2. **F5 is only testable by fault injection.** Because details and components are hidden, the healthy response cannot show that the DB is checked. The failure test in section 5 is therefore required, not optional.
3. **Snyk gate on a new dependency.** H2 has had high-severity CVEs in older versions. The Boot-managed version should be current, but this is unverified here. If the gate fails and the fix needs a version override in `pom.xml`, that departs from "no pinned versions" and must be noted as a deviation.
4. **Empty Data JDBC setup is unexercised.** With no entities or repositories, Data JDBC wiring is not proven beyond context startup. The first domain intent will be the real test; this is accepted by the intent's scope.
5. **Boot 4 artifact names.** Starter and test-starter names changed in Boot 4; verify at implementation time rather than relying on Boot 3 names.

No contradicting policies were found.

## 8. Assumptions
Choices made where the intent was silent or ambiguous (no questions could be asked in CI):
1. No `application.yaml` changes are needed; Boot's embedded-DB auto-configuration is relied on rather than explicit `spring.datasource.*` settings.
2. H2 is added with `runtime` scope, since no production code references H2 classes directly.
3. "Database unavailable" is taken to mean the DB health indicator reports `DOWN` (Boot default: HTTP 503 with `status: DOWN`). The intent only requires "not `UP`".
4. The `groups` array in the health body is acceptable under "status only" (consistent with intent 001's spec).
5. The test-only Data JDBC starter is optional and left out unless needed.
6. The database is verified empty by inspecting `INFORMATION_SCHEMA`, rather than by an application-level check.
7. A Snyk failure on H2 would be escalated to a human, not suppressed or worked around by the implementer.

## 9. Handoff notes for engineering
Small, single-PR change (two dependencies, tests). Record the resolved H2 and Boot versions in the PR description. This spec needs full human review before a plan is written.
