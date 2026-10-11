# Spec: Add or Edit a Pet
Source intent: [intent.md](./intent.md)
Author: matthew-js-porter
Visual reference: [PetClinic Pet Forms.html](./PetClinic%20Pet%20Forms.html) (Add New Pet link and Actions column on Owner Information, with and without pets; New Pet blank, submitted empty and with a duplicate name; Edit Pet pre-filled; both confirmations; and 320px, built with `docs/design-system/`). It awaits design-owner review (concern 8).

## 1. Summary
Add the pet write features, Add New Pet and Edit Pet, following upstream Spring PetClinic's flow, on top of the owner form conventions from intent 009.

- The owner details page (`/owners/{id}.html`) gains an "Add New Pet" link to a blank form at `GET /owners/{ownerId}/pets/new.html`, and each pet row gains an "Edit Pet" link to the same form pre-filled at `GET /owners/{ownerId}/pets/{petId}/edit.html`.
- The form has Name (text), Birth Date (native `<input type="date">`) and Type (a `<select>` of the six existing pet types). Plain HTML `POST` to the resource path (`/owners/{ownerId}/pets` or `/owners/{ownerId}/pets/{petId}`), no JavaScript.
- Valid input is saved and the visitor is redirected (post/redirect/get, 303) to the owner's details page, which shows "New Pet has been Added" or "Pet details has been edited" once.
- Invalid input redisplays the form with what was typed, a summary message and a message next to each failed field, tied to the field for assistive tech. Rules: name required; name unique per owner ignoring case (editing without renaming is allowed); birth date required and not in the future; type required.
- Unknown owner, unknown pet, or a pet that belongs to another owner is 404 on both GET and POST.
- An owner with no pets can be given a first pet; the empty state then gives way to the pet list. Owner JSON reflects the changes with no shape change.
- This is the first write to `pets`. It needs a narrow write surface (insert and update by `@Query`, never an `Owner` aggregate save), and an identity-counter reset for `pets` after the seed.
- The design system gains a select field, a date field and a table-actions link pattern, documented in the same PR. No new colour, radius or shadow.
- Out of scope: deleting pets; visits and the Add Visit action; pet JSON writes; persistence across restarts; i18n; login and CSRF.

## 2. Existing codebase context
- Spring Data JDBC on in-memory H2. `schema.sql` then `data.sql` run on every start with **explicit ids** (owners 1 to 11, pets 1 to 13, types 1 to 6), so the identity counters start at 1. Intents 006 and 008 recorded that the first insert without an id must `RESTART WITH` past the seed. Intent 009 does this for `owners` (to 12) and says pets and types are left for "the next intent that inserts them". That is this intent for `pets` (to 14). Types are never inserted, so they are not advanced.
- `Owner` is a record with `@MappedCollection(idColumn = "OWNER_ID") Set<Pet> pets`. `Pet` is a record `(@Id id, name, birthDate, @Column("TYPE_ID") AggregateReference<PetType,Integer> type)`; it has **no owner field**, because the owner id is the collection's id column. Consequences: (a) saving `Pet` on its own cannot set `OWNER_ID`; (b) saving an existing `Owner` aggregate deletes and re-inserts its `pets` rows and renumbers pet ids (the same footgun intent 009's spec records). Neither path is acceptable, so writes use explicit `@Modifying @Query` statements (4.2).
- `OwnerRepository` is read-only (`findAll`, `findById`, `findByLastNameStartingWithIgnoreCase`); `PetTypeRepository` is read-only with `findAll()`. `OwnerController` already holds both and has `ownerDetails(id)`, which maps pets to `PetDetailView(id, name, birthDate, type-name)` sorted by name then id and throws `ResponseStatusException(NOT_FOUND, "owner " + id + " does not exist")`.
- Repo convention: HTML at `*.html`, JSON at the bare path; `GET /owners/{id}` is owner JSON for any `Accept`; page errors use `ResponseStatusException`.
- `templates/owners/ownerDetails.html` renders a `dl`, an "Pets" `h3`, then either the muted "This owner has no pets." sentence or a three-column `pc-table` (Name, Birth Date, Type). The page has no actions today; intent 009 adds "Edit Owner" and a `message` flash banner (`role="status"`).
- **Intent 009 is not on `main` yet** (its spec is on branch `spec/009-modify-owners`; no code). This spec assumes 009 ships first, as the intent's "same as the owner form" constraint implies, and reuses what 009 specifies: `spring-boot-starter-validation`, the dedicated form record plus `@InitBinder` trimming, the flash attribute `message`, the `.pc-form-stacked` layout, `.pc-field-error` and `.pc-field-invalid` states, the `pc-message` summary with `role="alert"`, links-as-actions styling, `novalidate` with server messages, and the "writing tests are `@Transactional`" rule. Names below that come from 009 are marked "(009)". See concern 1.
- Design system today: FormField is a text input only; InlineMessage and Button exist; DataTable says wrap in `.table-responsive` above three columns; there is no select, date or per-row action pattern. 009 adds the stacked layout and the field-error state.
- Tests this touches: `OwnerDetailsPageHtmlUnitTests` asserts no edit, add or visit buttons or links (009 relaxes that for Edit Owner; this intent relaxes it for Add New Pet and Edit Pet and keeps the ban on Add Visit); `OwnerDetailsPageHtmlUnitTests` also asserts the pets table has three headers; tests that assert exactly 13 pets.

## 3. Requirements

### Functional
| ID | Requirement |
|----|-------------|
| F1 | `./mvnw clean verify` succeeds with all tests passing. |
| F2 | `/owners/{id}.html` has an "Add New Pet" link to `/owners/{id}/pets/new.html`, shown for every existing owner whether or not they have pets. |
| F3 | Each pet row on that page has an "Edit Pet" link to `/owners/{ownerId}/pets/{petId}/edit.html`. The accessible name includes the pet's name (4.3). No "Add Visit" or any other action appears. |
| F4 | `GET /owners/{ownerId}/pets/new.html` returns 200 `text/html` in the shared layout: `h2` "New Pet", the owner's name shown as context, a blank form with Name, Birth Date, Type in that order, and a submit button "Add Pet". Page title `New Pet · Spring PetClinic`. |
| F5 | `GET /owners/{ownerId}/pets/{petId}/edit.html` returns 200 for a pet that belongs to that owner: `h2` "Edit Pet", the same form pre-filled (name, birth date as `yyyy-MM-dd`, type selected), submit button "Update Pet". Page title `Edit Pet · Spring PetClinic`. |
| F6 | Type is a select with a leading empty prompt option ("Select a type", value empty) and one option per pet type, labelled with the stored name (cat, dog, lizard, snake, bird, hamster) and ordered by name (4.3). Only the six seeded types are accepted. |
| F7 | `POST /owners/{ownerId}/pets` with valid input inserts one pet for that owner and redirects (303) to `/owners/{ownerId}.html`, which shows "New Pet has been Added" once. The new pet has an id above every seed id. |
| F8 | `POST /owners/{ownerId}/pets/{petId}` with valid input updates only that pet's name, birth date and type, and redirects (303) to `/owners/{ownerId}.html`, which shows "Pet details has been edited" once. The pet's id and owner are unchanged; the owner's other pets and their ids are unchanged; the owner row is not touched. |
| F9 | Validation, after trimming leading and trailing whitespace: **Name** is required and at most 30 characters; **Name is unique among that owner's pets ignoring case** (adding "Leo" or "leo" to George Franklin is rejected). Editing a pet while keeping its name, or changing only its case, is allowed; renaming it to another of the same owner's pets' names is rejected. The same name on a different owner is allowed (seed has Lucky on owners 7 and 10). **Birth Date** is required, must be a valid calendar date in `yyyy-MM-dd`, and must not be after today (today is allowed). **Type** is required and must be one of the existing type ids. |
| F10 | Invalid input returns 200 and redisplays the form (same view, served from the POST URL) with every typed value kept (name as trimmed, birth date as typed, type selected), no row written or changed, one message next to each failed field, and one summary message above the form (4.5). Several failures at once all show. |
| F11 | After a successful save the confirmation shows once. Reloading the details page, following a link to it, or a direct visit shows none. |
| F12 | An owner with no pets (owner 11 or one made through 009) shows "Add New Pet". After saving a first pet, the details page shows the pet table in place of "This owner has no pets." |
| F13 | An unknown owner id (999, 0, -1) is 404 for GET and POST on both the new and edit pages. An unknown pet id, or a pet that exists but belongs to a different owner (e.g. `/owners/1/pets/2/edit.html`, pet 2 is Basil of owner 2), is 404 for GET and POST. No row is created or changed on any 404. A non-numeric id is 400. 404s carry no stack trace. |
| F14 | The request cannot set a pet id, an owner id, or any field other than name, birth date and type. Ids come only from the URL. |
| F15 | Owner JSON (`/owners/{id}`) and `/owners` JSON reflect added and edited pets (names, birth dates, types) with unchanged shape. The Find Owners list shows an added pet's name. |
| F16 | The navbar marks Find Owners (`aria-current="page"`) on both pet form pages and on the redisplay after an error; exactly one item is current. |
| F17 | The form works with JavaScript disabled and at 320px: fields and button stack, nothing scrolls horizontally, the navbar collapses via the existing toggle. The pets table, now with an actions column, is usable at 320px (4.7). |
| F18 | Find Owners, search, vets, vets JSON, homepage, `/actuator/health` and the owner details page (apart from the new links, the actions column and the confirmation) behave as before. |
| F19 | Nothing persists across a restart: after a restart the data is exactly the seed (11 owners, 13 pets). |

### Non-functional and constraints
- Thymeleaf in the shared layout; plain form posts; no new JavaScript; no client-side fetching.
- Spring Data JDBC and H2 only; no JPA. Config stays YAML with no `application.yaml` change; actuator unchanged. No new Maven dependency beyond the validation starter 009 adds.
- All output through `th:text` / `th:errors` (escaped), never `th:utext`. Entities are never bound from or rendered to the form; a dedicated form record is.
- Design system rules apply; every new component or token goes into `docs/design-system/` in the same PR (4.7). Tabs for Java indentation, Boot 4 test imports.
- No Spring Security and no CSRF protection (intent decision; concern 3).

## 4. Design

### 4.1 URLs and flow
| Method and path | Purpose |
|---|---|
| `GET /owners/{ownerId}/pets/new.html` | blank form |
| `POST /owners/{ownerId}/pets` | create; 303 to `/owners/{ownerId}.html` or redisplay |
| `GET /owners/{ownerId}/pets/{petId}/edit.html` | pre-filled form |
| `POST /owners/{ownerId}/pets/{petId}` | update; 303 to `/owners/{ownerId}.html` or redisplay |

Decision (as 009, reviewer-confirmed): `*.html` marks a GET page only; the bare path is the resource, so writes `POST` to the resource path. GET pages keep the repo's `*.html` convention, so they are not upstream's `/owners/{ownerId}/pets/new` and `/owners/{ownerId}/pets/{petId}/edit`. Upstream redirects to `/owners/{ownerId}`, which here is JSON, so the redirect targets `/owners/{ownerId}.html`. See concern 2.

Routing check: `/owners/{id}.html` has one path segment after `owners`, and these paths have more. The GET literal `/owners/{ownerId}/pets/new.html` does not overlap `POST /owners/{ownerId}/pets/{petId}` (different method and suffix). No other `POST` mapping exists for either POST path. An unknown owner or pet is 404 and a non-numeric id is 400, before any write. An error redisplay is served from the POST URL, so a GET of that URL is not the form page; this is the accepted cost, as in 009.

### 4.2 Java (package `org.springframework.samples.petclinic.owner`)
- **`PetForm`** (new record): `name` (`String`), `birthDate` (`String`, ISO text), `typeId` (`String`, digits as text). It is the only type bound from the request (F14). Bean Validation on the record: `@NotBlank` and `@Size(max = 30)` on `name`; `@NotBlank` on `birthDate`; `@NotBlank` on `typeId`. Message text lives on the annotations (4.5). Factory `PetForm.from(PetDetailView or Pet)` for the edit page. `@InitBinder("petForm")` registers `StringTrimmerEditor(false)` as 009 does.
  - Why `typeId` is text too: with an `Integer` field, `typeId=abc` fails at binding with Spring's generic `typeMismatch` text instead of "Type is required.". The controller parses it with `Integer.parseInt` (a `NumberFormatException` is treated as unknown), so the message stays ours and the typed value survives redisplay; the select matches options by comparing text.
  - Why `birthDate` is text, not `LocalDate`: with a `LocalDate` field, a malformed or out-of-range value (a browser without a date picker, a hand-made POST) produces Spring's generic type-mismatch text, which would need a messages file (i18n is out of scope, and config is YAML). Parsing in the controller with `LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE)` lets the messages be ours and keeps the typed text for redisplay. See assumption 8.
- **Cross-field and contextual checks** in the controller, after `@Valid`, adding to the `BindingResult` with `rejectValue` so they show next to the field:
  - birth date parses (`DateTimeParseException` becomes "must be a date in the form yyyy-mm-dd") and is not after `LocalDate.now(clock)` (a `Clock` bean is added so tests can fix "today"; see concern 6);
  - `typeId` parses as an integer and is in the loaded type ids (non-numeric such as `abc`, or an unknown id; otherwise the same "Type is required." message, which also stops a crafted id from reaching the foreign key as a 500);
  - name is not equal, ignoring case, to any **other** pet of this owner (compare ids, so a pet never conflicts with itself). `String.equalsIgnoreCase` is used; seed names are ASCII and the rule is "ignoring case" as upstream.
  - Only one message per field: required beats format beats rule.
- **Persistence, kept narrow.** Add a small `PetRepository extends Repository<Pet, Integer>` rather than widening `OwnerRepository`:
  - `@Modifying @Query("INSERT INTO PETS (NAME, BIRTH_DATE, TYPE_ID, OWNER_ID) VALUES (:name, :birthDate, :typeId, :ownerId)") void insert(...)`. The generated id is not needed because the flow redirects to the owner.
  - `@Modifying @Query("UPDATE PETS SET NAME = :name, BIRTH_DATE = :birthDate, TYPE_ID = :typeId WHERE ID = :id AND OWNER_ID = :ownerId") int update(...)`. The `OWNER_ID` predicate means a pet can only be changed through its own owner's URL even if a check were missed; a result of 0 is treated as 404. It never moves a pet between owners.
  - No `save`, no `delete`, no `Owner` aggregate writes. The Javadoc says why (aggregate save renumbers pets). Table and column names in UPPER CASE (CLAUDE.md). An alternative is a single write component shared with owners (concern 4).
- **`OwnerController` gains** four handlers (4.1) with the same `form(...)` render helper style 009 uses:
  1. Load the owner with `findById`, else `ResponseStatusException(NOT_FOUND, "owner " + id + " does not exist")`. This happens first on GET and POST, before any write.
  2. For edit, find the pet in `owner.pets()` by `petId`, else `ResponseStatusException(NOT_FOUND, "pet " + petId + " does not exist")`. A pet of another owner is not in this owner's set, so it gets the same message (no leak of whether it exists elsewhere).
  3. On POST: `@Valid @ModelAttribute("petForm") PetForm form, BindingResult result, RedirectAttributes flash`; contextual checks; on errors return the form view (200). Otherwise insert or update, set flash `message`, return `"redirect:/owners/" + ownerId + ".html"` (303).
  - Model attributes for the view: `petForm`, `owner` (name for context, as the existing `OwnerDetailView` or a small record), `petTypes` (id and name list, name order), `heading`, `submitLabel`, `action` (`@{/owners/{ownerId}/pets}` on create, `@{/owners/{ownerId}/pets/{petId}}` on edit), `today` (ISO, for the `max` hint).
  - The details handler needs no change for the banner (flash reaches the model as `message`; 009 adds the template line). It needs pet ids and names in `PetDetailView`, which it already has (`id`, `name`), for the Edit Pet link and its accessible name.
- **Controller size.** `OwnerController` is growing (009 added four handlers; this adds four more). Putting the pet handlers in a new package-private `PetController` in the same package is allowed and is the suggested shape; it reuses `OwnerRepository` and `PetTypeRepository`. Left to the engineer (assumption 15).
- **Identity counter:** append to `data.sql`, after the pet inserts, `ALTER TABLE pets ALTER COLUMN id RESTART WITH 14;` with a comment. A test inserts a pet and asserts its id is above 13. Types are not advanced.
- No logging of form values; pet names are low-sensitivity but owners' pets tie to personal data.

### 4.3 Templates
New `owners/petForm.html`, used for both pages, `layout(heading, 'owners', ~{::content})`.

```html
<h2 class="pc-page-title" th:text="${heading}">New Pet</h2>
<p class="pc-muted">Owner: <span th:text="|${owner.firstName} ${owner.lastName}|">George Franklin</span></p>
<div th:if="${#fields.hasErrors('${petForm.*}')}" class="pc-message" role="alert"><!-- "Pet not saved: 2 fields need attention." --></div>
<form method="post" th:action="${action}" th:object="${petForm}" novalidate class="pc-form pc-form-stacked">
  <p class="pc-muted">All fields are required.</p>
  <div class="pc-field" th:classappend="${#fields.hasErrors('name')} ? 'pc-field-invalid'">
    <label for="name" class="form-label">Name</label>
    <input type="text" th:field="*{name}" maxlength="30" required aria-required="true" class="form-control pc-input"
           th:attr="aria-invalid=..., aria-describedby=..."> <!-- as 009 -->
    <p th:if="${#fields.hasErrors('name')}" id="name-error" class="pc-field-error" th:errors="*{name}"></p>
  </div>
  <!-- Birth Date: type="date" th:field="*{birthDate}" th:max="${today}" required; helper "yyyy-mm-dd" for browsers without a picker -->
  <!-- Type: <select th:field="*{typeId}" class="form-select pc-input" required> prompt option + th:each over petTypes -->
  <button type="submit" class="btn pc-button" th:text="${submitLabel}">Add Pet</button>
</form>
```

- Native bubbles off (`novalidate`); `required` and `aria-required` still expose the requirement. `maxlength` mirrors F9; the server enforces everything. `max` on the date input is only a picker hint.
- Labels: Name, Birth Date, Type, in Title Case in markup, each with `label for`. Field order is DOM order is tab order. The owner context line is plain text, not a field.
- A pet's name has no `autocomplete` token that fits; use `autocomplete="off"` on Name so browsers do not offer the visitor's own name. The date input uses `autocomplete="off"` too.
- Select: the prompt option is `<option value="">Select a type</option>`, selected when nothing is chosen; the chosen type is kept on redisplay.
- No CSRF hidden input (4.8).
- **Details page changes** (`ownerDetails.html`):
  - "Add New Pet" `<a th:href="@{/owners/{id}/pets/new.html(id=${owner.id})}">` directly under the "Pets" `h3`, above the table or empty-state sentence, so it is found in the same place whatever the pet count. It is an ordinary link, not a button (Button rule: never make a link look like a button).
  - Pets table gains a fourth column with a visible header "Actions" (`th scope="col"`), each cell holding `<a>Edit Pet<span class="visually-hidden"> Leo</span></a>`, so a screen-reader link list reads "Edit Pet Leo". The header "Actions" is the one column without data; see concern 5.
  - Four columns exceed the DataTable three-column rule, so the table is wrapped in `.table-responsive` (as the Find Owners table is).
  - Confirmation banner is 009's `<p th:if="${message}" class="pc-message" role="status">`; nothing new.
- Type ordering: the select is sorted by name (bird, cat, dog, hamster, lizard, snake), as upstream, so a visitor can scan it. The intent lists the types in id order, but that is a listing of what exists, not a requested order (assumption 6).

### 4.4 Flash message
As in 009 (4.4): the confirmation is a flash attribute `message`, consumed by the next request, not a query parameter. Texts are exactly "New Pet has been Added" and "Pet details has been edited" (the intent's wording, upstream casing and grammar kept; concern 7).

### 4.5 Validation messages
Plain sentences that name the field, so they make sense read out of context:

| Failure | Message |
|---|---|
| name empty after trim | `Name is required.` |
| name too long | `Name must be 30 characters or fewer.` |
| name already used by another pet of this owner | `This owner already has a pet with this name.` |
| birth date empty | `Birth Date is required.` |
| birth date not a valid date | `Birth Date must be a date in the form yyyy-mm-dd.` |
| birth date after today | `Birth Date cannot be in the future.` |
| type empty or not a known type | `Type is required.` |

Only one message per field. The summary above the form is `Pet not saved: N field(s) need attention.` (pluralised), `role="alert"`. Messages are defined in code (no i18n).

### 4.6 Navbar and layout
No change to `layout.html`. Both pet form pages pass `activeMenu = 'owners'` (F16).

### 4.7 Design system and CSS
No new colour, radius, shadow or font. Changes go in `docs/design-system/` and `petclinic.css`, reflected in the component README(s), the top-level `README.md`, `design-system.json` and `bundle.css` as 009 did:
- **FormField** (extended, building on 009's stacked layout and error state):
  - *Select variant*: same label, border, padding, `radius-none`, `field-width` and focus outline as the text input. Bootstrap's `form-select` is used with the radius reset; keep the browser chevron (a native control keeps keyboard and screen reader behaviour). Check the border and focus meet contrast.
  - *Date variant*: the native date input styled as a text input; the picker is the browser's. The helper line "yyyy-mm-dd" in `pc-muted` covers browsers that fall back to a text box.
  - *Error state* applies to both, as 009 defines it (words plus a 4px rule, never colour alone).
- **DataTable** (extended): documents an actions column (header "Actions", Title Case in markup and uppercase by CSS, link per row with a hidden name for assistive tech) and notes it counts toward the three-column `.table-responsive` rule.
- **Links as actions**: "Add New Pet" and "Edit Pet" are `spring-green-dark` links with the visible 2px focus outline, as 009 documents for "Add Owner" and "Edit Owner". Add them to the examples.
- **Button**: unchanged; add "Add Pet" and "Update Pet" as label examples.
- **InlineMessage**: unchanged; reuses 009's confirmation and summary usage.
- Each changed component gets `preview.html` states: blank, filled, one error, several errors, select open state noted, 320px width.
- If a spacing or size token is missing, add it to `tokens.json` first; do not hard-code in CSS.

### 4.8 Security and privacy
- **Writes on an unauthenticated app.** Anyone who can reach the app can add pets to or change pets of any owner. Accepted by the intent ("no Spring Security and no CSRF"); concern 3.
- **CSRF:** no token, as in 009 and upstream; with no login there is no ambient authority to forge. If login is added, CSRF must be added in the same change. Snyk Code may flag a state-changing POST without a token; if so, record it as an accepted finding.
- **Injection:** all SQL is parameterised through `@Query` named parameters. No SQL is built from input. Output is escaped with `th:text` and `th:errors`; JSON is serialised by Jackson from view records. Tests put `<script>`, `&`, `"` and `'` into the pet name and check the redisplay, the details page and the JSON.
- **Mass assignment and IDOR:** only `PetForm` is bound (F14). The update has `AND OWNER_ID = :ownerId`, and the controller checks the pet is in the URL owner's set, so changing the owner segment cannot reach another owner's pet (F13). Pet and owner ids are sequential and guessable, which is already true of owner ids (008).
- **Input bounds:** name capped at 30 to match `VARCHAR(30)`, so a long name is a form error, not a 500. Type ids are checked against the loaded list so the foreign key cannot fail. Birth date is parsed strictly. There is no cap on pets per owner or total, so repeated posts grow H2 memory without limit (concern 3).
- **Errors:** 404 uses the standard response; nothing extra is revealed. Form values are not logged.
- No new dependency in this intent; the Snyk gate is unaffected beyond 009.

### 4.9 Tests and the shared in-memory database
As 009 (4.9): `@SpringBootTest` contexts and their H2 are cached and reused, and other tests assert 13 pets, so write tests are `@Transactional` (MockMvc and HtmlUnit-over-MockMvc run on the test thread, so the rollback works), with `@DirtiesContext` as the fallback. Identity counters do not roll back, so tests assert the new id is above 13, not that it is 14.

### 4.10 Out of scope
Deleting pets; visits and Add Visit; pet JSON writes (create, update); persistence across restarts; moving a pet to another owner; unique-name enforcement in the database; i18n; Spring Security, login, CSRF tokens; optimistic locking; a Cancel/back link on the form (concern 5 of 009 applies); a custom error page.

## 5. Acceptance and verification

### 5.1 Tests
- `PetRepositoryTests` (`@DataJdbcTest`, new): `insert` adds a pet for owner 11 and `findById` of the owner returns it; ids from the identity are above 13; `update` changes name, birth date and type of pet 1 and nothing else; after updating pet 8 (Max) the owner 6 pets still have ids 7 and 8; `update` with the wrong owner id returns 0 and changes nothing; a type id with no row is rejected by the foreign key (documents why the controller checks first). Existing owner tests still see 11 owners and 13 pets outside the test transaction.
- `OwnerControllerTests` or `PetControllerTests` (`@WebMvcTest`, mocked repositories):
  - `GET .../pets/new.html` is 200, view `owners/petForm`, blank `petForm`, six types in name order; `GET .../pets/{petId}/edit.html` is 200 pre-filled; unknown owner, unknown pet, and a pet of another owner are 404 on GET and POST; non-numeric ids are 400;
  - valid create redirects (303) to `/owners/{id}.html` with flash `New Pet has been Added` and calls `insert` once; valid edit redirects with `Pet details has been edited` and calls `update` once;
  - name blank, whitespace-only, 31 characters, and `Leo`, `leo`, ` LEO ` for George Franklin return 200 with the right message and no write; editing Leo as `Leo` and as `LEO` succeeds; renaming one of Jean Coleman's pets to the other (`Max` to `samantha`) is rejected; the same name on another owner is accepted;
  - birth date blank, `2999-01-01`, tomorrow (fixed `Clock`), `2024-02-30`, `07/09/2010`, `abc` fail with the right message; today and `1900-01-01` pass;
  - type empty, `0`, `7`, `abc` fail with "Type is required." (for `abc`, no 500);
  - extra `id`, `ownerId`, `owner`, `type` parameters are ignored; a request with owner A in the URL and a pet of owner B never writes;
  - values with surrounding spaces are saved trimmed; `<script>` in the name is escaped on redisplay.
- `PetFormPageHtmlUnitTests` (new; `@SpringBootTest` + `@AutoConfigureMockMvc`, HtmlUnit, JS off, `@Transactional`):
  - Blank form: title `New Pet · Spring PetClinic`, one `h2`, owner name shown, three labelled controls in order (Name input, Birth Date date input, Type select with prompt plus six options), one submit button "Add Pet", only Find Owners `aria-current="page"`.
  - Create flow: from George Franklin's page click "Add New Pet", enter `Rex`, a date, `dog`, submit; land on his details page showing both Leo and Rex, `New Pet has been Added` once, reload shows no message; `/owners/1` JSON lists Rex with type `dog`.
  - First pet for an owner with none (owner 11): the empty sentence is replaced by a table with the new pet.
  - Edit flow: click "Edit Pet" on Leo; form filled with `Leo`, `2010-09-07`, `cat` selected; change the type and name, submit; details show the new values and `Pet details has been edited`; other pets and ids unchanged.
  - Duplicate: adding `leo` to George Franklin redisplays with the duplicate message beside Name, input kept, `aria-invalid="true"` and an `aria-describedby` that points to an existing element.
  - Empty form submit shows three field messages and the summary (`role="alert"`); a single bad field shows exactly one.
  - Future date is rejected with the date message and the other values kept.
- `OwnerDetailsPageHtmlUnitTests` (updated): "Add New Pet" is present for owners with and without pets; each pet row has an "Edit Pet" link to the right URL whose full text includes the pet name; the table has four headers (Name, Birth Date, Type, Actions); there is still no Add Visit link; the 009 "Edit Owner" assertions stay.
- `OwnersIntegrationTests` (updated): JSON still shows 13 pets in total and shapes are unchanged outside write tests; a created pet appears in both `/owners/{id}` and `/owners` JSON (in a rolled-back test).
- Existing vets, homepage, actuator and context tests stay green unchanged. An axe-style check (or a manual one) covers the form and the new table column (5.2).

### 5.2 Manual check
`./mvnw spring-boot:run`; open George Franklin's page, click "Add New Pet", submit empty (three messages, summary announced), add `Leo` (duplicate message), add a valid pet (details page with banner, reload with none), click "Edit Pet", change and save. Add a pet to Helen Ostrander (owner 11) and check the empty state is replaced. Open `/owners/1/pets/2/edit.html` and `/owners/999/pets/new.html` (both 404). Check keyboard order and visible focus on the links, date input and select, the date picker in two browsers plus one without a picker (type the date), the form and table at 320px, and `/actuator/health` is `UP`. Restart and confirm the added pet is gone.

## 6. Policy conformance
| Policy | How met |
|--------|---------|
| Design system | Reuses FormField, Button, InlineMessage, DataTable; extends FormField with select and date, DataTable with an actions column; square corners, no shadows or cards; `spring-green-dark` links; Title Case markup uppercased by CSS; muted helper text; docs and previews in the same PR. |
| Accessibility | Visible labels tied by `for`/`id`; `required`/`aria-required`; `aria-invalid` and `aria-describedby` on failed fields; `role="alert"` summary and `role="status"` confirmation; error words not colour; native date and select keep their keyboard behaviour; Edit Pet links carry the pet name; `th scope="col"` including the Actions header; visible focus; 320px layout. |
| CLAUDE.md | YAML config untouched; actuator untouched; Spring Data JDBC; UPPER CASE table and column names in `@Query`; Java 25; Boot 4 test packages; tabs; no `application.properties`. |
| Security (Snyk, REVIEW.md) | Parameterised SQL; bound form record only; escaped output; inputs bounded; foreign key and ownership checks before write; no new dependency here; the unauthenticated write and missing CSRF are explicit, accepted concerns. |
| Workflow | Branch and PR; the plan is written only after this spec has full human review. Never pushed to `main`. |

## 7. Areas of concern
1. **Hard dependency on intent 009, which is not merged.** Almost every form convention (validation starter, `message` flash, stacked layout, error states, `pc-message`, link styling, `@Transactional` write tests, owner `RESTART WITH 12`) is specified by 009 but absent from `main`. This intent cannot be built until 009 lands, and this spec may need a pass for naming drift (`OwnerForm`, `ownerForm.html`, the CSS class names) once the 009 implementation is final. If 009 changes direction, this spec's section 2 and 4.7 must be revisited. The engineer must not re-implement 009's pieces here.
2. **URL shape vs upstream.** The intent asks to match upstream's URL shape "where practical". Upstream is `/owners/{id}/pets/new` and `/owners/{id}/pets/{petId}/edit`. The repo's settled rule is `*.html` for pages, and 009 follows it. The two goals conflict; chosen, as in 009: GET pages at `*.html`, writes `POST` to the bare resource paths (`/owners/{ownerId}/pets`, `/owners/{ownerId}/pets/{petId}`). Remaining cost: an error redisplay is served from a URL that is not a form page on GET.
3. **Unauthenticated writes, no CSRF, unbounded growth.** Any reachable client can add or edit any pet of any owner, forge a cross-site form post (no token), or fill memory by repeated posts. Accepted by the intent only while data is fictitious and the app is not exposed. Real data must not be loaded until authentication, CSRF and some rate or size limit exist. Snyk Code may flag the POST handlers (4.8).
4. **Write surface and the aggregate footgun.** `Owner` owns `pets`, so the obvious "save the owner" is wrong (renumbers pet ids). This spec adds a separate narrow `PetRepository` with insert and update queries. That is a second place that writes the `pets` table beside whatever 009 does for owners. An alternative is one shared write component, and a reviewer could argue for it; also, intent 008's "no write methods in any repository" and 009's "narrow write surface" statements are now relaxed for pets as well.
5. **Table actions column vs the three-column rule and the design system.** Adding "Actions" makes the pets table four columns (needs `.table-responsive`, so it can scroll sideways on a phone) and adds a header with no data. The alternatives (an action link in the Name cell, or no column) either mix navigation with data or hide the action. No approved visual reference exists for actions in a table; the design owner should confirm. Also, native `select` and `date` controls look different per browser and cannot be fully styled to the "square corners" rule on every platform; the control box is square, the open picker is the OS's.
6. **"Not in the future" depends on the server's date.** A visitor in a time zone ahead of the server may pick today's date and have it rejected as future; one behind sees a `max` hint a day ahead. A `Clock` bean makes tests deterministic, but there is no per-visitor time zone. Acceptable for a clinic in one zone; noted so nobody is surprised. The date also has no lower bound (a 1000-year-old pet is accepted); upstream has none.
7. **Message wording is upstream's, not house style.** "New Pet has been Added" and "Pet details has been edited" have odd capitalisation and grammar ("Pet details has been"). The intent fixes them, so the spec keeps them verbatim and 009's messages use a different style ("New Owner Created"). A content review could unify them; it needs the intent changed.
8. **Designer reference not yet approved.** [PetClinic Pet Forms.html](./PetClinic%20Pet%20Forms.html) shows the select field, date field and actions column built from the system, but the design owner has not reviewed it. Engineers still build `preview.html` states from it; the design owner reviews before merge, and the docs should reach the published design-system source to avoid drift (as with `DetailList`).
9. **Name uniqueness is checked, not enforced.** The check runs in the controller, then the insert. Two simultaneous submissions can both pass and create two same-name pets for one owner, and the database has no unique constraint (seed names are mixed case, and H2 has no simple case-insensitive unique index without changing column types). Acceptable for a single-user, in-memory demo; a schema change would be a separate intent. Name comparison is `equalsIgnoreCase`, so unusual Unicode case rules (e.g. Turkish dotless i) are not special-cased.
10. **Controller growth.** `OwnerController` would reach roughly twelve handlers. A separate `PetController` is suggested but is an engineering choice.
11. **Tests that assert "no actions" or "three columns" change.** They belong to 008 and 009 and are updated here (5.1); any other doc or test that assumes a read-only details page should be found by search. `intent/008` docs are historical and left as is.

## 8. Assumptions
1. Open questions: none were raised by the intent. Resolved items (no Spring Security or CSRF; case-insensitive duplicate check; native date picker) are followed as written.
2. Intent 009 ships first and its conventions (validation starter, flash `message`, stacked form, error state, link-as-action) are reused unchanged (concern 1).
3. URLs are `/owners/{ownerId}/pets/new.html` and `/owners/{ownerId}/pets/{petId}/edit.html`, the form `POST`s to `/owners/{ownerId}/pets` (create) and `/owners/{ownerId}/pets/{petId}` (update), as 009 does for owners, and the success redirect goes to `/owners/{ownerId}.html` (concern 2).
4. Success is a 303 redirect to the owner's details page with a flash `message` carrying the intent's exact texts.
5. Template is `owners/petForm.html`; the form is one record `PetForm(name, birthDate, typeId)`, all three bound as text.
6. The type select is ordered by name, with an empty prompt option "Select a type", and is rendered from `PetTypeRepository.findAll()`.
7. Names are trimmed; max length 30 (column size); the duplicate rule ignores case, compares against the owner's other pets only, and allows keeping a pet's own name.
8. `birthDate` is bound as text and parsed as ISO `yyyy-MM-dd` in the controller, so validation messages stay in our wording without a messages file. Today is allowed; there is no lower bound; the server's `Clock` defines today.
9. An unknown or non-numeric type id (hand-made POST) gets the same "Type is required." message as an empty one.
10. A pet that belongs to another owner is a plain 404, the same as a non-existent pet.
11. The form page shows the owner's name as plain context text (not a field), and the heading is "New Pet" or "Edit Pet".
12. Submit labels are "Add Pet" and "Update Pet" (as upstream). Link labels are "Add New Pet" and "Edit Pet".
13. The pets table gets a fourth column with the visible header "Actions" and `.table-responsive`; the "Add New Pet" link sits under the "Pets" heading in both the empty and non-empty states.
14. Writes go through a new narrow `PetRepository` using `@Modifying @Query`; the generated id is not returned; `pets` identity is restarted at 14.
15. Whether the pet handlers live in `OwnerController` or a new `PetController` is the engineer's choice.
16. The Add Visit action stays off the page; Find Owners, search, owner details, vets, homepage and `/actuator/health` are otherwise unchanged.
17. Added or edited pets live only for the life of the running app; data stays fictitious sample data.

## 9. Handoff notes for engineering
- Do not start until intent 009 is merged; rebase on it and check the names in section 2 against what 009 actually shipped. Record any mismatch in section 10.
- Branch from `main`; never push to `main`. Keep `intent/010-add-edit-pet/` in sync and record deviations in a section 10 added to this spec.
- Suggested order: `data.sql` identity reset and a repository test for it; `PetRepository` with insert and update and tests; `PetForm`, `Clock` bean and controller handlers with `@WebMvcTest`; `petForm.html`; details page links, Actions column and tests; design-system select, date, actions docs, CSS and previews; HtmlUnit tests; docs.
- Boot 4 test imports live in `org.springframework.boot.webmvc.test.autoconfigure.*`; check the generated imports.
- Writing tests are `@Transactional` and assert ids "above 13", never "equal to 14".
- Request design-owner review of the new previews (concern 8) before merge, and record the outcome in section 10. Do not add a back link or Cancel without a design-system update.
- Run `actionlint` only if workflows change (they should not).

## 10. Implementation notes and deviations
- **Concern 1 (dependency on 009):** resolved. 009 shipped before this work (PR #43) and the names match this spec: `OwnerForm`, `owners/ownerForm.html` and its `field(...)` fragment (reused here for Name), the flash `message`, `.pc-form-stacked`, `.pc-field-invalid`, `.pc-field-error`, `.pc-action`, `pc-message`. The design-system docs for the select, date and actions column landed in PR #42, so this PR only implements them in `petclinic.css` (select padding and chevron position, date height, `.pc-field-help`, `.pc-col-actions`, `.pc-section-title + .pc-action`); no token added.
- **Concern 4 (write surface):** as specified, a separate `PetRepository` with `@Modifying @Query` `insert` and `update`. `update` returns `boolean` (as `OwnerRepository.update` does) rather than `int`; `false` is a 404.
- **Concern 10 / assumption 15 (controller):** the pet handlers live in a new package-private `PetController`, with its own owner and pet lookups and `seeOther` helper; `OwnerController` is unchanged.
- **Form context:** the view gets `ownerName` (a string) rather than an owner object, so no entity reaches the form view.
- **Birth date parsing:** the value must match `\d{4}-\d{2}-\d{2}` and then parse with `ISO_LOCAL_DATE` (strict resolver). The shape check is needed because `ISO_LOCAL_DATE` alone accepts signed and five-digit years (`-0001-01-01`); `2024-02-30` and `2023-13-01` are rejected by the resolver.
- **Select prompt:** on a blank form Thymeleaf marks the empty "Select a type" option `selected`, which is the intended default.
- **Concern 8 (design review):** still pending. The previews in `docs/design-system/components/{FormField,DataTable,PetForm}` are the board for the design owner before merge.
- Spec 5.2's manual check, as far as it was run: health `UP`, a create gives 303 to a clean `/owners/1.html` and pet id 14, `leo` on owner 1 gets the duplicate message, `/owners/1/pets/2/edit.html` and `/owners/999/pets/new.html` are 404. The 320px layout, the date picker in different browsers and keyboard focus still need checking in a real browser.
