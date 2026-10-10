# Spec: Find Owners
Source intent: [intent.md](./intent.md)
Author: matthew-js-porter
Visual reference: [PetClinic Owners.html](./PetClinic%20Owners.html) (page 1, page 2 and phone width, built with `docs/design-system/`)

## 1. Summary
Add a read-only owners list, as the first step toward upstream Spring PetClinic's "Find Owners" area.

- New tables `owners`, `pets` and `types`, seeded on every start with upstream's 10 owners, 13 pets and 6 pet types (no manual step).
- `GET /owners.html` returns a Thymeleaf page in the shared layout: a paginated table (5 per page) with Name, Address, City, Telephone and Pets (pet names only, `none` if the owner has no pets).
- `GET /owners` returns all owners as JSON, as `/vets` does for vets.
- The navbar gains a "Find Owners" item in the order Home, Find Owners, Veterinarians, current (`aria-current="page"`) on the owners page.
- No search form, forms, details page, links to owners, visits, i18n or security. Vets page, vets JSON, homepage and `/actuator/health` do not change.

## 2. Existing codebase context
- Spring Data JDBC + in-memory H2 (intent 003). `schema.sql` then `data.sql` run on every start, with explicit ids and no migration tool. Intent 004 set the pattern: records with `@Table("UPPER_CASE")`, a `Repository<T, Integer>` (not `CrudRepository`) exposing only read methods, a controller that maps entities to view records, a `PAGE_SIZE = 5` constant, and `@MappedCollection` children.
- `VetController`: `GET /vets.html` is HTML (paginated, `?page=` 1-based, `page<1` is 400, page beyond the last is 404); `GET /vets` is JSON wrapped as `{"vetList": [...]}`. Vets use separate paths, not content negotiation.
- `fragments/layout.html` is `layout(pageTitle, activeMenu, content)` with a hand-written navbar `<li>` per item (inline Bootstrap Icons SVG, `th:aria-current` keyed on `activeMenu`). `fragments/pagination.html` takes `(page, url, label)`.
- `vets/vetList.html` is the template pattern: `h2.pc-page-title`, `table.pc-table` with a visually hidden `caption`, `th scope="col"`, `td.pc-muted` for `none`.
- `RootController` still redirects `/` to `/vets.html` on `main`. Intent 005 (spec only so far) replaces it with a Home item and `WelcomeController`.
- Tests: `VetRepositoryTests` (`@DataJdbcTest`), `VetControllerTests` (`@WebMvcTest`), `VetsIntegrationTests`, `VetsPageHtmlUnitTests`.
- `docs/design-system/` Iconography: nav icons are thin outline glyphs from Bootstrap Icons, drawn inline here.

## 3. Requirements

### Functional
| ID | Requirement |
|----|-------------|
| F1 | `./mvnw clean verify` succeeds with all tests passing. |
| F2 | On every start the database holds 10 owners, 13 pets and 6 pet types with the ids and values in 4.1, with no manual step. |
| F3 | `GET /owners.html` returns 200 `text/html`, rendered server-side by Thymeleaf with the shared layout. |
| F4 | The page has one `h2` "Owners" (class `pc-page-title`) and a table with columns Name, Address, City, Telephone, Pets. Name is `First Last`. Pets is the owner's pet names only, plain text, joined by a space (as the vets page joins specialties), sorted by name; an owner with no pets shows `none` in `pc-muted`. Names are not links. |
| F5 | The list is ordered by owner id and paginated at 5 per page (`?page=N`, 1-based, default 1), using the shared pagination fragment with url `/owners.html` and label `Owners pages`. With the seed data, page 1 has 5 owners and page 2 has 5. |
| F6 | `?page=0`, `?page=-1`, `?page=abc` return 400 and a page beyond the last returns 404, the same as the vets page, with no stack trace. |
| F7 | `GET /owners` returns 200 `application/json` with all 10 owners (not paginated), each with `id`, `firstName`, `lastName`, `address`, `city`, `telephone` and `pets` (see 4.4), regardless of the `Accept` header. |
| F8 | The navbar lists Find Owners, with a search icon, linking to `/owners.html`, with `aria-current="page"` on the owners page only. Nav order is Home, Find Owners, Veterinarians regardless of build order (see 4.5). Exactly one item is current on each page. |
| F9 | The page title is `Owners · Spring PetClinic`. |
| F10 | The page works at phone width: the navbar collapses via the existing toggle and the table does not cause page-level horizontal scroll (see 4.3). |
| F11 | The vets page, `/vets` JSON, the homepage (or `/` redirect, whichever exists) and `/actuator/health` behave as before. |

### Non-functional and constraints
- Thymeleaf in the existing layout; no client-side fetching, no new JavaScript.
- Spring Data JDBC only; no JPA. No new Maven dependency. Config stays YAML; no `application.yaml` change; actuator unchanged.
- Read-only: `GET` only, no request body, no write methods in any repository.
- Design system rules apply; any new token or component goes in `docs/design-system/` in the same PR.
- Tabs for Java indentation, Boot 4 test imports.

## 4. Design

### 4.1 Schema and seed data
Append to `schema.sql`, in the style of the vets tables (upstream names; unquoted lowercase, which H2 upper-cases to match the `@Table`/`@Column` names):
```sql
CREATE TABLE IF NOT EXISTS types (
  id   INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  name VARCHAR(80) NOT NULL
);
CREATE INDEX IF NOT EXISTS types_name ON types (name);

CREATE TABLE IF NOT EXISTS owners (
  id         INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  first_name VARCHAR(30) NOT NULL,
  last_name  VARCHAR(30) NOT NULL,
  address    VARCHAR(255) NOT NULL,
  city       VARCHAR(80) NOT NULL,
  telephone  VARCHAR(20) NOT NULL
);
CREATE INDEX IF NOT EXISTS owners_last_name ON owners (last_name);

CREATE TABLE IF NOT EXISTS pets (
  id         INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  name       VARCHAR(30) NOT NULL,
  birth_date DATE NOT NULL,
  type_id    INTEGER NOT NULL,
  owner_id   INTEGER NOT NULL,
  FOREIGN KEY (owner_id) REFERENCES owners (id),
  FOREIGN KEY (type_id)  REFERENCES types (id)
);
CREATE INDEX IF NOT EXISTS pets_name ON pets (name);
```
Upstream's `telephone` is `VARCHAR(20)` and `address` is `VARCHAR(255)`; kept.

Append to `data.sql` with explicit ids (values from upstream; the engineer compares with upstream's `data.sql` and notes any difference in section 10):

| Types | |
|---|---|
| 1 cat, 2 dog, 3 lizard, 4 snake, 5 bird, 6 hamster | |

| id | Owner | Address | City | Telephone |
|---|---|---|---|---|
| 1 | George Franklin | 110 W. Liberty St. | Madison | 6085551023 |
| 2 | Betty Davis | 638 Cardinal Ave. | Sun Prairie | 6085551749 |
| 3 | Eduardo Rodriquez | 2693 Commerce St. | McFarland | 6085558763 |
| 4 | Harold Davis | 563 Friendly St. | Windsor | 6085553198 |
| 5 | Peter McTavish | 2387 S. Fair Way | Madison | 6085552765 |
| 6 | Jean Coleman | 105 N. Lake St. | Monona | 6085552654 |
| 7 | Jeff Black | 1450 Oak Blvd. | Monona | 6085555387 |
| 8 | Maria Escobito | 345 Maple St. | Madison | 6085557683 |
| 9 | David Schroeder | 2749 Blackhawk Trail | Madison | 6085559435 |
| 10 | Carlos Estaban | 2335 Independence La. | Waunakee | 6085555487 |

| id | Pet | Born | Type | Owner |
|---|---|---|---|---|
| 1 | Leo | 2010-09-07 | cat | 1 |
| 2 | Basil | 2012-08-06 | hamster | 2 |
| 3 | Rosy | 2011-04-17 | dog | 3 |
| 4 | Jewel | 2010-03-07 | dog | 3 |
| 5 | Iggy | 2010-11-30 | lizard | 4 |
| 6 | George | 2010-01-20 | snake | 5 |
| 7 | Samantha | 2012-09-04 | cat | 6 |
| 8 | Max | 2012-09-04 | cat | 6 |
| 9 | Lucky | 2011-08-06 | bird | 7 |
| 10 | Mulligan | 2007-02-24 | dog | 8 |
| 11 | Freddy | 2010-03-09 | bird | 9 |
| 12 | Lucky | 2010-06-24 | dog | 10 |
| 13 | Sly | 2012-06-08 | cat | 10 |

Ids are inserted explicitly, so the identity counters are not advanced. The first intent that inserts owners, pets or types without an id must `ALTER TABLE ... ALTER COLUMN id RESTART WITH` past the seed (same note as intent 004). No rows are written at runtime here.

### 4.2 Java (package `org.springframework.samples.petclinic.owner`)
Same shape as `vet`:
- `Owner` record: `@Table("OWNERS")`, `@Id Integer id, String firstName, lastName, address, city, telephone`, and `@MappedCollection(idColumn = "OWNER_ID") Set<Pet> pets`.
- `Pet` record: `@Table("PETS")`, `@Id Integer id, String name, LocalDate birthDate`, `@Column("TYPE_ID") AggregateReference<PetType, Integer> type`. `birthDate` and `type` are mapped now because they are in the table and later intents need them, but this page does not display them.
- `PetType` record: `@Table("TYPES")`, `@Id Integer id, String name`, and a read-only `PetTypeRepository` (kept for later pet and visit intents; the owners page does not need it, so add it only if a test uses it, otherwise leave the `types` table and record for the next intent).
- `OwnerRepository extends Repository<Owner, Integer>` with `Page<Owner> findAll(Pageable)` and `List<Owner> findAll(Sort)`. No write methods.
- `OwnerView(Integer id, String firstName, String lastName, String address, String city, String telephone, List<PetView> pets)` and `PetView(Integer id, String name)`. These are what the page and JSON render, so entities (and `birthDate`/`type`) are never serialised directly.
- `OwnerController` (package-private, `@Controller`): `static final int PAGE_SIZE = 5`, sort by `id`, `GET /owners.html` for HTML and `GET /owners` for JSON (the `VetController` structure), page validation identical to `VetController` (400 for `page < 1`, 404 beyond the last page, with the same page-1-always-valid rule).

The `pets` child collection is loaded by Spring Data JDBC with one extra query for the page, not one per owner. The repository test confirms the query count is bounded.

### 4.3 Template `templates/owners/ownersList.html`
The visual reference is [PetClinic Owners.html](./PetClinic%20Owners.html). It drops the 50% `pc-col-name` width for the five-column table (see 4.6).

Same shape as `vetList.html`: `layout('Owners', 'owners', ~{::content})`, `h2.pc-page-title` "Owners", `table#owners.table.table-striped.pc-table` with a visually hidden caption (`Owners and their pets, page N of M`), `th scope="col"` for Name, Address, City, Telephone, Pets; Pets cell `none` / `pc-muted` when empty; then `fragments/pagination :: pagination(${listOwners}, '/owners.html', 'Owners pages')`. All values are output with `th:text` (escaped), never `th:utext`.

**Phone width.** Five columns do not fit at 320px. Wrap the table in Bootstrap's `.table-responsive` so the table scrolls inside its own container while the page does not scroll horizontally. The design system does not currently describe a wide-table pattern; add one line to `components/DataTable/README.md` documenting `.table-responsive` for tables with more than three columns, rather than hard-coding CSS. Telephone cells should not wrap mid-number (`text-nowrap`, a Bootstrap utility). The `.table-responsive` wrapper must stay keyboard-reachable: give it `tabindex="0"`, `role="region"` and an `aria-label` ("Owners table") so keyboard users can scroll it, and check the focus outline is visible (concern 4).

### 4.4 URLs and JSON
**Decision:** follow the vets structure exactly: `GET /owners.html` is the paginated HTML page and `GET /owners` is the JSON (as `/vets.html` and `/vets`). No content negotiation, so no `Accept` or `*/*` edge cases and no `Vary` concern. The navbar and pagination link to `/owners.html`.

`/owners/find` is not mapped: there is no search form, so "Find Owners" goes straight to `/owners.html`. The nav label stays "Find Owners" as the intent specifies, even though no search exists yet (concern 2).

JSON shape, following the vets precedent of an upstream-style wrapper (`{"ownerList": [...]}`):
```json
{"ownerList":[{"id":1,"firstName":"George","lastName":"Franklin","address":"110 W. Liberty St.","city":"Madison","telephone":"6085551023","pets":[{"id":1,"name":"Leo"}]}]}
```
Pets in JSON carry `id` and `name` only, matching what the page shows; birth date and type are left for the owner details intent. JSON is ordered by owner id and unpaginated (10 rows; revisit when owners can be added, see concern 5).

### 4.5 Navbar and layout (`fragments/layout.html`)
- Add a Find Owners `<li class="nav-item">` in the existing `pc-nav-link` markup: `th:href="@{/owners.html}"`, `th:aria-current="${activeMenu == 'owners'} ? 'page' : null"`, label `<span>Find Owners</span>` (Title Case in markup; CSS uppercases). Icon: inline Bootstrap Icons `search` SVG (16x16, `fill="currentColor"`, `aria-hidden="true"`, `focusable="false"`), as the other items inline theirs; no WebJar. The `search` glyph is new to the design system and is documented in `README.md` Iconography and the Navbar README (the system today names only house and list-ul).
- **Order.** Final order is Home, Find Owners, Veterinarians. Intent 005 may land before or after this one:
  - If Home is already in `layout.html`, insert Find Owners between Home and Veterinarians.
  - If not, insert Find Owners before Veterinarians; intent 005's spec must then insert Home before Find Owners. Whichever merges second rebases and checks the order, and the nav-order test (5.1) asserts whichever items exist in this order.
- Layout parameters are unchanged; `activeMenu` for this page is `'owners'`.

### 4.6 Design system and CSS
- No new colours, radii or shadows. The table reuses `pc-table`, `pc-col-name` and `pc-muted`.
- Update in the same PR: `components/Navbar/README.md` (Find Owners item, current on `/owners.html`, `search` icon; update `preview.html` to show it), `components/DataTable/README.md` (the `.table-responsive` wide-table note and the Owners table as an example), `README.md` Iconography (search glyph).
- The Telephone and Pets columns need no new styles; if the Name column's `pc-col-name` width rule hurts the five-column layout, adjust within the existing tokens and note it in section 10.

### 4.7 Security and privacy
- Read-only, `GET` only, two unauthenticated endpoints (`/owners.html` and `/owners` JSON). The only input is `page`, validated as for vets. No SQL is built from input (repository paging only), so no injection surface.
- Thymeleaf `th:text` escapes all values; JSON is serialised by Jackson from view records.
- The data is upstream's fictitious sample data (names, addresses, telephone numbers), so no real personal data is added. When real owner data and security are introduced (out of scope), the unauthenticated list of names, addresses and phone numbers becomes personal data and must be protected (concern 3).
- No new dependency, so the Snyk gate is unaffected; actuator unchanged.

### 4.8 Out of scope
Last-name search form and `/owners/find`; adding or editing owners and pets; owner details page and links; visits; internationalisation; Spring Security, login, CSRF; showing pet type or birth date; a custom error page.

## 5. Acceptance and verification

### 5.1 Tests
- `OwnerRepositoryTests` (`@DataJdbcTest`, like `VetRepositoryTests`): the 10 owners, 13 pets and 6 types exist; George Franklin has Leo; Eduardo Rodriquez has Jewel and Rosy; Carlos Estaban has two pets named Lucky and Sly; owners 1 to 10 each have the expected pet count (owners with none: none in seed, so use a test that inserts an owner without pets in a test transaction to cover `none`, or cover it in the controller test with a mocked repository). `findAll(PageRequest.of(0, 5, Sort.by("id")))` returns 5 of 10 with pets populated and page index 1 returns the other 5; the query count is bounded, not one per owner. Guards against `schema.sql`/`data.sql` ceasing to run.
- `OwnerControllerTests` (`@WebMvcTest`, `MockMvcTester`, mocked repository): `/owners.html` is 200, view `owners/ownersList`, `text/html`; `/owners` gives `application/json` with `ownerList`, `address`, `city`, `telephone` and `pets`; `page=0`, `-1`, `abc` give 400; a page beyond the last gives 404; an owner with no pets renders `none`; HTML-special characters in a name are escaped.
- `OwnersPageHtmlUnitTests` (`@SpringBootTest` + `@AutoConfigureMockMvc`, HtmlUnit, JS off, like `VetsPageHtmlUnitTests`): title `Owners · Spring PetClinic`; one `h2` "Owners"; headers Name, Address, City, Telephone, Pets in order; page 1 has 5 rows starting with George Franklin and showing `110 W. Liberty St.`, `Madison`, `6085551023`, `Leo`; Jean Coleman shows `Max Samantha` (sorted by name); no cell contains an anchor; page 1 has a pagination link to page 2 and following it shows Maria Escobito's neighbours (owners 6 to 10); page 2 has no "Next" link; nav items include Find Owners and are in the order Home (if present), Find Owners, Veterinarians; on `/owners.html` only Find Owners is current, and on `/vets.html` Find Owners is not.
- `OwnersIntegrationTests` (`@SpringBootTest`, real server or MockMvc): JSON from `/owners` has 10 owners and 13 pets in total.
- Existing vets, root/homepage, actuator and context tests stay green unchanged (F11). If `RootController` is still on `main`, its redirect test is untouched.

### 5.2 Manual check
`./mvnw spring-boot:run`; open `/owners.html` in a browser. Verify the Find Owners nav item and its current state, the pagination, keyboard focus outlines (including the scrollable table region), and at 320px wide that the navbar toggle works and only the table region scrolls, not the page. `curl -i localhost:8080/owners.html` is HTML; `curl -i localhost:8080/owners` is JSON; `/actuator/health` is `UP`.

## 6. Policy conformance
| Policy | How met |
|--------|---------|
| Design system | Existing tokens, DataTable, Pagination, Navbar and Footer; `pc-page-title`; square corners; `none` in `ink-muted`; Title Case headers uppercased by CSS; docs updated in the same PR for the search icon and wide tables. |
| Accessibility | Landmarks via the layout, hidden `caption`, `th scope="col"`, `aria-current` on one item, `aria-label` on pagination and on the scrollable region, decorative icon `aria-hidden`, visible focus outlines. |
| CLAUDE.md | YAML-only config untouched; actuator untouched; Spring Data JDBC with UPPER CASE `@Table`/`@Column` names; Java 25; Boot 4 test packages; tabs. |
| Security (Snyk, REVIEW.md) | No new dependency; read-only; no user input beyond a validated page number; output escaped. |
| Workflow | Branch and PR; the plan is written only after this spec has full human review. |

## 7. Areas of concern
1. **Two paths, as vets.** HTML at `/owners.html` and JSON at `/owners`, matching `/vets.html` and `/vets`. Consistent, but `/owners` shows JSON rather than the page a visitor might type; accepted for consistency.
2. **"Find Owners" label with no search.** The intent fixes the label and the order, but the page is only a list; visitors may expect a search field. Accepted, since the search follows in a later intent; the label and the `h2` ("Owners") deliberately differ.
3. **Unauthenticated personal data.** Names, addresses and phone numbers are public with no login. Acceptable only because the data is fictitious sample data and security is out of scope. Real data must not be loaded until authentication and access control exist.
4. **Five columns on a phone.** The design system has no wide-table rule and was derived from a two-column vets page. A scrollable region (`.table-responsive`) is added and documented, which is a new pattern with its own accessibility needs (focusable, labelled). The alternative, stacking rows into labelled blocks on mobile, needs new CSS and design decisions and was rejected as more than this intent asks.
5. **Unpaginated JSON.** Returns all owners as the intent requires. Fine for 10 rows; it is unbounded once owners can be added, so a later intent should add paging or a limit.
6. **Nav order depends on merge order with intent 005.** Described in 4.5; the second intent to merge owns the rebase.
7. **Design-system mirror.** As in intent 005, `docs/design-system/` mirrors a published artifact; the updated docs should also reach the published source to avoid drift.
8. **Seed data reproduction.** The values in 4.1 were reproduced from knowledge of upstream's `data.sql`, not fetched during spec writing. The engineer must diff them against upstream and record differences.
9. **Pets without `type` use.** `types` is seeded and `Pet` maps `type`, though nothing displays it. This is deliberate groundwork per the intent ("owners and their pets exist in the database"), at the cost of a little unused mapping.

## 8. Assumptions
1. HTML is at `/owners.html` and JSON at `/owners`, the same structure as the vets page; the navbar goes straight to `/owners.html`; `/owners/find` is not mapped.
2. Page `<title>` and `h2` are "Owners"; the nav label is "Find Owners" (from the intent); `activeMenu` is `'owners'`; the template is `owners/ownersList.html`; package is `...petclinic.owner`.
3. JSON is wrapped as `{"ownerList": [...]}` following the vets `vetList` precedent, unpaginated, ordered by id; pets in JSON are `{id, name}`.
4. Pet names are sorted alphabetically and joined by a space, matching how specialties are shown on the vets page. Upstream shows them in its own order.
5. Pagination, page validation (400/404) and `PAGE_SIZE = 5` copy the vets behaviour exactly, and rows are ordered by id.
6. Schema column types and index names follow upstream; `telephone` is stored as a string of digits and shown unformatted, as upstream.
7. `birth_date` and `type_id` are in the schema and seeded, and mapped on `Pet`, but are not displayed or serialised.
8. The search icon is the Bootstrap Icons `search` glyph, inlined like the existing icons; no WebJar.
9. Wide-table handling uses Bootstrap `.table-responsive` with a focusable labelled region, documented in the DataTable README.
10. If intent 005 is not yet merged, Find Owners is inserted before Veterinarians and no Home item or homepage is added here.
11. Owner and pet data are upstream's fictitious sample data; no real personal data.
12. `PetTypeRepository` is only added if needed; the `types` table and `PetType` record are the required minimum.
13. No `application.yaml` change.

## 9. Handoff notes for engineering
- Branch from `main`; never push to `main`. Keep `intent/006-find-owners/` in sync and record deviations in a section 10 added to this spec.
- Suggested order: schema and seed, records and repository with its test, controller and JSON, template and nav item, HtmlUnit tests, design-system docs.
- Boot 4 test imports live in `org.springframework.boot.webmvc.test.autoconfigure.*`; check the generated imports.
- Run `actionlint` only if workflows change (they should not).

## 10. Implementation notes and deviations
1. **Seed data matches upstream.** The values in 4.1 were diffed against upstream `spring-petclinic` `src/main/resources/db/h2/data.sql` (main branch). Every type, owner and pet matches. The only difference is mechanical: upstream inserts with `default` ids, and this repo inserts the same ids explicitly (4.1).
2. **Query bound is 2 + page size, not "one extra query for the page" (4.2).** Spring Data JDBC loads a `@MappedCollection` with one query per aggregate, as the vets repository test documents. `OwnerRepositoryTests` asserts at most `2 + PAGE_SIZE` statements per page (page, count and one pets query per owner), which is bounded by the page size rather than the table size. Single-query loading was not enabled.
3. **Wide-table CSS.** `petclinic.css` gains a `.pc-table-region > .pc-table { width: max-content; min-width: 100% }` rule, taken from the visual reference's phone board, so at narrow widths the table scrolls inside its region instead of squeezing its columns. The existing `:focus-visible` rule (2px `focus-ring`) now also covers `.pc-table-region`. No new tokens. Documented in `components/DataTable/README.md`.
4. **Existing test updated for the new nav item.** `WelcomePageHtmlUnitTests` asserted the exact nav list `Home, Veterinarians`. It now expects `Home, Find Owners, Veterinarians` (F8). No other existing test changed.
5. **Nav order.** Intent 005 had already merged, so Find Owners sits between Home and Veterinarians (4.5, first case).
6. **No `PetTypeRepository`.** The repository test checks the 6 types with `JdbcClient`, so per assumption 12 only the `PetType` record and the `types` table were added.
7. **Pet ordering.** Pets are sorted by name, then id, so Carlos Estaban's and Jeff Black's two pets named Lucky keep a stable order.
