# Spec: Vets
Source intent: [intent.md](./intent.md)
Author: matthew-js-porter

## 1. Summary
Add the first PetClinic feature: a read-only list of veterinarians and their specialties, as in upstream Spring PetClinic.

- Schema and seed data are created at startup from plain SQL scripts (`schema.sql`, `data.sql`) on the in-memory H2 database from intent 003. No migration tool.
- Vets are read with Spring Data JDBC (no JPA).
- `GET /vets.html` renders a paginated (5 per page) Thymeleaf page inside a shared Bootstrap layout with a navigation bar.
- `GET /vets` returns all vets with their specialties as JSON.
- The actuator surface does not change.

This spec also sets the patterns later domain intents (owners, pets, visits) will follow: where SQL lives, package layout, layout fragments, JSON shape, test style. Those choices are in section 4 and listed as assumptions in section 8.

## 2. Existing codebase context
- `pom.xml`: Boot 4.1.1 parent, Java 25. Dependencies are `spring-boot-starter-webmvc`, `-actuator`, `-data-jdbc`, `spring-boot-h2console` (present, disabled), H2 (runtime) and the test starters for actuator, data-jdbc and webmvc. No template engine, no web assets.
- `src/main/resources/application.yaml` contains only `spring.application.name: PetClinic`. YAML only.
- `src/main/java/org/springframework/samples/petclinic/PetClinicApplication.java` is the only production class. No tables, repositories or controllers exist.
- `src/test/java/.../PetClinicApplicationTests.java` has only `contextLoads()`. Intent 003 recorded that the health and endpoint assertions it referred to were never added, and that F5 (DB health feeding overall status) is unverified.
- Code style is tab-indented Java in `org.springframework.samples.petclinic`.
- CI (`build.yml`, `snyk.yml`) runs `./mvnw -B clean verify` and Snyk with a `high` gate; both will see the new dependencies.

## 3. Requirements

### Functional
| ID | Requirement |
|----|-------------|
| F1 | `./mvnw clean verify` succeeds with all tests passing. |
| F2 | On every start, with no manual step, the database contains exactly the six upstream vets and three specialties (table in 4.2), with the upstream vet-to-specialty links. |
| F3 | `GET /vets.html` returns 200 `text/html`, rendered server-side by Thymeleaf, with a table of vets showing name (first and last) and specialties. A vet with no specialty shows the text `none`. |
| F4 | A vet with several specialties shows all of them, in a stable order (alphabetical by name), separated by a space. |
| F5 | The page is paginated at 5 vets per page, ordered by vet id (upstream order). `?page=N` is 1-based; the default is page 1. The page shows previous/next and numbered links to other pages and marks the current page. With the seed data: page 1 has 5 vets, page 2 has 1. |
| F6 | The page uses the shared layout: Bootstrap styling, a navigation bar and a footer, the same on every page. The navigation bar links to the vets page and marks it active. |
| F7 | `GET /vets` with `Accept: application/json` returns 200 with all vets (not paginated), each with `id`, `firstName`, `lastName` and `specialties` (a possibly empty array of `{id, name}`). |
| F8 | `GET /actuator/health` still returns 200 with `status` `UP`; no other actuator endpoint is exposed; no `management.*` setting is added. |
| F9 | Invalid page input is handled without a stack trace or 500: see 4.4. |

### Non-functional and constraints
- Server-side rendering with Thymeleaf; no client-side data fetching. Bootstrap's own JavaScript is allowed for the collapsible navbar only.
- Spring Data JDBC on in-memory H2 (intent 003). No JPA, no Hibernate.
- Config in `application.yaml` only. Do not add `application.properties`.
- Read-only: only `GET` mappings; no create, update or delete.
- Dependency versions come from the Boot BOM where it manages them. Anything not managed must be pinned in a `pom.xml` property and noted as a deviation (see concern 3).
- New dependencies must pass the Snyk gate (no `high` or above).
- Boot 4 package names for test annotations; check generated imports, not Boot 3 memory.

## 4. Design

### 4.1 Dependencies (`pom.xml`)
Add:
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
<dependency>
  <groupId>org.webjars</groupId>
  <artifactId>bootstrap</artifactId>
  <version>${webjars-bootstrap.version}</version>  <!-- 5.3.x, latest patch at implementation time -->
</dependency>
<dependency>
  <groupId>org.webjars</groupId>
  <artifactId>webjars-locator-lite</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-thymeleaf-test</artifactId>
  <scope>test</scope>
</dependency>
<dependency>  <!-- added in implementation, see section 10 -->
  <groupId>org.htmlunit</groupId>
  <artifactId>htmlunit</artifactId>
  <scope>test</scope>
</dependency>
```
Verify the Boot 4 artifact names (the Thymeleaf starter and its test starter, and whether `webjars-locator-lite` is BOM-managed) with `spring-tools:create-spring-boot-project` or start.spring.io, and run `spring-tools:validate`. Add the Bootstrap WebJar version as a property next to `java.version`. The WebJar is chosen over files copied into `static/` so that Snyk Open Source sees Bootstrap and can gate on it. No Thymeleaf layout dialect: use plain Thymeleaf fragments (4.6). No devtools.

### 4.2 Database: schema and seed data
**Decision (open question 1): plain SQL scripts, no migration tool.** Boot's SQL initialisation runs `classpath:schema.sql` then `classpath:data.sql` on every start of an embedded database. This is enough because the database is in-memory and rebuilt on every start. Flyway or Liquibase should be introduced by the intent that adds a persistent or production database; until then, later domain intents append their tables to `schema.sql` and their rows to `data.sql`. `spring.sql.init.*` is left at defaults; a test (section 5) guards the behaviour so a future config change cannot silently remove the seed.

`src/main/resources/schema.sql` (names as upstream):
```sql
CREATE TABLE IF NOT EXISTS vets (
  id         INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  first_name VARCHAR(30) NOT NULL,
  last_name  VARCHAR(30) NOT NULL
);
CREATE INDEX IF NOT EXISTS vets_last_name ON vets (last_name);

CREATE TABLE IF NOT EXISTS specialties (
  id   INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  name VARCHAR(80) NOT NULL
);
CREATE INDEX IF NOT EXISTS specialties_name ON specialties (name);

CREATE TABLE IF NOT EXISTS vet_specialties (
  vet_id       INTEGER NOT NULL,
  specialty_id INTEGER NOT NULL,
  PRIMARY KEY (vet_id, specialty_id),
  FOREIGN KEY (vet_id)       REFERENCES vets (id),
  FOREIGN KEY (specialty_id) REFERENCES specialties (id)
);
```

`src/main/resources/data.sql`, explicit ids (so tests and later foreign keys are deterministic):

| Vet id | Name | Specialties |
|--------|------|-------------|
| 1 | James Carter | none |
| 2 | Helen Leary | radiology |
| 3 | Linda Douglas | dentistry, surgery |
| 4 | Rafael Ortega | surgery |
| 5 | Henry Stevens | radiology |
| 6 | Sharon Jenkins | none |

Specialties: 1 radiology, 2 surgery, 3 dentistry. Seed values are taken from upstream PetClinic as the intent says; the engineer should compare with upstream's `data.sql` and note any difference here. Because ids are inserted explicitly, the identity counter is not advanced; the first intent that inserts rows without an id must `ALTER TABLE ... ALTER COLUMN id RESTART WITH` past the seed. Vets are read-only, so this does not bite here.

### 4.3 Java code
Package `org.springframework.samples.petclinic.vet`, tab-indented, immutable records where Spring Data JDBC allows.

- `Specialty` — `@Table("specialties")` record `(@Id Integer id, String name)`.
- `VetSpecialty` — `@Table("vet_specialties")` record holding `AggregateReference<Specialty, Integer> specialty` (column `specialty_id`).
- `Vet` — `@Table("vets")` record `(@Id Integer id, String firstName, String lastName, @MappedCollection(idColumn = "vet_id") Set<VetSpecialty> specialties)`.
- `VetRepository` — `extends Repository<Vet, Integer>` (not `CrudRepository`, so no write methods exist) with `Page<Vet> findAll(Pageable)` and `List<Vet> findAll(Sort)`, or equivalent. Sorting is by `id`.
- `SpecialtyRepository` — read-only `findAll()`; the table has three rows, so specialty names are resolved from one in-memory lookup per request instead of one query per vet.
- `VetView` / `SpecialtyView` — small records used for both the template model and the JSON body, so the API does not expose persistence types. `VetView` has `id`, `firstName`, `lastName`, `specialties` (sorted by name).
- `VetController` — `@Controller`, constructor-injected repositories, `@GetMapping("/vets.html")` returning the view `vets/vetList` and `@GetMapping(path = "/vets", produces = APPLICATION_JSON_VALUE) @ResponseBody`.
- `RootController` or a `WebMvcConfigurer` view controller — `GET /` redirects to `/vets.html` (see 4.5).

Page size 5 is a named constant in the controller, not configuration. Spring Data JDBC's JDBC paging issues the page query, a count query, and one child-collection query for the page, so it is bounded and not N+1 per vet. The engineer confirms this in the repository slice test.

### 4.4 URLs, status codes and JSON
**Decision (open question 2): upstream URLs.** `/vets.html` for HTML, `/vets` for JSON.

| Request | Result |
|---------|--------|
| `GET /vets.html` | 200 HTML, page 1 |
| `GET /vets.html?page=2` | 200 HTML, page 2 (one vet) |
| `GET /vets.html?page=0`, `?page=-1`, `?page=abc` | 400 with the standard Boot error response (no stack trace). Implemented with a `@Min(1)` constraint or explicit check; non-numeric is already a 400 from MVC type conversion. |
| `GET /vets.html?page=3` (beyond the last page) | 404. Page 1 is always valid, even if there were no vets. |
| `GET /vets` with `Accept: application/json` (or `*/*`) | 200 JSON, all vets |
| `GET /vets` with `Accept: text/html` only | 406 (browsers use `/vets.html`; there is no content-negotiated HTML on `/vets`) |
| `POST/PUT/DELETE` on either path | 405 (Spring default) |

JSON body (keeps upstream's `vetList` wrapper so existing PetClinic consumers still parse it; drops upstream's derived `nrOfSpecialties` and `new` fields):
```json
{"vetList":[{"id":1,"firstName":"James","lastName":"Carter","specialties":[]},
            {"id":3,"firstName":"Linda","lastName":"Douglas","specialties":[{"id":3,"name":"dentistry"},{"id":2,"name":"surgery"}]}]}
```
Ordered by id. Spring Boot 4 uses Jackson 3 (the `jackson-bom.version` property in `pom.xml` is already set); use `tools.jackson` packages if any Jackson types are needed directly (none expected).

### 4.5 Pages, layout and navigation
Templates in `src/main/resources/templates/`, plain Thymeleaf fragments (as upstream, no layout dialect):
- `fragments/layout.html` — `layout(pageTitle, activeMenu, content)` fragment: `<!DOCTYPE html>`, `<html lang="en">`, viewport meta, title, Bootstrap CSS, `petclinic.css`, navbar, `<main>`, footer, Bootstrap bundle JS. Pages call it with `th:replace`.
- `vets/vetList.html` — heading "Veterinarians", table (columns Name, Specialties), pagination control.
- Shared fragment for pagination so the owners list can reuse it.

**Navigation (open question 3), with a deviation from the suggested default.** The intent suggests nav items "Home, Veterinarians". There is no home page and one is out of scope, so a "Home" item would point at a 404. Decision: the nav bar contains the brand link (logo and "PetClinic") and a single item, **Veterinarians**, to `/vets.html`. The brand link goes to `/`, and `/` redirects (302) to `/vets.html`, so the brand link works without building a welcome page. Later intents add their own nav entries. This needs confirmation: it differs from "Home, Veterinarians" and adds a tiny redirect that the intent did not list (concern 5).

**Look.** Bootstrap 5.3 from the WebJar, referenced through `webjars-locator-lite` version-less paths (`/webjars/bootstrap/css/bootstrap.min.css`). A hand-written `static/resources/css/petclinic.css` (about 50 lines) applies the Spring green and dark navbar palette, the heading and table styles, and footer. **Deviation from upstream:** upstream compiles SCSS to CSS in the Maven build; that adds a build plugin, a Node/Sass toolchain and its Snyk surface, so this spec does not. Upstream's images (logo, `pets.png`) are optional: include only the Spring logo (Apache-2.0, from upstream) with a meaningful `alt` or `alt=""` where it is decorative. No hero image, no welcome-page content.

Page content:
- Table with `<caption class="visually-hidden">`, `<th scope="col">`, one row per vet. Name in the first column ("Helen Leary"); specialties in the second, each as plain text separated by a space, or `none`.
- Pagination: Bootstrap `pagination` inside `<nav aria-label="Vets pages">`; first/previous/next/last plus numbered links; the current page has `aria-current="page"` and the disabled controls are not links. Links are built with `@{/vets.html(page=${n})}`.
- All dynamic text uses `th:text` (auto-escaped). `th:utext` is not used anywhere.

### 4.6 Configuration (`application.yaml`)
No change is expected. Thymeleaf's defaults (`classpath:/templates/`, `.html`, caching on) are used. Do not add `management.*`. Do not enable the H2 console. If verification shows a default needs overriding, add it in YAML and record the deviation here.

### 4.7 Out of scope
Adding, editing or deleting vets or specialties; owners, pets and visits and their nav entries; a welcome page beyond the `/` redirect; internationalisation (strings are hard-coded English in templates; use no `messages.properties`); Spring Security, login, CSRF (no forms exist); file-based or production databases; Flyway/Liquibase; a custom error page (Boot's default error handling applies); Bootstrap SCSS compilation; the H2 console.

## 5. Acceptance and verification
Use Boot 4 test packages (for example `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`); confirm imports from the generated starters.

1. **Build:** `./mvnw clean verify` passes.
2. **Seed data (`@DataJdbcTest`):** the six vets exist with the expected names; vet 3 has dentistry and surgery, vets 1 and 6 have none; exactly three specialties. Fails if `schema.sql`/`data.sql` stop running.
3. **Repository:** `findAll(PageRequest.of(0, 5, Sort.by("id")))` returns 5 vets with total 6; page index 1 returns 1 vet. Specialties are populated on the returned vets.
4. **HTML page (MockMvc; `@WebMvcTest` with mocked repositories plus a `@SpringBootTest` check, see 5.1):** `/vets.html` is 200, content type `text/html`, view `vets/vetList`; the body contains "James Carter", "Helen Leary", "radiology", "dentistry surgery" and `none`; exactly 5 rows on page 1, 1 on page 2; the body does not contain "Sharon Jenkins" on page 1; a pagination link to page 2 is present; the nav bar contains a link to `/vets.html`; the page references the Bootstrap CSS.
5. **Page edge cases:** `?page=0`, `?page=-1` and `?page=abc` give 400; `?page=3` gives 404; none leaks a stack trace.
6. **Static assets:** `GET /webjars/bootstrap/css/bootstrap.min.css` and `/resources/css/petclinic.css` return 200.
7. **JSON:** `/vets` with `Accept: application/json` is 200; `$.vetList.length()` is 6; vet 3's `specialties` has two entries; vet 1's is `[]`; no `nrOfSpecialties`.
8. **Read-only:** `POST /vets` returns 405.
9. **Redirect:** `GET /` is 302 to `/vets.html`.
10. **Escaping:** a unit or slice test renders a vet whose name contains `<script>` (via a mocked repository in a `@WebMvcTest`) and asserts the output is escaped.
11. **Actuator (intent 001/003 carry-over):** `/actuator/health` is 200 and `status` is `UP`; `$.components` and `$.details` do not exist; `/actuator/info`, `/actuator/env` and `/h2-console` return 404.
12. **Manual smoke (`run` skill):** start the app; open `http://localhost:8080/vets.html` and check the layout, table, nav and pagination in a browser at desktop and narrow width (navbar collapses and opens); `curl -i localhost:8080/vets -H 'Accept: application/json'`; `curl -i localhost:8080/actuator/health`; restart and confirm the data is still seeded and no `*.mv.db` file appears.
13. `spring-tools:validate` shows no diagnostics; `build.yml` and `snyk.yml` pass on the PR. If Snyk reports `high` or above on Bootstrap, Thymeleaf or a transitive dependency, do not suppress it; raise it.

### 5.1 Tests to be added
Tests sit in `src/test/java/org/springframework/samples/petclinic/vet/` (and the root package for the application-wide ones), tab-indented, JUnit 5 with AssertJ. They use the narrowest slice that proves the behaviour, so most run without starting the full context. The numbers refer to the acceptance list above.

| Test class | Annotation | Covers | Notes |
|------------|------------|--------|-------|
| `VetRepositoryTests` | `@DataJdbcTest` | 2, 3 | Runs `schema.sql` and `data.sql` against embedded H2. Asserts the six vets, vet 3's two specialties, vets 1 and 6 with none, three specialties. Asserts `findAll(PageRequest.of(0, 5, Sort.by("id")))` returns 5 of 6 and page index 1 returns 1, with specialties populated. Confirms the page is read with a bounded number of queries, not one per vet. |
| `SpecialtyRepositoryTests` | `@DataJdbcTest` | 2 | `findAll()` returns radiology, surgery and dentistry. May be folded into `VetRepositoryTests` if it stays small. |
| `VetControllerTests` | `@WebMvcTest(VetController.class)` with `@MockitoBean` `VetRepository` and `SpecialtyRepository` | 4, 5, 7, 8, 10 | See below. |
| `RootRedirectTests` | `@WebMvcTest` (imports the redirect controller or `WebMvcConfigurer`) | 9 | `GET /` is 302 to `/vets.html`. |
| `VetsPageHtmlUnitTests` | `@SpringBootTest` + `@AutoConfigureMockMvc`, HtmlUnit `WebClient` | 4, 6 | Added in implementation (deviation 9): drives the page like a browser through MockMvc's HtmlUnit integration: table rows, pagination clicks, navbar and brand links, assets load. |
| `PetClinicApplicationTests` (existing) | `@SpringBootTest` | boot | Keeps `contextLoads()`. |
| `VetsIntegrationTests` | `@SpringBootTest` + `@AutoConfigureMockMvc` | 4, 6, 7, 11 | A few end-to-end checks with the real database, Thymeleaf and WebJar: `/vets.html` page 1 has 5 rows and page 2 has 1, `/vets` returns six vets, Bootstrap and `petclinic.css` return 200, and the actuator checks. |

**`@WebMvcTest` (`VetControllerTests`).** Repositories are mocked, so the tests do not depend on the seed data and run fast. They check:
- `/vets.html` is 200, `text/html`, view `vets/vetList`, with the `Page` of `VetView`s in the model.
- The body shows each vet's name, specialties separated by a space (`dentistry surgery`) and `none` for a vet without any; one table row per vet.
- Pagination: with a mocked second page, a link to `?page=2` is present, the current page has `aria-current="page"`, and disabled controls are not links. The navbar links to `/vets.html` and is marked active.
- `?page=0`, `?page=-1` and `?page=abc` give 400, and `?page=3` (beyond the mocked total) gives 404, with no stack trace in the body.
- `/vets` with `Accept: application/json` gives a `vetList` array in id order, `specialties` as `[]` when empty, and no `nrOfSpecialties` or `new`. `Accept: text/html` gives 406.
- `POST /vets` and `POST /vets.html` give 405.
- Escaping: a vet named `<script>alert(1)</script>` is rendered as `&lt;script&gt;...` and never as a raw `<script>` tag.

**`@DataJdbcTest` (`VetRepositoryTests`).** The annotation replaces the datasource with an embedded one and only loads Data JDBC components, so it exercises the real SQL scripts, the `Vet`/`VetSpecialty` mapping and paging, with no web layer. It would fail if `schema.sql`/`data.sql` stopped being applied or the `AggregateReference` mapping was wrong (concern 8).

Naming: `@JdbcDataTest` is not a Spring Boot annotation. The slice for Spring Data JDBC is `@DataJdbcTest`, which this spec uses. (`@JdbcTest` is the plain-JDBC slice and does not load repositories.) In Boot 4 both slices live in modular packages, for example `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` and `org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest`; take the imports from the generated starters (`spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jdbc-test`, already in `pom.xml`) rather than from Boot 3 memory. Mocks use `@MockitoBean`, not the removed `@MockBean`.

Not tested: write operations (none exist), the browser rendering and responsive navbar (manual smoke, item 12), and intent 003's F5, which stays open.

F5 of intent 003 (DB health feeding overall status) is still untested; this intent does not change that and does not claim to close it.

## 6. Policy conformance
No brand, security or UX policy skills or documents exist in this repo or among the available skills (only `intent`, `spec` and Spring tooling), so none could be checked. General defaults were applied, for a policy owner to confirm:
- **Security:** read-only `GET` endpoints, no user-supplied data except `page`, which is bound to an `int` and range-checked; all output is auto-escaped by Thymeleaf; no `th:utext`; queries go through Spring Data (no string-built SQL); no new actuator endpoints; H2 console stays disabled; Bootstrap comes from a dependency Snyk can scan; the Snyk gate applies to all new dependencies.
- **Brand:** the "original PetClinic look" is the only brand reference given by the intent. Spring green and the Spring logo are used as upstream does.
- **UX / accessibility (general defaults, WCAG 2.2 AA as a target):** semantic table with caption and column headers, `lang` on `<html>`, landmark elements (`header`/`nav`/`main`/`footer`), labelled pagination `nav`, `aria-current` on the current page, keyboard-operable navbar toggle, responsive viewport.
- **Repo conventions (`CLAUDE.md`):** YAML-only config, minimal actuator surface, Boot 4 test packages, change through a branch and PR, spec deviations noted explicitly.

## 7. Areas of concern
1. **Colour contrast vs. "original look".** Upstream's Spring green (`#6db33f`) with white text is about 2.7:1, below the WCAG AA minimum of 4.5:1 for normal text. Using it as a text or button background fails accessibility; using a darker green or dark text on the green departs from the original look. Decision in this spec: keep the green for accents and the navbar background, but use dark text on it and check every pair with a contrast checker; any remaining gap is raised in the PR. No policy exists to say which wins.
2. **No security headers.** There is no Spring Security (out of scope), so the app sends no CSP, `X-Frame-Options` or `X-Content-Type-Options` beyond Boot/Tomcat defaults. The page has no forms or inline scripts, so the risk is low, but a CSP is not configured and Bootstrap's bundle must not require inline scripts. A security-headers decision belongs to the future security intent.
3. **Bootstrap version pinning.** The Boot BOM may not manage `org.webjars:bootstrap`. If not, the version is pinned in `pom.xml`, which departs from the "BOM-managed versions" convention in intent 003. Snyk or Dependabot-style updates are then manual for this one dependency.
4. **Upstream SCSS build not replicated.** The look will approximate upstream, not match it pixel for pixel. Accepted to avoid a Node/Sass toolchain; a human should check the result against the original.
5. **Navigation and `/` redirect.** The suggested default was "Home, Veterinarians". With no home page, Home would 404, so this spec ships one nav item plus a brand link and a redirect from `/`. This is a small scope addition and a choice the owner (Matthew) should confirm. Redirecting `/` to the vets list also means the root URL's meaning changes when a real home page is added.
6. **`/vets` is not browser-friendly.** A browser address-bar request to `/vets` sends `Accept: text/html` and gets 406. This matches the intent (HTML at `/vets.html`, JSON at `/vets`) but may surprise users.
7. **Seed data in the production artifact.** `data.sql` runs for every embedded database. That is correct today; when a persistent database is introduced, seed data must move behind a profile or a migration tool, or it will run against real data.
8. **First use of Data JDBC aggregates.** Intent 003 left Data JDBC unexercised. A many-to-many via `AggregateReference` and a join table is the least obvious mapping; if it proves awkward the engineer may use `JdbcClient` or a `@Query` projection instead, noting the deviation here, because later intents will copy the pattern.
9. **Test baseline.** The suite had only `contextLoads()` before this intent. The tests listed in section 5 are the first real ones and set the style for later intents; the actuator checks in item 11 close the gap that intents 001 and 003 left.
10. **Snyk gate on new dependencies.** Thymeleaf and Bootstrap are widely used but have had advisories in older versions. The resolved versions are unverified here.

No directly contradicting policies were found, because no policies exist to contradict; concerns 1 and 2 are where a real policy would likely be needed.

## 8. Assumptions
Choices made where the intent was silent or ambiguous (no questions could be asked in CI):
1. **Schema and seed:** plain `schema.sql` and `data.sql` on the classpath, one file each, appended to by later intents. No Flyway/Liquibase now. Defaults for `spring.sql.init.*` are used.
2. **URLs:** upstream's `/vets.html` (HTML) and `/vets` (JSON).
3. **Navigation:** brand link plus one "Veterinarians" item; `/` redirects to `/vets.html`. Differs from the suggested "Home, Veterinarians" (see concern 5).
4. **Seed values** are upstream's six vets and three specialties, with upstream's links, reproduced from the intent and from memory of upstream; to be checked against upstream's `data.sql`.
5. **Table and column names** follow upstream (`vets`, `specialties`, `vet_specialties`).
6. **JSON shape:** upstream's `vetList` wrapper; each vet has `id`, `firstName`, `lastName`, `specialties[{id,name}]`; `nrOfSpecialties` and `new` are dropped. JSON is not paginated.
7. **Ordering:** vets by id; specialties by name within a vet.
8. **Page parameter:** 1-based `page`; below 1 or non-numeric gives 400; beyond the last page gives 404. Upstream returns an empty page for out-of-range values; this spec chooses explicit errors.
9. **Bootstrap** 5.3.x via a WebJar and `webjars-locator-lite`, with a hand-written `petclinic.css` instead of upstream's compiled SCSS. Only the Spring logo is carried over as an image.
10. **Layout:** plain Thymeleaf fragments, no layout dialect; hard-coded English text, no `messages.properties`.
11. **Package** `org.springframework.samples.petclinic.vet`; records for entities and views; `Repository` (not `CrudRepository`) to keep the model read-only.
12. **Page size** 5 is a code constant, not a configuration property.
13. **Tests** use Boot 4 test starters (`spring-boot-starter-thymeleaf-test` added), MockMvc and `@DataJdbcTest`; actuator assertions are added here even though earlier intents referred to them.
14. **No custom error page**; Boot's default error handling applies.
15. A Snyk failure on a new dependency is escalated to a human, not suppressed or worked around.

## 9. Handoff notes for engineering
One PR is feasible: dependencies, two SQL files, `vet` package (about 8 classes), two templates plus layout fragments, one CSS file, tests. Record resolved versions (Boot, Thymeleaf, Bootstrap WebJar, H2) in the PR description, and list any deviation from this spec in the spec itself. This spec needs full human review before a plan is written.

## 10. Implementation deviations
Recorded during implementation (plan: [plan.md](./plan.md)). The look was settled after this spec by the design system in `docs/design-system/` and the reference mock `PetClinic Vets.html`; where they differ from section 4.5, the implementation follows them.

1. **Look from the design system.** `petclinic.css` implements the tokens in `docs/design-system/tokens.json`: a `#34302d` navbar with a 4px `#6db33f` top bar, `#3f7223` for links and the active nav item, warm-grey stripes, square corners. This resolves concern 1: Spring green is decorative only (accent bar, logo, focus ring on dark), and every text pair meets WCAG AA. The one exception is the disabled pagination chevrons (`#b6b0a8`, 2.2:1), which WCAG exempts as disabled controls.
2. **Pagination is plain text**, `Pages: [ 1 2 ] « ‹ › »` as in the design system's Pagination component, not Bootstrap's boxed `.pagination`. It is not rendered when there is only one page. Disabled chevrons are `<span aria-disabled="true">`, not links.
3. **Brand mark** is the inline SVG heart-and-cross from the mock, not the upstream Spring logo. No image files are added.
4. **Fonts** are Montserrat (600/700, variable) and Varela Round, latin subset, self-hosted in `static/resources/fonts/` under the SIL OFL 1.1 (licences alongside). There is no request to Google Fonts.
5. **Navigation** keeps this spec's decision (section 4.5, confirmed by the owner): the brand link plus one "Veterinarians" item. The mock's "Home" item is intentionally left out. The icon is Bootstrap Icons' `list-ul` (MIT), inlined as SVG, with no icons WebJar.
6. **Page range checks** are explicit in the controller (`ResponseStatusException` 400/404). No `@Min` is used, so no validation starter is needed.
7. **Unquoted identifiers.** Spring Data JDBC quotes mapped table and column names by default, which makes them case-sensitive, and they then miss H2's upper-cased unquoted DDL names. `PetClinicApplication` registers a `BeanPostProcessor` that sets `JdbcMappingContext.forceQuote` to `false`, so `@Table`/`@Column` use the lower-case names from `schema.sql` and resolve like the DDL on any database. Later entities need nothing extra.
8. **Query count.** A page read issues the page query plus **one `vet_specialties` query per vet on the page**, so it is bounded by the page size (at most 1 + 5) but is not the single child query stated in 4.3. `VetRepositoryTests` asserts the bound. The JSON endpoint reads all vets the same way (1 + 6). This is acceptable for the seed data; revisit with single-query loading or a `@Query` projection if vets grow.
9. **HtmlUnit tests** were added at review (`VetsPageHtmlUnitTests`, `org.htmlunit:htmlunit` BOM-managed, test scope). HtmlUnit's JavaScript engine cannot parse Bootstrap 5.3.8's bundle, so these tests run with JavaScript disabled. Asset and paging checks live there rather than being repeated in `VetsIntegrationTests`, and the collapsing navbar stays a manual browser check (5, item 12).
10. **WebJar URLs are versioned when rendered.** Templates use version-less `@{/webjars/bootstrap/...}`, and the generated HTML contains `/webjars/bootstrap/5.3.8/...`. Both forms are served.
11. **Resolved versions:** Spring Boot 4.1.1, Thymeleaf 3.1.5, Bootstrap WebJar 5.3.8 (pinned as `webjars-bootstrap.version`, not BOM-managed: concern 3), webjars-locator-lite 1.1.4, HtmlUnit 4.21.0, H2 2.4.240. Upstream seed data was checked: names, specialties and links match section 4.2.
