# Spec: Modify Owners
Source intent: [intent.md](./intent.md)
Author: matthew-js-porter
Visual reference: [PetClinic Owner Forms.html](./PetClinic%20Owner%20Forms.html) (Add Owner link on Owners, New Owner blank and submitted empty, Edit Owner pre-filled and with two errors, both confirmations on Owner Information, and 320px, built with `docs/design-system/`). It awaits design-owner review (concern 8).

## 1. Summary
Add the first write features, Add Owner and Edit Owner, following upstream Spring PetClinic's flow.

- Find Owners (`/owners.html`) gains an "Add Owner" link to a blank form at `GET /owners/new.html`.
- The owner details page (`/owners/{id}.html`, intent 008) gains an "Edit Owner" link to the same form, pre-filled, at `GET /owners/{id}/edit.html`.
- The form has First Name, Last Name, Address, City and Telephone. All are required; Telephone is exactly 10 digits. Plain HTML `POST` to the resource path (`/owners` or `/owners/{id}`), no JavaScript.
- Valid input is saved and the visitor is redirected (post/redirect/get) to the owner's details page, which shows a one-line confirmation: "New Owner Created" or "Owner Values Updated". Pets are never touched.
- Invalid input redisplays the form with what was typed, a summary message, and a message next to each failed field, tied to the field for assistive tech.
- Editing an unknown owner is 404, as the details page is. Data lives only for the life of the app; a restart resets to the seed.
- The design system gains a stacked form layout, a field-error state, and a documented use of InlineMessage for confirmations. No new colour or radius token.
- This is the first runtime write: it needs write access to owners (kept narrow), an identity-counter reset after the seed, and a Bean Validation dependency (concern 2).
- Out of scope: deleting owners; pets and visits; owner JSON writes; duplicate detection; i18n; login, roles, rate limiting.

## 2. Existing codebase context
- Spring Data JDBC on in-memory H2. `schema.sql` then `data.sql` run on every start with **explicit ids** (owners 1 to 11, pets 1 to 13, types 1 to 6), so the identity counters still start at 1. Intent 006 and 008 both recorded that the first intent that inserts without an id must `RESTART WITH` past the seed. This is that intent.
- `OwnerRepository extends Repository<Owner, Integer>` is deliberately read-only (`findAll`, `findById`, `findByLastNameStartingWithIgnoreCase`); its Javadoc says "no write methods exist". Intent 008's spec also says "no write methods in any repository". This spec lifts that for owners only (see 4.2).
- `Owner` is a record `(@Id id, firstName, lastName, address, city, telephone, @MappedCollection pets)`. Saving an existing `Owner` aggregate through Spring Data JDBC deletes and re-inserts its `pets` rows, which would renumber pet ids. Updates therefore must not go through aggregate `save` (4.2).
- `OwnerController` is package-private and follows the repo convention: HTML at `*.html`, JSON at the bare path. `GET /owners/{id}` is owner JSON for any `Accept`; `GET /owners/{id}.html` is the details page. Page errors use `ResponseStatusException` (400, 404). Layout is `layout(pageTitle, activeMenu, content)`; Find Owners is current when `activeMenu == 'owners'`.
- `pom.xml` has no `spring-boot-starter-validation`; there is no Spring Security. Config is `application.yaml` with only the application name.
- Templates use `th:text` only. `.pc-form` is a one-row flex layout from `sm` up, built for the search form (label over input, button bottom-aligned). There is no stacked multi-field layout, no field-error style and no red or other status colour in `tokens.json`.
- Design system rules that bite here: InlineMessage "Don't: use it for validation errors on a field" and has "no colour-coded success, warning or error variant"; Button "Don't: ... make a link look like a button" and is "used to submit a form"; validation must not be shown "by colour alone"; `spring-green` is never text.
- Tests that this change touches: `OwnerDetailsPageHtmlUnitTests` asserts there are **no edit/add/visit buttons or links** on the details page (intent 008); `OwnersPageHtmlUnitTests` asserts the other cells have no anchors; counts of 11 owners and 3 pages are asserted in several places. `@SpringBootTest` contexts are cached, so a test that writes can leak rows into other tests (4.9).

## 3. Requirements

### Functional
| ID | Requirement |
|----|-------------|
| F1 | `./mvnw clean verify` succeeds with all tests passing. |
| F2 | `/owners.html` has an "Add Owner" link (to `/owners/new.html`) on every page of the list, on search results, and in the "no owners found" state. |
| F3 | `GET /owners/new.html` returns 200 `text/html` in the shared layout: `h2` "New Owner", a blank form with First Name, Last Name, Address, City, Telephone in that order, and a submit button "Add Owner". Page title `New Owner · Spring PetClinic`. |
| F4 | `/owners/{id}.html` has an "Edit Owner" link (to `/owners/{id}/edit.html`) for an existing owner. |
| F5 | `GET /owners/{id}/edit.html` returns 200 for an existing owner: `h2` "Edit Owner", the same form pre-filled with the stored values, submit button "Update Owner". Page title `Edit Owner · Spring PetClinic`. Unknown id (999, 0, -1) is 404 with no stack trace; a non-numeric id is 400. |
| F6 | `POST /owners` with valid input inserts one owner with no pets and redirects (303) to `/owners/{newId}.html`. The new owner has a fresh id above every seed id. |
| F7 | `POST /owners/{id}` with valid input updates only that owner's five columns and redirects (303) to `/owners/{id}.html`. The owner's pets, and their ids, are unchanged. Unknown id is 404 for GET and POST (no row is created); a non-numeric id is 400 on POST as on GET. |
| F8 | Validation, applied after trimming leading and trailing whitespace: First Name, Last Name, Address, City and Telephone are required (not empty after trim); Telephone matches exactly 10 ASCII digits; maximum lengths follow the columns: First Name 30, Last Name 30, Address 255, City 80, Telephone is fixed at 10. Anything else is accepted as typed (accents, apostrophes, hyphens). |
| F9 | Invalid input returns 200 and redisplays the form (same view, served from the POST URL) with every typed value kept (as trimmed), no row written or changed, one message next to each failed field, and one summary message above the form (4.5). |
| F10 | After a successful save the details page shows a one-line confirmation, "New Owner Created" or "Owner Values Updated", once. Reloading the details page, following a link to it, or a direct visit shows no confirmation. |
| F11 | A created owner appears on Find Owners (page order is by id, so last) and is found by last name search, including a case-insensitive prefix. An edited owner shows new values on the list, search, details page and `/owners` and `/owners/{id}` JSON. |
| F12 | The request cannot set an owner id, pets, or any field other than the five. The id comes only from the URL. |
| F13 | The navbar marks Find Owners (`aria-current="page"`) on both form pages, and on the form redisplay after an error; exactly one item is current. |
| F14 | The form works at phone width (320px): fields and button stack, nothing scrolls horizontally, the navbar collapses via the existing toggle. It works with JavaScript disabled. |
| F15 | Find Owners (except the new link), search, owner details (except the new link and the confirmation), owners JSON, vets, vets JSON, homepage and `/actuator/health` behave as before. |
| F16 | Nothing persists across a restart: after a restart the data is exactly the seed (11 owners, 13 pets). |

### Non-functional and constraints
- Thymeleaf in the shared layout; plain form posts; no new JavaScript; no client-side fetching.
- Spring Data JDBC and H2 only; no JPA. Config stays YAML, with no change to `application.yaml`; actuator unchanged.
- One new Maven dependency, `spring-boot-starter-validation` (Boot-managed version), subject to the Snyk gate (concern 2).
- All output through `th:text` / `th:errors` (escaped), never `th:utext`. Entities are never bound from or rendered to the form; a dedicated form record is.
- Design system rules apply. Every new component or token goes into `docs/design-system/` in the same PR (4.7). Tabs for Java indentation, Boot 4 test imports.

## 4. Design

### 4.1 URLs and flow
| Method and path | Purpose |
|---|---|
| `GET /owners/new.html` | blank form |
| `POST /owners` | create; 303 to `/owners/{id}.html` or redisplay |
| `GET /owners/{id}/edit.html` | pre-filled form |
| `POST /owners/{id}` | update; 303 to `/owners/{id}.html` or redisplay |

Decision (reviewer-confirmed, option A): `*.html` marks a GET page only; the bare path is the resource, so writes `POST` to the resource path. GET pages keep the repo's `*.html` convention (as intent 008 did), so they are not upstream's `/owners/new`, `/owners/{id}/edit`. Upstream also redirects to `/owners/{id}`, which here is JSON, so the redirect targets `/owners/{id}.html`. See concern 1.

Routing check: for GET, `/owners/new.html` is a literal and wins over `/owners/{id}.html` (where `new` would be a non-numeric id and a 400). A test pins that. `/owners/{id}/edit.html` has no overlap. The POST paths share their URL with the JSON endpoints (`GET /owners`, `GET /owners/{id}`) but differ by method, and no other `POST` mapping exists for either path, so there is no clash. An unknown id on `POST /owners/{id}` is 404 and a non-numeric id is 400, before any write. Post/redirect/get stops a reload from re-submitting. An error redisplay is served from the POST URL, so a GET of that URL returns owner JSON, not the form; this is the accepted cost of the choice, and the redisplay is never a bookmarkable page.

### 4.2 Java (package `org.springframework.samples.petclinic.owner`)
- **`OwnerForm`** (new record): `firstName`, `lastName`, `address`, `city`, `telephone`, all `String`, with `@NotBlank` and `@Size(max = ...)` per F8 and `@Pattern(regexp = "\\d{10}")` on telephone (Java `\d` is ASCII-only unless `UNICODE_CHARACTER_CLASS` is set; do not set it). Message text lives on the annotations (4.5). This is the only type bound from the request, so id and pets cannot be overposted (F12). Factory `OwnerForm.from(Owner)` for the edit page.
- **Trimming:** `@InitBinder("ownerForm")` registers `StringTrimmerEditor(false)` so values are trimmed and an empty string stays `""` (then fails `@NotBlank`), keeping the redisplayed value as trimmed.
- **Persistence, kept narrow.** `OwnerRepository` stays the read interface. Add the minimum write surface to it, with the class Javadoc updated to say so:
  - Insert: `Owner save(Owner owner)` is used **only** with `id == null` and `pets` empty (`new Owner(null, ..., Set.of())`). Spring Data JDBC inserts the row and returns it with the generated id.
  - Update: `@Modifying @Query("UPDATE OWNERS SET FIRST_NAME = :firstName, LAST_NAME = ..., ADDRESS = ..., CITY = ..., TELEPHONE = ... WHERE ID = :id") void update(...)` (or `boolean`/`int` result). This never touches `PETS`, which is what F7 requires. Column and table names in UPPER CASE, as CLAUDE.md says for explicit names.
  - `save` must never be called for an existing owner (it would delete and re-insert pets). A repository test proves pet ids survive an update, and the Javadoc says why. An alternative that removes the footgun is a small separate write component (concern 3).
- **`OwnerController` gains** four handlers (4.1). Create and update are separate methods sharing a private `form(...)` render helper:
  - `@PostMapping("/owners")` (create) and `@PostMapping("/owners/{id}")` (update) take `@Valid @ModelAttribute("ownerForm") OwnerForm form, BindingResult result, RedirectAttributes flash`. On errors it returns the form view (200). Otherwise it saves, adds the flash attribute `message`, and returns `"redirect:/owners/" + id + ".html"` (303; set `RedirectView` status or use `ResponseEntity` if the default 302 is not acceptable to the tests; both are fine for browsers).
  - Edit handlers first `findById(id)` and throw `ResponseStatusException(NOT_FOUND, "owner " + id + " does not exist")`, the details page's mechanism, before anything is written, on both GET and POST.
  - The details handler (`showOwner`) needs no change: Spring puts flash attributes into the model, and the template reads `message`.
- **Identity counter:** append to `data.sql`, after the owner inserts, `ALTER TABLE owners ALTER COLUMN id RESTART WITH 12;` with a comment. Pets and types are not advanced here because nothing inserts them yet (the next intent that does must). A test inserts an owner and asserts the id is above 11.
- No logging of form values (names, addresses and phone numbers are personal data).

### 4.3 Templates
New `owners/ownerForm.html`, used for both pages. Model attributes: `ownerForm`, `ownerId` (null on create), `heading`, `submitLabel`, `action` (`@{/owners}` on create, `@{/owners/{id}(id=${ownerId})}` on edit). Page title and `h2` come from the heading ("New Owner" or "Edit Owner"); `activeMenu` is `'owners'`.

```html
<h2 class="pc-page-title" th:text="${heading}">New Owner</h2>
<div th:if="${#fields.hasErrors('${ownerForm.*}')}" class="pc-message" role="alert">
  <!-- "Owner not saved: 2 fields need attention." -->
</div>
<form method="post" th:action="${action}" th:object="${ownerForm}" novalidate class="pc-form pc-form-stacked">
  <p class="pc-muted">All fields are required.</p>
  <div class="pc-field" th:classappend="${#fields.hasErrors('firstName')} ? 'pc-field-invalid'">
    <label for="firstName" class="form-label">First Name</label>
    <input type="text" th:field="*{firstName}" maxlength="30" required aria-required="true"
           autocomplete="given-name" class="form-control pc-input"
           th:attr="aria-invalid=${#fields.hasErrors('firstName')} ? 'true', aria-describedby=${#fields.hasErrors('firstName')} ? 'firstName-error'">
    <p th:if="${#fields.hasErrors('firstName')}" id="firstName-error" class="pc-field-error" th:errors="*{firstName}"></p>
  </div>
  <!-- lastName family-name, address street-address, city address-level2,
       telephone: type="tel" inputmode="numeric" autocomplete="tel-national" maxlength="10" -->
  <button type="submit" class="btn pc-button" th:text="${submitLabel}">Add Owner</button>
</form>
```

- Native browser validation bubbles are off (`novalidate`), so server messages are the single, tested, system-styled mechanism; `required` and `aria-required` still expose the requirement to assistive tech. The `maxlength` attributes mirror F8 but the server still enforces everything.
- Labels in Title Case in markup. Each input has a visible `label for`. `autocomplete` tokens meet WCAG 1.3.5. Field order is DOM order = tab order.
- No CSRF hidden input (4.8).
- Details page: after the page title, `<p th:if="${message}" class="pc-message" role="status" th:text="${message}">`; below the `dl`, an "Edit Owner" link `<a th:href="@{/owners/{id}/edit.html(id=${owner.id})}">Edit Owner</a>`, Title Case, link style (see 4.7). The 008 test that forbids edit links is changed (5.1).
- Find Owners: `<a th:href="@{/owners/new.html}">Add Owner</a>` directly under the page title, above the search form, so it never sits between the form and its "no owners found" message (InlineMessage must be directly under the form it answers).

### 4.4 Flash message
Confirmation text travels as a **flash attribute** (session-held, consumed by the next request), not a query parameter. A query parameter would let any link put arbitrary text into a trusted message and would break the stable-URL rule from 008 (F8 there). Cost: a session cookie is created on first save; there is still no login. Flash attributes are plain text and rendered with `th:text`.

### 4.5 Validation messages
Plain sentences naming the field, so they make sense out of context (a screen reader reads them with the field):

| Failure | Message |
|---|---|
| empty after trim | `First Name is required.` (and likewise Last Name, Address, City, Telephone) |
| too long | `First Name must be 30 characters or fewer.` (Last Name 30, Address 255, City 80) |
| telephone not 10 digits | `Telephone must be exactly 10 digits.` |

If telephone is both empty and malformed only one message shows, the "required" one. Summary above the form: `Owner not saved: N field(s) need attention.` (pluralised), `role="alert"` so it is announced on redisplay without JavaScript. Messages are defined on the annotations in `OwnerForm` (no i18n; intent excludes it).

### 4.6 Navbar and layout
No change to `layout.html`. Both form pages pass `activeMenu = 'owners'` (F13).

### 4.7 Design system and CSS
No new colour, radius, shadow or font. Changes in `docs/design-system/` and `petclinic.css`, reflected in `README.md`, `design-system.json` and `bundle.css`:
- **FormField** (extended):
  - *Stacked layout* `.pc-form-stacked`: fields one per row, `space-3` apart (`space-5` between groups is not needed), button below, at most `field-width` from `sm`. The existing one-row search layout is unchanged.
  - *Error state*: error text under the input, `body` style, `ink` (13.1:1), with a 4px `line-strong` left rule (`border-accent`) like InlineMessage; input border grows to `border-strong` (2px) `line-strong`. The words and the weight/shape of the rule carry the meaning, not colour, which matches the system's "never by colour alone" rule. Focus outline unchanged.
  - Helper line "All fields are required." in `pc-muted`.
- **InlineMessage** (usage widened, no new variant): a confirmation at the top of a page after a redirect (`role="status"`) and a form-level summary (`role="alert"`). The "Don't use for field validation" rule stays: per-field errors use the FormField error state. Rewrite the "in place of a result" wording to cover "the result of the last action".
- **Button**: unchanged styling. Document the labels "Add Owner" and "Update Owner" as examples.
- **Links as actions**: "Add Owner" and "Edit Owner" are ordinary `spring-green-dark` links with the visible focus outline, not button-styled (concern 4). Document under the link rules (the DataTable README already holds the name-link style).
- Each changed component gets a `preview.html` state (blank, filled, one error, several errors, confirmation). Check at 320px.
- If a spacing or size token is missing, add it to `tokens.json` first; do not hard-code in CSS.

### 4.8 Security and privacy
- **First write path on an unauthenticated app.** Anyone who can reach the app can create or change any owner (name, address, phone). Accepted by the intent ("security/login" out of scope), recorded as concern 5.
- **CSRF (open question 1): no token**, following upstream, as there is no Spring Security and no login, so no ambient authority exists to forge; an attacker could post directly. This holds only while the app has no session identity. When login arrives, CSRF protection must be added in the same change (concern 5). Snyk Code may still flag a state-changing POST without a token; if it does, treat it as an accepted, documented finding, not a reason to bolt on a half-measure.
- **Injection:** all SQL is parameterised (`@Query` named parameters, Spring Data insert). No SQL is built from input. Output is escaped by `th:text`/`th:errors`; JSON is serialised by Jackson. Tests include `<script>`, `&`, `"` and `'` in every field on both the redisplay and the details page.
- **Mass assignment:** prevented by binding only `OwnerForm` (F12). Tests post `id=99` and `pets=...` and see them ignored.
- **Input bounds:** max lengths from the columns avoid database errors turning into 500s; Tomcat's default form-size limit applies. There is no cap on the number of owners, so repeated posts grow H2 memory without limit (concern 5).
- **Errors:** unknown owner is the standard 404; nothing extra is revealed. Form values are not logged.
- The flash cookie is the container's default session cookie; no custom cookie handling is added.
- New dependency: `spring-boot-starter-validation` (Hibernate Validator, `jakarta.validation`). Boot-managed version; the Snyk Open Source gate (fail on high) applies and must pass.

### 4.9 Tests and the shared in-memory database
Tests that write must leave the data as they found it, because `@SpringBootTest` contexts (and their embedded H2) are cached and reused across test classes, and other tests assert exactly 11 owners and 3 pages. Write tests are `@Transactional` (MockMvc and HtmlUnit-over-MockMvc run on the test thread, so the rollback works). `@DirtiesContext` is the fallback. Identity counters do not roll back, so tests assert the new id is above 11, not that it is 12.

### 4.10 Out of scope
Deleting owners; pets and visits; owner writes through JSON; duplicate-owner detection; i18n; Spring Security, login, roles, CSRF tokens; optimistic locking and conflict detection; rate limiting; a custom error page; a Cancel/back link on the form (see concern 7).

## 5. Acceptance and verification

### 5.1 Tests
- `OwnerRepositoryTests` (`@DataJdbcTest`): inserting an owner via `save` returns an id above 11 and the owner has no pets; `update` changes the five columns and returns the changed row from `findById`; after updating owner 6 (Jean Coleman) her pets still have ids 7 and 8 and names Samantha and Max; updating an unknown id changes nothing; the seed count stays 11 outside the test transaction.
- `OwnerControllerTests` (`@WebMvcTest`, mocked repositories): `GET /owners/new.html` 200 view `owners/ownerForm` with a blank `ownerForm` and form action `/owners`; `GET /owners/{id}/edit.html` 200 pre-filled with action `/owners/{id}`, 404 unknown, 400 non-numeric; `POST /owners` valid create redirects to `/owners/{id}.html` with flash `New Owner Created`; `POST /owners/{id}` valid edit redirects with `Owner Values Updated`; each of the five fields blank, whitespace-only, over length, and telephone `123456789`, `12345678901`, `60855510ab`, `6085551 23`, `٦٠٨٥٥٥١٠٢٣` (non-ASCII digits) return 200 with the right field error and no repository write; values with surrounding spaces are saved trimmed; extra `id` and `pets` parameters are ignored; `/owners/new.html` is routed to the form, not to the details handler; `POST /owners/{id}` on an unknown id is 404 and on a non-numeric id 400, with no write; `GET /owners` and `GET /owners/{id}` JSON still work alongside the POST mappings; `GET /owners/1.html` JSON and HTML are unchanged.
- `OwnerFormPageHtmlUnitTests` (new; `@SpringBootTest` + `@AutoConfigureMockMvc`, HtmlUnit, JS off, `@Transactional`):
  - Blank form: title `New Owner · Spring PetClinic`, one `h2`, five labelled inputs in order with the expected `autocomplete`, one submit button "Add Owner", only Find Owners `aria-current="page"`.
  - Create flow: fill and submit; land on the details page showing the values, `New Owner Created`, and "This owner has no pets."; reload shows no message; Find Owners search for the last name lists the new owner.
  - Edit flow: from George Franklin's details page click "Edit Owner"; form is filled with his values; change City and Telephone, submit; details show the new values, `Owner Values Updated`, and Leo is still listed.
  - Invalid submit: empty form shows five messages and the summary (`role="alert"`); each input has `aria-invalid="true"` and an `aria-describedby` pointing to an element that exists with the message; typed values are kept; a single bad telephone shows exactly one error.
  - Escaping: `<script>alert(1)</script>` in every field is shown as text on redisplay and on the details page; no `script` element from input.
  - No link is styled as a button and no new inline script or `style` attributes are present.
- `OwnerDetailsPageHtmlUnitTests` (updated): replace the "no edit/add/visit buttons or links" assertion with: an "Edit Owner" link to `/owners/{id}/edit.html` is present; there are still no Add Pet / Edit Pet / Add Visit controls and no `button`.
- `OwnersPageHtmlUnitTests` (updated): an "Add Owner" link to `/owners/new.html` is present on page 1, on page 3, on a search result page and on the no-results page; the name-cell links and "other cells have no anchors" assertions still hold.
- `OwnersIntegrationTests`: `/owners` JSON still has 11 owners and 13 pets; a created owner appears in `/owners` and `/owners/{id}` JSON within its own test and is gone after (rollback).
- Existing vets, homepage, actuator and context tests stay green unchanged.

### 5.2 Manual check
`./mvnw spring-boot:run`; open `/owners.html`, click Add Owner; submit empty, then with only a bad telephone, and check messages, kept values and focus order by keyboard; fill in a valid owner and confirm the details page and message, then reload (message gone) and search by last name. Edit George Franklin, check Leo is still listed. Open `/owners/999/edit.html` (404). Check 320px with the keyboard and a screen reader (labels, errors read with the field, summary announced). Disable JavaScript and repeat once. Restart and confirm the new owner is gone. `/actuator/health` is `UP`.

## 6. Policy conformance
| Policy | How met |
|--------|---------|
| Design system | Square, no shadow or card; `spring-green-dark` for links and Button; no `spring-green` text; Title Case markup uppercased by CSS; new states (stacked form, field error) and widened InlineMessage documented in `docs/design-system/` in the same PR. The tension points are listed in concerns 4 and 8. |
| Accessibility | Visible labels, `aria-required`, `aria-invalid`, `aria-describedby` to the error, `role="alert"` summary and `role="status"` confirmation, `autocomplete` tokens, visible 2px focus, DOM order is tab order, errors are text plus a non-colour cue, works at 320px and without JavaScript. |
| CLAUDE.md | YAML only and no config change; actuator untouched; Spring Data JDBC, no JPA; UPPER CASE explicit table and column names; Java 25; Boot 4 test packages; tabs; branch and PR, never `main`. |
| Security (Snyk, REVIEW.md) | One Boot-managed dependency under the Snyk gate; parameterised writes; escaped output; form record prevents overposting; no logging of personal data. Unauthenticated writes and no CSRF token are known gaps (concern 5). |
| Workflow | Branch and PR; the plan is written only after this spec has full human review. |

## 7. Areas of concern
1. **URL shape vs upstream.** The intent asks to match upstream's URLs where practical. Upstream uses `/owners/new` and `/owners/{id}/edit` for both GET and POST, and redirects to `/owners/{id}`. This repo's settled rule is HTML at `*.html` and JSON at the bare path, so `/owners/{id}` is JSON here. Decided (reviewer, option A): GET pages are `/owners/new.html` and `/owners/{id}/edit.html`; writes are `POST /owners` and `POST /owners/{id}`, because `.html` names a representation for GET and a form submission is not one; redirect to `/owners/{id}.html`. Upstream parity (`POST /owners/new`, `POST /owners/{id}/edit`) was rejected. Remaining cost: an error redisplay is served from a URL whose GET is JSON (4.1).
2. **New dependency vs the Snyk gate and a lean pom.** Bean Validation needs `spring-boot-starter-validation`. It is Boot-managed and the idiomatic route (and what upstream uses), but it adds Hibernate Validator to the supply chain and must pass the high-severity gate. Alternative with no dependency: a hand-written Spring `Validator` for the five rules. Chosen: the starter. The cost of the alternative is more code and less standard error handling.
3. **Writing to a deliberately read-only repository.** Intents 006 and 008 made `OwnerRepository` write-free on purpose. A plain `CrudRepository`/`save` for updates would silently delete and re-insert pets (changing ids, and breaking the "pets are not changed" outcome). The spec uses `save` for inserts of pet-less owners only and a targeted `UPDATE` for edits, which keeps the write surface small but leaves `save` available for misuse. Alternative: move writes to a separate component so the read interface stays write-free. Needs an engineering call; either way the "pets unchanged" repository test is mandatory.
4. **Design system forbids what the intent asks for.**
   - *Actions as links.* The intent wants "Add Owner" and "Edit Owner" **actions**. The Button component is used "to submit a form" and says never to make a link look like a button. Chosen: plain links, which are less prominent than a button. Alternative: a one-field `GET` form with a Button that navigates (works without JavaScript, honours the rule, but is an unusual use of a form). A designer should decide.
   - *Field errors.* InlineMessage says not to use it for validation errors, and the system has no error colour and forbids colour-only meaning. Chosen: a FormField error state built from existing tokens (text, a 4px rule, a thicker border). It is less visually urgent than the usual red; this is the system's intent but needs design confirmation.
   - *Confirmation.* The intent's open question 2 is resolved as "InlineMessage, no variant": same style, role `status`, README wording widened. No colour-coded success.
5. **Unauthenticated writes; CSRF decision.** Open question 1 is resolved as "follow upstream, no token". The accepted gaps: anyone who can reach the app can add or change owners and so deface or fill the in-memory DB without limit (no rate limit, no cap); and a forged cross-site POST is possible (with no login there is nothing to forge that a direct request could not do, but an internal-only deployment is exposed to browsers on that network). Real client data must not be entered until authentication, access control, CSRF protection and abuse limits exist. Snyk Code may report the missing token.
6. **Shared in-memory database in tests.** Cached Spring contexts share H2, so a leaking write test breaks the 11-owner and 3-page assertions elsewhere, order-dependently. Mitigation in 4.9 (`@Transactional` rollback); reviewers should check every write test for it. Identity counters do not roll back.
7. **No Cancel or back affordance.** As in intent 008 concern 5, the form has no Cancel link; leaving without saving is the navbar or the browser back button. A text link would be easy but is a design call.
8. **No designer-approved visual reference.** The vets, owners and details pages each had an HTML board; this form now has one too ([PetClinic Owner Forms.html](./PetClinic%20Owner%20Forms.html)), but it is not yet approved. The engineer builds the `preview.html` states and the design owner reviews them before merge; record the outcome in a section 10 of this spec. The published design-system artifact also needs updating to avoid drift (as in 008).
9. **Last write wins.** There is no version column, so two people editing the same owner overwrite each other silently. A `schema.sql` version column and optimistic locking are out of scope; flagged for a later intent.
10. **Intent 008 spec conflicts.** 008 says the details page has no edit actions and that repositories have no write methods. Both are intentionally superseded here; the affected 008 test is updated (5.1) and the deviation is noted in section 10 when implemented.
11. **Counter reset is manual.** `RESTART WITH 12` is hard-coded in `data.sql`. Adding a seed owner later without bumping it makes the first create fail with a duplicate key. The repository test for "new id above the seed" catches it.

## 8. Assumptions
1. Open question 1: no CSRF token, following upstream; revisit with authentication.
2. Open question 2: confirmation uses InlineMessage with no new variant; its README wording is widened.
3. GET page URLs follow the repo's `*.html` convention, not upstream's bare paths; form submissions `POST` to the bare resource paths (`/owners`, `/owners/{id}`); redirect goes to `/owners/{id}.html`.
4. The form is one Thymeleaf template `owners/ownerForm.html` for both create and edit; headings "New Owner" and "Edit Owner"; buttons "Add Owner" and "Update Owner" (upstream labels); page titles follow the headings.
5. Bean Validation via `spring-boot-starter-validation` on a dedicated `OwnerForm` record, not on the `Owner` entity.
6. Leading and trailing whitespace is trimmed from all five fields before validation and saving; internal spaces are kept. "Required" means non-empty after trim.
7. Maximum lengths follow the database columns (30, 30, 255, 80); the intent names none, and exceeding them would otherwise be a 500.
8. Telephone is exactly 10 ASCII digits, stored as typed; no spaces, dashes, brackets, `+` or country code are accepted or reformatted.
9. No other character restrictions on names, address or city.
10. Error redisplay returns HTTP 200 (as upstream), not 422.
11. A form-level `role="alert"` summary is added for no-JavaScript users, in addition to per-field messages; focus is not moved programmatically.
12. Native browser validation is disabled with `novalidate` so server messages are the single mechanism; `required` is still marked for assistive tech.
13. The confirmation is a flash attribute, not a query parameter; a session cookie appears after the first save.
14. "Add Owner" and "Edit Owner" are text links, not button-styled; "Add Owner" sits under the page title above the search form; "Edit Owner" sits under the owner's `dl`.
15. Edits use a targeted `UPDATE` so pets are untouched; inserts use `save` with no id and no pets.
16. Only the owners identity counter is advanced (`RESTART WITH 12`); pets and types are not, as nothing inserts them yet.
17. New owners get no pets; the details page shows the existing empty-state sentence.
18. No Cancel/back link, no duplicate detection, no optimistic locking, no rate limiting.
19. The seed data and the order on Find Owners (by id) are unchanged; a new owner appears last.
20. Data stays fictitious; no real personal data is entered.

## 9. Handoff notes for engineering
- Branch from `main` after intent 008 is merged (it is); never push to `main`. Keep `intent/009-modify-owners/` in sync and record deviations in a section 10 added to this spec.
- Suggested order: dependency and identity reset (with the "new id above seed" test), `OwnerForm` and repository write methods with tests, controller handlers, `ownerForm.html`, the two links and the flash message, design-system changes and CSS, HtmlUnit tests, docs.
- Boot 4 test imports are under `org.springframework.boot.webmvc.test.autoconfigure.*`; check the generated imports. Update `OwnerRepository` Javadoc and any CLAUDE.md line that says the repositories are read-only (none today; check).
- Run `actionlint` only if workflows change (they should not).
- Do not widen the actuator, add `application.properties`, or add `management.endpoints`.
- Resolve with a human before coding: concerns 1, 2, 3 and 4 (URLs, the dependency, the repository shape, the link and error treatment). Request design-owner review of the `preview.html` states before merge (concern 8).
- Cross-check every write test for rollback (concern 6).
