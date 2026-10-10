# Spec: Owner Details
Source intent: [intent.md](./intent.md)
Author: matthew-js-porter

## 1. Summary
Add a read-only owner details page, the first step toward upstream Spring PetClinic's "Owner Information" page.

- `GET /owners/{id}.html` returns a Thymeleaf page in the shared layout: the owner's Name, Address, City and Telephone in a definition list, then a table of their pets with Name, Birth Date (`yyyy-MM-dd`) and Type. An owner with no pets shows an empty-state message instead of a table.
- `GET /owners/{id}` returns the same owner as JSON, including each pet's birth date and type.
- On the Find Owners page each owner's name becomes a link to that owner's page.
- Seed data gains one owner with no pets (id 11, Helen Ostrander). Owner count goes from 10 to 11, so the list now has 3 pages.
- A missing owner returns 404. Find Owners (otherwise), `/owners` JSON, vets, homepage and `/actuator/health` do not change.
- The design system gains a definition-list component (`DetailList`), documented in `docs/design-system/` in the same PR.
- Out of scope: visits; Edit Owner, Add New Pet, Edit Pet and Add Visit actions (not shown, not disabled); redirecting a single search match to the details page; i18n; security.

## 2. Existing codebase context
- Spring Data JDBC + in-memory H2. `schema.sql` then `data.sql` run on every start with explicit ids, no migration tool. Intent 006 added `owners`, `pets` and `types`, the records `Owner`, `Pet`, `PetType`, the view records `OwnerView` and `PetView(id, name)`, a read-only `OwnerRepository extends Repository<Owner, Integer>` (`findAll(Pageable)`, `findAll(Sort)`), and a package-private `OwnerController`.
- `OwnerController` follows the vets pattern: `GET /owners.html` is the paginated HTML page, `GET /owners` is JSON `{"ownerList": [...]}`. Separate paths, no content negotiation. Page errors use `ResponseStatusException` (400 / 404).
- `Pet` already maps `birthDate` (`LocalDate`) and `type` (`AggregateReference<PetType, Integer>`). Intent 006 deliberately did not display or serialise them, and did not add a `PetTypeRepository`.
- `templates/owners/ownersList.html` shows names as plain text (no anchors) and pets as joined names. `fragments/layout.html` is `layout(pageTitle, activeMenu, content)`; Find Owners is current when `activeMenu == 'owners'`.
- Intent 007 (search) exists as an intent only; no spec or code is on `main`. It will also render the owners table, so the link must survive whichever merges second (see 4.4).
- Design system: `DataTable` is the only record display component. There is no component for one record's details (no `dl` pattern in `docs/design-system/` or `petclinic.css`).
- Existing tests that hard-code 10 owners or "no anchors": `OwnersIntegrationTests` (`hasSize(10)`), `OwnerRepositoryTests` (total 10, page 2 = owners 6 to 10), `OwnersPageHtmlUnitTests` (no-anchor and last-page assertions), `OwnerControllerTests`.

## 3. Requirements

### Functional
| ID | Requirement |
|----|-------------|
| F1 | `./mvnw clean verify` succeeds with all tests passing. |
| F2 | `GET /owners/{id}.html` for an existing owner returns 200 `text/html`, rendered by Thymeleaf with the shared layout. |
| F3 | The page has one `h2` "Owner Information" (class `pc-page-title`) and a definition list with Name (`First Last`), Address, City and Telephone, in that order. |
| F4 | Below it, an `h3` "Pets" and a table (`pc-table`) with columns Name, Birth Date, Type, one row per pet, sorted by name then id. Birth Date is `yyyy-MM-dd`. Type is the pet type's name as stored (e.g. `cat`). George Franklin's page shows Leo, 2010-09-07, cat. Jean Coleman's shows Max then Samantha. |
| F5 | An owner with no pets shows no table. The Pets section shows the message "This owner has no pets." in `pc-muted`. |
| F6 | The seed data gains owner 11 (Helen Ostrander, see 4.1) with no pets. Seed owners 1 to 10 and pets 1 to 13 are unchanged. |
| F7 | On `/owners.html`, each owner's name is a link to `/owners/{id}.html`. The other cells stay plain text. This holds for every page of the list and for search results once intent 007 exists. |
| F8 | The page URL is stable and bookmarkable: it depends only on the owner id, with no session or query state. |
| F9 | `GET /owners/{id}.html` and `GET /owners/{id}` return 404 for an id with no owner (e.g. 999, 0, -1), with no stack trace. A non-numeric id (e.g. `abc`) returns 400. |
| F10 | `GET /owners/{id}` returns 200 `application/json` for an existing owner (see 4.3): `id`, `firstName`, `lastName`, `address`, `city`, `telephone` and `pets`, each pet with `id`, `name`, `birthDate` (`yyyy-MM-dd`) and `type` (the type name). Regardless of the `Accept` header. |
| F11 | The navbar marks Find Owners (`aria-current="page"`) on the details page; exactly one item is current. |
| F12 | The page title is `Owner Information · Spring PetClinic`. |
| F13 | The page works at phone width: the navbar collapses via the existing toggle, labels and values stack, and the page does not scroll horizontally. |
| F14 | Find Owners (apart from the name links and the third page), `/owners` JSON (unchanged shape: pets stay `{id, name}`), vets, vets JSON, homepage and `/actuator/health` behave as before. |
| F15 | With 11 owners, `/owners.html` has 3 pages (5, 5, 1). `?page=3` returns 200 with Helen Ostrander and `none` in Pets; `?page=4` returns 404. `/owners` JSON returns 11 owners. |

### Non-functional and constraints
- Thymeleaf in the existing layout; no client-side fetching, no new JavaScript.
- Spring Data JDBC only; no JPA. No new Maven dependency. Config stays YAML, with no `application.yaml` change; actuator unchanged.
- Read-only: `GET` only, no write methods in any repository. Entities are never serialised directly; view records are.
- Design system rules apply. The new component and any token go in `docs/design-system/` in the same PR.
- Tabs for Java indentation, Boot 4 test imports.

## 4. Design

### 4.1 Seed data
Append to `data.sql` (explicit id, same style as the other owners):

| id | Owner | Address | City | Telephone |
|---|---|---|---|---|
| 11 | Helen Ostrander | 4120 Monroe St. | Fitchburg | 6085554412 |

No pets row references owner 11. The last name does not start with "Dav" or collide with any other seeded last name, so intent 007's "Davis" examples are unaffected. `schema.sql` does not change. Identity counters are still not advanced (explicit ids), so the note from intent 006 stands: the first intent that inserts without an id must `RESTART WITH` past the seed (owner 11, pet 13).

### 4.2 Java (package `org.springframework.samples.petclinic.owner`)
- `OwnerRepository` gains `Optional<Owner> findById(Integer id)`. Still extends `Repository`, no write methods. Spring Data JDBC loads `pets` with the owner.
- `PetTypeRepository extends Repository<PetType, Integer>` with `List<PetType> findAll()`, read-only. Intent 006 deferred it until needed; it is needed now to turn `type` ids into names. Six rows, so the controller loads them into an id-to-name map per request. A join query is not used, to stay on the aggregate model.
- New view records, so the existing list JSON is unchanged (F14): `OwnerDetailView(Integer id, String firstName, String lastName, String address, String city, String telephone, List<PetDetailView> pets)` and `PetDetailView(Integer id, String name, LocalDate birthDate, String type)`. `OwnerView` and `PetView` stay as they are.
- `OwnerController` gains:
  - `GET /owners/{id}.html` (HTML, view `owners/ownerDetails`, model attribute `owner`)
  - `GET /owners/{id}` (`produces = application/json`, `@ResponseBody`, returns `OwnerDetailView` unwrapped)
  - a shared private lookup that throws `ResponseStatusException(NOT_FOUND, "owner " + id + " does not exist")` when `findById` is empty, and maps pets sorted by name then id (reuse the existing comparator idea).
  - Every pet's `type` id has a matching row (foreign key), so no null-type handling is added.
- `Integer` path variable: a non-numeric id gives Spring's standard 400 `MethodArgumentTypeMismatchException`. Ids below 1 are simply not found (404); no extra validation.
- JSON dates: Spring Boot's default Jackson config writes `LocalDate` as `yyyy-MM-dd` (ISO) when timestamps are disabled, which is Boot's default. No configuration is added. A test asserts the format so a change would be caught (see concern 4).

### 4.3 Template `templates/owners/ownerDetails.html`
`layout('Owner Information', 'owners', ~{::content})`, then:

```html
<h2 class="pc-page-title">Owner Information</h2>
<dl class="pc-detail">
  <dt>Name</dt><dd th:text="|${owner.firstName} ${owner.lastName}|">George Franklin</dd>
  <dt>Address</dt><dd th:text="${owner.address}">…</dd>
  <dt>City</dt><dd th:text="${owner.city}">…</dd>
  <dt>Telephone</dt><dd th:text="${owner.telephone}">…</dd>
</dl>
<h3 class="pc-section-title">Pets</h3>
<!-- table when pets exist; <p class="pc-muted"> otherwise -->
```

- Pets table: `table.table.table-striped.pc-table` with a visually hidden `caption` ("Pets of George Franklin"), `th scope="col"` for Name, Birth Date, Type. Three columns, so no `.table-responsive` wrapper is needed (the DataTable rule from intent 006 applies above three columns).
- Birth date uses `${#temporals.format(pet.birthDate, 'yyyy-MM-dd')}` (explicit pattern, not the locale default).
- All values via `th:text`, never `th:utext`.
- Structure choice: a `dl` rather than a two-row table, because it is one record's labelled facts; each `dt`/`dd` pair is exposed to assistive tech as a name/value group. Wrap each pair in a `div` if the CSS needs it (valid in HTML5).

### 4.4 Find Owners template
In `owners/ownersList.html`, the Name cell becomes `<a th:href="@{/owners/{id}.html(id=${owner.id})}" th:text="|${owner.firstName} ${owner.lastName}|">`. Link colour is `spring-green-dark` with the system's visible focus outline, as the design system prescribes for links; the green is not used as text colour. `OwnerView` already carries `id`.

Intent 007 independence (open question 1): this intent is written to ship independently of 007. The link is a one-line change in the shared owners table row, which 007 also renders. Whichever merges second checks that the name cell is still a link; the 007 search-result test and this intent's link test cover it. If 007 merges first, it needs no change and this intent's link covers results automatically as long as results use the same row markup.

### 4.5 URLs and JSON
**Decision:** follow the repo's two-path convention (`/owners.html` + `/owners`, `/vets.html` + `/vets`): HTML at `/owners/{id}.html`, JSON at `/owners/{id}`. This deviates from upstream, where `/owners/{id}` is the HTML page (concern 1).

JSON, unwrapped (a single owner, unlike the list wrapper `ownerList`):
```json
{"id":1,"firstName":"George","lastName":"Franklin","address":"110 W. Liberty St.","city":"Madison","telephone":"6085551023","pets":[{"id":1,"name":"Leo","birthDate":"2010-09-07","type":"cat"}]}
```
`pets` is an empty array for owner 11.

### 4.6 Navbar and layout
No change to `layout.html`. The page passes `activeMenu = 'owners'`, so Find Owners is current (F11). Whether the current item should instead be a breadcrumb/"parent" is deliberately not modelled (concern 5).

### 4.7 Design system and CSS
- **New component `DetailList`** in `docs/design-system/components/DetailList/` (`README.md`, `preview.html`), and an entry in `README.md` and `design-system.json`/`bundle.css` where the other components are listed.
  - Labels (`dt`): Title Case in markup, UPPERCASE via CSS, Montserrat, `ink-muted`-or-`ink` per the tokens (check contrast: text on white must pass 4.5:1; `spring-green` is never used for text).
  - Values (`dd`): Varela Round, `ink`.
  - Layout: at `md` and up, a two-column grid (label column about 12rem, value fills); below `md`, labels stack above values. Square corners, no shadow, hairline `border` between rows, as the table rows use.
  - Section heading: if a `h3` style is missing (`pc-section-title`), add it to the type tokens rather than hard-coding. If an existing token covers it, reuse it and drop the class.
- `components/DataTable/README.md`: add the Pets table as an example, and a note that the empty collection state is a muted sentence, not an empty table.
- Link style in the DataTable (name links) documented in the DataTable README.
- `petclinic.css` gains `.pc-detail` (and `.pc-section-title` if needed) mapped to tokens. No new colours, radii or shadows; if a spacing token is missing, add it to `tokens.json` first.

### 4.8 Security and privacy
- Read-only, `GET` only, unauthenticated. The only input is the `id` path variable, typed as `Integer` and passed to a repository `findById` (parameterised). No SQL is built from input.
- Thymeleaf `th:text` escapes all values (tests include HTML-special characters in a name and address); JSON comes from view records via Jackson, so no entity internals leak.
- This page and `/owners/{id}` JSON expose a single owner's name, address, telephone and their pets' birth dates by guessable sequential id. The data is fictitious, but it makes bulk enumeration trivial (concern 3). No auth, per the intent.
- 404 uses the standard error response; no stack trace or other detail is added.
- No new dependency, so the Snyk gate is unaffected; actuator unchanged.

### 4.9 Out of scope
Visits; Edit Owner / Add New Pet / Edit Pet / Add Visit actions; redirecting a single search match; adding or editing owners and pets; i18n; Spring Security, login, CSRF; a custom error page; content negotiation on one URL.

## 5. Acceptance and verification

### 5.1 Tests
- `OwnerRepositoryTests` (`@DataJdbcTest`): total owners is 11; `findById(1)` returns George Franklin with Leo; `findById(11)` returns Helen Ostrander with no pets; `findById(999)` is empty; Jean Coleman has Max and Samantha; page 1 and 2 of `findAll(PageRequest.of(n, 5, id))` are unchanged (1 to 5, 6 to 10) and page index 2 is owner 11; query count stays bounded. A `PetTypeRepository` test checks the six types.
- `OwnerControllerTests` (`@WebMvcTest`, mocked repositories): `/owners/1.html` is 200, view `owners/ownerDetails`, `text/html`; unknown id is 404 for both `.html` and JSON; `abc` is 400; JSON carries `address`, `city`, `telephone`, and pets with `birthDate` `"2010-09-07"` and `type` `"cat"`; JSON is returned for `Accept: text/html` and `*/*` too; an owner with no pets renders the empty message and no table; HTML-special characters are escaped; `/owners` JSON pets remain `{id, name}`.
- `OwnerDetailsPageHtmlUnitTests` (`@SpringBootTest` + `@AutoConfigureMockMvc`, HtmlUnit, JS off): George Franklin's page title `Owner Information · Spring PetClinic`; one `h2`; definition list shows Name, Address, City, Telephone with the seed values; pets table headers Name, Birth Date, Type, one row `Leo 2010-09-07 cat`; Jean Coleman shows `Max` then `Samantha`; Eduardo Rodriquez shows `Jewel` then `Rosy`; owner 11 shows the empty message and no `table`; only Find Owners is `aria-current="page"`; no edit/add/visit buttons or links on the page.
- `OwnersPageHtmlUnitTests` (updated): on page 1 each Name cell is a link to `/owners/{id}.html`, and following the George Franklin link lands on his details page; other cells have no anchors; the list has 3 pages and page 3 shows Helen Ostrander with `none`; `?page=4` is 404; the "last page has no Next link" assertion moves to page 3.
- `OwnersIntegrationTests` (updated): `/owners` JSON has 11 owners and still 13 pets in total; `/owners/6` JSON has two pets.
- Existing vets, homepage, actuator and context tests stay green unchanged.

### 5.2 Manual check
`./mvnw spring-boot:run`; open `/owners.html`, click George Franklin, verify the details and Leo's row, the Find Owners nav state, keyboard tab order and visible focus outlines (links and nav), and at 320px that the definition list stacks and nothing scrolls horizontally. Open `/owners.html?page=3`, click Helen Ostrander, check the empty state. `curl -i localhost:8080/owners/999.html` is 404; `curl -i localhost:8080/owners/1` is JSON; `/actuator/health` is `UP`.

## 6. Policy conformance
| Policy | How met |
|--------|---------|
| Design system | New `DetailList` component and docs in the same PR; existing DataTable for pets; square corners; no shadows or cards; `spring-green-dark` for links; Title Case markup uppercased by CSS; muted empty state. |
| Accessibility | Landmarks via the layout, heading order h2 then h3, `dl` semantics, hidden table `caption`, `th scope="col"`, one `aria-current`, visible focus on links, stacking at phone width. |
| CLAUDE.md | YAML config untouched; actuator untouched; Spring Data JDBC; UPPER CASE `@Table`/`@Column`; Java 25; Boot 4 test packages; tabs. |
| Security (Snyk, REVIEW.md) | No new dependency; read-only; typed path variable; escaped output; no new data exposure beyond what Find Owners already shows plus pet birth date and type. |
| Workflow | Branch and PR; the plan is written only after this spec has full human review. |

## 7. Areas of concern
1. **URL shape vs upstream.** The intent asks to match upstream's URL shape where practical. Upstream serves HTML at `/owners/{id}`. This repo has a settled rule of HTML at `*.html` and JSON at the bare path, so `/owners/1` returns JSON here and the visitor page is `/owners/1.html`. The two goals conflict. Chosen: repo consistency (no `Accept` edge cases, caches are simple). Alternative: serve HTML at `/owners/{id}` and use `produces`-based content negotiation for JSON, as upstream-like; needs a `Vary: Accept` review and changes the established pattern. Needs human decision.
2. **Empty-state wording vs the `none` rule.** The design system renders empty values as `none` in `ink-muted`. The intent asks for a "clear" empty state for a whole empty collection. A bare `none` under a "Pets" heading is terse; the spec uses a muted sentence ("This owner has no pets."). This is a small, documented extension of the system, not a contradiction, but a designer should confirm.
3. **Enumerable personal data.** Sequential ids plus no authentication make every owner's address and phone number (and pets' birth dates) trivially enumerable. Acceptable only because the data is fictitious and security is out of scope. Real data must not be loaded until authentication and access control exist.
4. **Date format is a default, not a decision.** `yyyy-MM-dd` in JSON depends on Boot's default Jackson settings. The spec adds no config (YAML constraint, minimal change) and relies on a test to catch drift. If the default changes, the fix is an explicit `@JsonFormat` on `PetDetailView.birthDate`, which would be the fallback.
5. **Details page has no back or parent affordance.** Navbar highlights Find Owners as current, as the intent requires, but there is no breadcrumb or "back to list" link and no way to reach the page the visitor came from (page 3, a search) other than the browser back button. Adding one is a design-system change and was left out; the link back is the navbar.
6. **Merge order with intent 007.** The link lives in the table row 007 also edits. Whichever merges second owns the check; both specs' tests should assert it (4.4).
7. **Seed count change ripples.** Moving to 11 owners changes pagination and several existing tests (listed in section 2). They are updated as part of this PR; any other test or doc relying on "10 owners" or two pages should be found by search, including `intent/006` docs, which are historical and left as is.
8. **New design-system component.** `DetailList` has no designer-approved visual reference like the vets/owners HTML boards. The engineer builds `preview.html` and the design owner should review before merge. The docs should also reach the published design-system source to avoid drift.
9. **Type lookup via a second repository.** The controller makes one extra query (six rows) to name pet types. Cheap now, but it is a per-request query the list page does not have; a cache or a join is a later optimisation, not needed here.

## 8. Assumptions
1. Open question 1: this intent ships independently of 007; whichever merges second handles the link (4.4).
2. Open question 2: the pet-less owner is id 11, Helen Ostrander, 4120 Monroe St., Fitchburg, 6085554412 (invented, fictitious, Madison-area style).
3. HTML is at `/owners/{id}.html` and JSON at `/owners/{id}`, following the repo's two-path convention rather than upstream's single path.
4. The `h2` and page title are "Owner Information" (upstream wording); the nav label stays "Find Owners" and `activeMenu` is `'owners'`.
5. Template is `owners/ownerDetails.html`; model attribute is `owner`.
6. Single-owner JSON is unwrapped and also includes `id`, `firstName` and `lastName`, beyond the address, city, telephone and pets the intent lists.
7. Pet `type` in JSON and HTML is the type name string (e.g. `cat`), not the id.
8. Pets are sorted by name then id, as on the list page. Upstream's order differs.
9. Field labels are Name, Address, City, Telephone as in the intent; Pets columns are Name, Birth Date, Type.
10. The empty state is the sentence "This owner has no pets." in `pc-muted`, not a table with zero rows and not a bare `none`.
11. A missing owner is 404 via `ResponseStatusException`, the repo's existing mechanism, with Boot's default error response. A non-numeric id is 400; ids below 1 are 404.
12. Existing `/owners` list JSON keeps `pets` as `{id, name}`; the new detail records are separate so nothing already consumed changes.
13. `PetTypeRepository` is added now (deferred in intent 006) to resolve type names; no join query.
14. The definition list is a new `DetailList` design-system component; the Pets table reuses `DataTable` without `.table-responsive` (three columns).
15. No back link, breadcrumb or action buttons are added.
16. No `application.yaml` change; Jackson's default ISO date output is relied on and tested.
17. Data stays fictitious sample data.

## 9. Handoff notes for engineering
- Branch from `main`; never push to `main`. Keep `intent/008-owner-details/` in sync and record deviations in a section 10 added to this spec.
- Suggested order: seed owner 11 and update count-dependent tests, repository methods and tests, controller and JSON, `ownerDetails.html`, name links in `ownersList.html`, design-system `DetailList` and CSS, HtmlUnit tests, docs.
- Boot 4 test imports live in `org.springframework.boot.webmvc.test.autoconfigure.*`; check the generated imports.
- Run `actionlint` only if workflows change (they should not).
- Check `intent/007-search-owners` status before merging, per concern 6.
