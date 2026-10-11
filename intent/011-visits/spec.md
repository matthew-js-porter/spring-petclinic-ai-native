# Spec: Visits
Source intent: [intent.md](./intent.md)
Author: matthew-js-porter

## 1. Summary
Add visits: a stored record that belongs to a pet. The owner details page lists each pet's visits and offers "Add Visit" for each pet; a new form page records a visit.

- `visits` table with four upstream seed rows (Samantha and Max, both Jean Coleman's). Every other pet has none.
- `/owners/{id}.html` shows, for each pet, its visits (date, description), most recent first, or an empty-state sentence. The pet row's Actions cell gains an "Add Visit" link beside "Edit Pet".
- `GET /owners/{ownerId}/pets/{petId}/visits/new.html` shows the form: read-only context (pet and owner), fields Date (native date input, defaults to today, any date allowed) and Description, then the pet's previous visits. Plain HTML `POST` to `/owners/{ownerId}/pets/{petId}/visits`, no JavaScript.
- A valid save redirects (303) to the owner's details page, which shows "Your visit has been booked" once.
- Invalid input redisplays the form with the typed values and a message next to each failed field: description required, date required and a real date.
- Unknown owner, unknown pet, or a pet of another owner is 404 on GET and POST.
- Owner JSON gains a `visits` array on each pet; existing fields keep their shape.
- The design system gains a per-pet visit list pattern and a read-only context block above a form; two links in a table's actions cell need a design decision (concern 5).
- Out of scope: editing, deleting or cancelling visits; vets, visit types, times or calendars on visits; JSON writes; persistence across restarts; i18n; login and CSRF.

## 2. Existing codebase context
Checked against `main` at commit `da2f8da`.

- Spring Data JDBC on in-memory H2. `schema.sql` then `data.sql` run on every start with explicit ids. `data.sql` already restarts the `owners` identity at 12 and `pets` at 14 (intents 009, 010). `visits` is the next table that needs this.
- `Owner` is a record with `@MappedCollection(idColumn = "OWNER_ID") Set<Pet> pets`. `Pet` is a record `(id, name, birthDate, type)` with no owner field. Saving an existing `Owner` aggregate deletes and re-inserts its pets, which renumbers them. Owner and pet writes therefore use `@Modifying @Query` statements (`OwnerRepository.update`, `PetRepository.insert/update`).
- `OwnerController.ownerDetails(id)` builds `OwnerDetailView` with `PetDetailView(id, name, birthDate, type)` sorted by name then id; it serves both `/owners/{id}.html` and the JSON at `/owners/{id}`. Unknown ids are `ResponseStatusException(NOT_FOUND, ...)`.
- `PetController` (package-private, `owner` package) holds the pet form handlers and the pattern to reuse: dedicated form record with text fields, `@InitBinder` with `StringTrimmerEditor`, `checkContext` adding messages via `rejectValue`, a `Clock` bean, `seeOther` (303 to `/owners/{id}.html`), flash attribute `message`, `findOwner`/`findPet` giving 404, and the `owners/ownerForm :: field(...)` Thymeleaf fragment.
- `ownerDetails.html` renders the `message` banner (`pc-message`, `role="status"`), a `dl`, "Edit Owner", a "Pets" `h3`, "Add New Pet", and either the empty sentence or a four-column table (Name, Birth Date, Type, Actions) in a `table-responsive pc-table-region`. The Actions cell holds one link, "Edit Pet" with a visually hidden pet name.
- Design system: `docs/design-system/` has DataTable (including an actions-column rule that says one link per cell and "no Add Visit"), DetailList, FormField, Button, InlineMessage, OwnerForm and PetForm patterns. There is no nested record list per pet and no read-only context block above a form (PetForm uses a muted "Owner: ..." line).
- Tests this touches: `OwnerDetailsPageHtmlUnitTests` asserts the page text does not contain "Visit" (line 112); tests asserting JSON shapes of `/owners/{id}`; tests asserting seed counts (11 owners, 13 pets).
- Intent 012 (custom error page) is open at the same time. It changes how 404/400 render, not whether they occur; this spec's 404 requirements stay about status codes.

## 3. Requirements

### Functional
| ID | Requirement |
|----|-------------|
| F1 | `./mvnw clean verify` succeeds with all tests passing. |
| F2 | Seed data contains exactly four visits: pet 7 Samantha 2013-01-01 "rabies shot" and 2013-01-04 "spayed"; pet 8 Max 2013-01-02 "rabies shot" and 2013-01-03 "neutered". Existing owners, pets, types, vets are unchanged. Other pets have no visits. |
| F3 | `/owners/{id}.html` shows, for every pet, a visit list with columns Date and Description, most recent date first (ties: higher id first). Samantha shows 2013-01-04 then 2013-01-01; Max shows 2013-01-03 then 2013-01-02. |
| F4 | A pet with no visits shows the muted sentence "This pet has no visits." and no table. |
| F5 | Each pet row has an "Add Visit" link to `/owners/{ownerId}/pets/{petId}/visits/new.html`, in the same Actions cell as "Edit Pet", after it. Its accessible name includes the pet's name (visually hidden span, as Edit Pet). |
| F6 | `GET /owners/{ownerId}/pets/{petId}/visits/new.html` returns 200 `text/html` in the shared layout: `h2` "New Visit", page title `New Visit · Spring PetClinic`, a context block naming the pet and the owner, the form, then a "Previous Visits" section listing the pet's visits (most recent first, or the empty sentence). Navbar marks Find Owners only. |
| F7 | The form has Date (`<input type="date">`, pre-filled with today, `required`) then Description (single-line text, `maxlength=255`, `required`), and a submit button "Add Visit". Each has a visible `label` tied by `for`/`id`. |
| F8 | `POST /owners/{ownerId}/pets/{petId}/visits` with valid input inserts one visit for that pet and redirects (303) to `/owners/{ownerId}.html`, which shows "Your visit has been booked" once and the new visit in the pet's list. The new visit id is above 4. |
| F9 | Validation after trimming: **Description** required, at most 255 characters. **Date** required, in `yyyy-MM-dd`, a real calendar date (`2024-02-30`, `2023-13-01`, `abc` fail). Any valid date is accepted: past, today, future. |
| F10 | Invalid input returns 200 and redisplays the form (same view, served from the POST URL) with typed values kept, no row written, one message next to each failed field, and one summary message above the form. Several failures show together. The Previous Visits list is still shown. |
| F11 | The confirmation shows once; reloading or revisiting the details page shows none. |
| F12 | Unknown owner id, unknown pet id, or a pet that belongs to a different owner (e.g. `/owners/1/pets/7/visits/new.html`) is 404 on GET and POST, with no row written. A non-numeric id is 400. |
| F13 | The request cannot set a visit id, pet id or owner id. Ids come only from the URL. |
| F14 | Owner JSON (`/owners/{id}`) includes `visits` on each pet: an array of `{id, date, description}` ordered as F3, empty array when none. All existing fields keep name, type and position. `/owners` (list) JSON is unchanged. |
| F15 | Find Owners, search, add/edit owner, add/edit pet, vets, homepage and `/actuator/health` behave as before. |
| F16 | Accessibility: labelled fields; `aria-invalid` and `aria-describedby` on failed fields; `role="alert"` summary and `role="status"` confirmation; each visit table has a hidden caption and `th scope="col"`; every "Add Visit" link names its pet; visible 2px focus outlines. |
| F17 | Works with JavaScript disabled and at 320px: form stacks, visit tables do not force the page to scroll horizontally, navbar collapses via the existing toggle. |
| F18 | Nothing persists across restart: after a restart the data is exactly the seed (4 visits). |

### Non-functional and constraints
- Thymeleaf in the shared layout; plain form posts; no new JavaScript.
- Spring Data JDBC and H2 with plain SQL scripts; no JPA. YAML config unchanged; actuator unchanged; no new Maven dependency.
- All output through `th:text`/`th:errors`, never `th:utext`. Only a dedicated form record is bound.
- Design-system rules apply; any new component goes into `docs/design-system/` in the same PR (4.6). Tabs for Java, Boot 4 test imports, UPPER CASE `@Table`/`@Column`/SQL names. No mention of intents in code or comments.
- No Spring Security and no CSRF protection (intent decision; concern 3).

## 4. Design

### 4.1 URLs and flow
| Method and path | Purpose |
|---|---|
| `GET /owners/{ownerId}/pets/{petId}/visits/new.html` | blank form, date = today |
| `POST /owners/{ownerId}/pets/{petId}/visits` | create; 303 to `/owners/{ownerId}.html` or redisplay |

Upstream uses `/owners/{ownerId}/pets/{petId}/visits/new` for both GET and POST and redirects to `/owners/{ownerId}`. The repo rule (set in 009 and 010) is `*.html` for GET pages and the bare resource path for writes, with the bare owner path being JSON, so this spec deviates: GET at `.../visits/new.html`, POST to `.../visits`, redirect to `/owners/{ownerId}.html` (concern 2). The path `.../visits` has no GET mapping, so a browser GET of it is a 405 as for other write paths.

### 4.2 Data
`schema.sql`:
```sql
CREATE TABLE IF NOT EXISTS visits (
  id          INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  pet_id      INTEGER NOT NULL,
  visit_date  DATE NOT NULL,
  description VARCHAR(255) NOT NULL,
  FOREIGN KEY (pet_id) REFERENCES pets (id)
);
CREATE INDEX IF NOT EXISTS visits_pet_id ON visits (pet_id);
```
`data.sql` (after the pets and their identity restart): four `INSERT`s with explicit ids 1 to 4 and the F2 values, then `ALTER TABLE visits ALTER COLUMN id RESTART WITH 5;` with a comment (explicit ids leave the counter at 1). Seed pet ids 7 and 8 are Samantha and Max; verify against `data.sql` when implementing.

Column name `visit_date` (as upstream) avoids the SQL keyword `date`.

### 4.3 Java (package `org.springframework.samples.petclinic.owner`)
- **`Visit`** record `@Table("VISITS")`: `@Id Integer id`, `@Column("PET_ID") Integer petId` (or `AggregateReference<Pet,Integer>`), `@Column("VISIT_DATE") LocalDate date`, `String description`. Read-only mapping used by the repository.
- **`VisitRepository extends Repository<Visit, Integer>`**, narrow like `PetRepository`:
  - `@Query("SELECT * FROM VISITS WHERE PET_ID IN (:petIds)") List<Visit> findByPetIds(Collection<Integer> petIds)` (one query per page, not one per pet).
  - `@Modifying @Query("INSERT INTO VISITS (PET_ID, VISIT_DATE, DESCRIPTION) VALUES (:petId, :date, :description)") void insert(Integer petId, LocalDate date, String description)`.
  - No `save`, no `delete`. Javadoc says why.
- **Visits are not mapped onto `Pet`/`Owner`.** The alternative, `@MappedCollection Set<Visit> visits` on `Pet`, makes the `Owner` aggregate three levels deep, changes the `Pet` constructor (used by `PetForm.from` and tests), and makes any aggregate re-save of an owner delete pets that visits reference. A separate repository keeps `Owner` and `Pet` untouched (assumption 3, concern 4).
- **`VisitView`** record `(Integer id, LocalDate date, String description)`. `PetDetailView` gains `List<VisitView> visits` as its last component (F14). `OwnerController.ownerDetails` loads the owner's visits with one `findByPetIds` call, groups by pet, sorts by date then id descending.
- **`VisitForm`** record: `date` (`String`, ISO text, default `LocalDate.now(clock)` for the blank form) and `description` (`String`). `@NotBlank(message = "Date is required.")`, `@NotBlank(message = "Description is required.")`, `@Size(max = 255, message = "Description must be 255 characters or fewer.")`. Text for the date, as in `PetForm`, so malformed values keep our wording and survive redisplay.
- **`VisitController`** (package-private, new): reuses `OwnerRepository`, `VisitRepository` and the existing `Clock` bean. Handlers per 4.1. Order on each: find owner (404), find pet within owner's `pets()` (404, the same message for a pet of another owner), then bind/validate, then write. Date check reuses `PetController`'s rule (shape `\d{4}-\d{2}-\d{2}` then strict `ISO_LOCAL_DATE`); extract it to a small shared package-private helper rather than copying it. Model attributes: `visitForm`, `ownerName`, `petName`, `petType` (name), `visits` (previous, as `VisitView`s), `action`. Flash `message` = "Your visit has been booked". `@InitBinder("visitForm")` trims, as `PetController`.
- **Logging:** no form values are logged.

### 4.4 Templates
- New `owners/visitForm.html`, `layout('New Visit', 'owners', ~{::content})`: `h2` "New Visit", context block (4.6), summary `pc-message role="alert"` when errors ("Visit not saved: N fields need attention."), `form.pc-form.pc-form-stacked` with `novalidate`, the Date field (date variant from PetForm, but no `max`), the Description field via `owners/ownerForm :: field('description', 'Description', 'text', 255, 'off')`, and the button. Then `h3` "Previous Visits" and the shared visit list fragment.
- New fragment `owners/visitList.html :: visits(list)` used by both pages: the empty sentence or a `pc-table` with hidden caption, `th scope="col"` Date, Description; date cell `text-nowrap`, formatted `yyyy-MM-dd`.
- `ownerDetails.html`: Actions cell gains `<a>Add Visit<span class="visually-hidden"> Leo</span></a>` after Edit Pet. A new section "Visits" follows the pets table: one `h4` per pet (pet name) with its visit list fragment. This keeps tables flat (no table nested in a table cell, which is poorly announced by screen readers and does not fit 320px) and works for any number of visits (assumption 5).
- Messages: Date: `Date is required.`, `Date must be a date in the form yyyy-mm-dd.`; Description: `Description is required.`, `Description must be 255 characters or fewer.`. One message per field: required beats format.

### 4.5 Security and privacy
- Unauthenticated writes: anyone who can reach the app can add visits to any pet. Accepted by the intent while data is fictitious (concern 3). Visit descriptions are free text about a patient animal and, in a real clinic, linkable to an owner's personal data; not logged.
- No CSRF token, as in 009 and 010 (concern 3). Snyk Code may flag the POST; record as accepted if so.
- Injection: SQL via named `@Query` parameters only. Output escaped (`th:text`, `th:errors`, Jackson for JSON). Tests post `<script>`, `&`, `"`, `'` in the description and check the redisplay, details page and JSON.
- Mass assignment/IDOR: only `VisitForm` is bound; pet ownership is checked against the URL owner's pets before the insert, so changing the owner segment cannot reach another owner's pet. The foreign key is the backstop. Ids are sequential and guessable, as already true for owners and pets.
- Bounds: description capped at 255 to match the column, so a long value is a form error, not a 500. Date shape-checked (4-digit year) so it stays inside what H2 `DATE` and `LocalDate` handle. No cap on visits per pet: repeated posts grow memory without limit (concern 3).
- No new dependency.

### 4.6 Design system and CSS
No new colour, radius, shadow or font. Changes go in `docs/design-system/` (READMEs, `preview.html`, `design-system.json`, `bundle.css` as in 009/010) and `petclinic.css`:
- **VisitList** (new component, or a section in DataTable): a per-pet record list: `h4` section title (uses the existing type scale; add a token only if none fits), then a two-column DataTable (Date, Description) or the muted empty sentence "This pet has no visits." Shared by Owner Information and the Add Visit page. States: with rows, empty, 320px.
- **Context block** (extension to DetailList or PetForm): read-only labelled values above a form (Pet, Type, Owner) as a compact `dl.pc-detail`, not inputs, so it is not mistaken for editable fields. Replaces PetForm's one-off "Owner: ..." line only if the design owner agrees; otherwise PetForm stays as is.
- **FormField**: Date variant reused; note the visit form's date has a default value and no `max`.
- **DataTable**: update the actions-column rule from "one link per cell, no Add Visit" to allow Edit Pet then Add Visit; define spacing between two links (`space-3` gap, no separator character) and keep the cell `text-nowrap` or allow wrap at 320px (concern 5).
- **Button/InlineMessage**: add "Add Visit" and "Your visit has been booked" as examples. Links-as-actions: "Add Visit" is a `spring-green-dark` link, not a button.
- Previews: details page with visits, a pet without, the form blank, one error, several errors, 320px.

### 4.7 Tests and the shared in-memory database
Write tests are `@Transactional` (MockMvc and HtmlUnit-over-MockMvc run on the test thread), `@DirtiesContext` as fallback; identity counters do not roll back, so assert the new visit id is above 4, never equal to 5. Tests that count pets (13) or owners (11) are unaffected; tests that assert exact JSON of a pet must add `visits`.

## 5. Acceptance and verification

### 5.1 Tests
- `VisitRepositoryTests` (`@DataJdbcTest`, new): seed has 4 visits; `findByPetIds(7, 8)` returns them, empty for pet 1; `insert` adds one with id above 4; a pet id with no row is rejected by the foreign key.
- `VisitControllerTests` (`@WebMvcTest`, mocked repositories): GET 200 with view `owners/visitForm`, form date = fixed-clock today, previous visits in order; unknown owner, unknown pet, pet of another owner are 404 on GET and POST; non-numeric ids 400; valid POST redirects 303 to `/owners/{id}.html` with flash "Your visit has been booked" and calls `insert` once; description blank, whitespace-only and 256 characters fail without a write; date blank, `abc`, `2024-02-30`, `07/09/2010` fail; yesterday, today, tomorrow, `2999-01-01` pass; extra `id`, `petId`, `ownerId` parameters are ignored; spaces trimmed; `<script>` escaped.
- `VisitFormPageHtmlUnitTests` (`@SpringBootTest` + `@AutoConfigureMockMvc`, HtmlUnit, JS off, `@Transactional`): title; one `h2`; context shows Samantha and Jean Coleman; Date then Description in order, labelled; button "Add Visit"; only Find Owners current; Previous Visits lists Samantha's two visits newest first; empty-pet page shows "This pet has no visits."; submit empty shows two field messages plus summary with `aria-invalid`/`aria-describedby` pointing at existing elements; invalid date keeps description; full flow: Jean Coleman's page, click "Add Visit" for Max, submit, land on details with the banner once and the new visit first, reload shows no banner, `/owners/6` JSON lists it.
- `OwnerDetailsPageHtmlUnitTests` (updated): the "no Visit" assertion is replaced; each pet row has Add Visit with the pet name in its text and the right URL; seeded visits appear in order; empty sentence for a pet without; table headers still Name, Birth Date, Type, Actions.
- `OwnersIntegrationTests` (updated): `/owners/6` JSON shows `visits` for Samantha and Max with 2 each, `[]` for others; existing fields unchanged; `/owners` list JSON unchanged.
- Existing vets, homepage, actuator, context tests stay green. An axe-style or manual accessibility check covers both pages.

### 5.2 Manual check
`./mvnw spring-boot:run`; open Jean Coleman's page: visits in order; click "Add Visit" for Max, submit empty (two messages, summary announced), enter a bad date, then a valid visit (banner, new row first, reload shows no banner). Add one for a pet without visits (empty sentence replaced). Open `/owners/1/pets/7/visits/new.html` and `/owners/999/pets/1/visits/new.html` (404). Check keyboard order, visible focus, the date picker in two browsers, both pages at 320px, `/actuator/health` is `UP`, and a restart returns to the seed.

## 6. Policy conformance
| Policy | How met |
|--------|---------|
| Design system | Reuses DataTable, FormField, Button, InlineMessage, DetailList; adds a documented VisitList pattern and context block; square corners, no shadows or cards; `spring-green-dark` links; Title Case markup, uppercase by CSS; docs and previews in the same PR. Two actions in one cell needs design sign-off (concern 5). |
| Accessibility | See F16: labels, error association, alert/status roles, captions, scoped headers, pet-named links, focus, 320px. |
| CLAUDE.md | YAML untouched; actuator untouched; Spring Data JDBC; UPPER CASE names; Java 25; Boot 4 test packages; no intent mentions in code or comments; plain SQL scripts. |
| Security (Snyk, REVIEW.md) | Parameterised SQL; bound form record only; escaped output; bounded inputs; ownership check before write; no new dependency; unauthenticated writes and missing CSRF recorded as accepted concerns. |
| Workflow | Branch and PR; plan only after human review of this spec; never pushed to `main`. |

## 7. Areas of concern
1. **Cannot satisfy "match upstream URLs" and "repo `.html` convention" together.** See 2.
2. **URL shape deviates from upstream.** Upstream: `/owners/{id}/pets/{petId}/visits/new` for GET and POST. Here: GET `.../visits/new.html`, POST `.../visits`, redirect to `/owners/{id}.html`. Cost: an error redisplay is served from the POST URL, which is not a form page on GET.
3. **Unauthenticated writes, no CSRF, unbounded growth.** Anyone reachable can add visits to any pet, forge a cross-site post, or fill memory. Accepted by the intent only for fictitious data. Real data must not be loaded until authentication, CSRF and a size or rate limit exist.
4. **Visits sit outside the `Owner` aggregate.** Reading them needs a second query and a separate write repository, so there are now three write surfaces (owners, pets, visits). The benefit is that the owner/pet aggregate and its constructors are untouched. If anyone later deletes or re-saves pets through the aggregate, the foreign key will fail loudly rather than lose visits silently. A reviewer may prefer mapping visits onto `Pet`; Spring Data JDBC's nested collection support would need checking first.
5. **Two links in one Actions cell and a new nested-list pattern are not in the design system.** DataTable currently forbids more than one link per actions cell and says Add Visit must not appear; the intent asks for Add Visit beside Edit Pet. This spec updates that rule, but the design owner must approve; at 320px the cell may need to wrap. The per-pet "Visits" sections below the pets table repeat each pet's name rather than nesting a table in the row; upstream nests, which would be unreadable on a phone. No approved visual reference exists for either; the design owner should confirm before merge.
6. **"Today" is the server's date.** The default date uses the server clock; a visitor in another time zone may see yesterday or tomorrow. Acceptable for a single-zone clinic; any date is allowed so nothing is rejected because of it.
7. **No bounds on dates.** A visit in year 0001 or 9999 is accepted (intent: any date). A visit before the pet's birth date is also accepted; upstream does the same. Not validated against the pet's birth date because the intent says any date.
8. **Message wording is upstream's.** "Your visit has been booked" differs in style from "New Pet has been Added" and "New Owner Created". Kept as the intent fixes it.
9. **Page length.** Owner details with many pets and visits grows long; there is no pagination or collapsing. Fine for the seed data; revisit if visits grow.
10. **Test and doc churn.** The "no Visit" assertion and the DataTable "no Add Visit" wording are intentionally reversed; any other text assuming a read-only details page should be found by search. Historical `intent/008` to `010` docs stay as they are.
11. **Concurrent intent 012 (error page).** Both touch ownership/404 behaviour in tests; whichever lands second must re-run the other's 404 assertions.

## 8. Assumptions
1. The intent has no open questions; the resolved items (seed four visits; any date, default today) are followed as written.
2. URLs are `/owners/{ownerId}/pets/{petId}/visits/new.html` (GET) and `/owners/{ownerId}/pets/{petId}/visits` (POST), redirecting to `/owners/{ownerId}.html`.
3. Visits are read through a separate `VisitRepository` and not mapped onto `Pet`/`Owner`; `Pet` and its constructor stay unchanged.
4. Description is single-line text, trimmed, required, maximum 255 (the column size). Upstream only requires it to be non-empty.
5. The details page shows visits in a "Visits" section below the pets table (one `h4` and one small table per pet), not nested in the pet row.
6. The form page shows previous visits below the form (so the form is first on a phone), under "Previous Visits".
7. Page and heading text: `h2` "New Visit", submit "Add Visit" (upstream), labels "Date" and "Description", link "Add Visit". Empty sentence "This pet has no visits."
8. Visit dates are bound as text and parsed strictly as `yyyy-MM-dd` with a four-digit year, like birth dates; messages are defined in code.
9. A pet of another owner is a plain 404, the same as a missing pet.
10. JSON visit objects are `{id, date, description}`, `visits` is always present (possibly empty) and placed last in each pet; `/owners` list JSON is unchanged.
11. Ordering: date descending, then id descending.
12. Table `visit_date` column name follows upstream; the Java field is `date`.
13. The `Clock` bean added in intent 010 supplies "today" for the default date and tests.
14. Added visits live only for the running app; data stays fictitious.

## 9. Handoff notes for engineering
- Branch from `main`; never push to `main`. Keep this folder in sync and record deviations in a section 10 added to this spec.
- Suggested order: schema, seed and identity restart with `VisitRepositoryTests`; `VisitView`, `PetDetailView.visits` and `OwnerController` grouping with JSON tests; visit list fragment and details page; `VisitForm`, `VisitController` with `@WebMvcTest`; `visitForm.html`; design-system docs, CSS, previews; HtmlUnit tests; docs.
- Confirm the shared date-parsing helper against `PetController` before extracting; do not change birth-date behaviour.
- Do not add Cancel or back links without a design-system update. Request design-owner review of the previews (concern 5) before merge.
- The full spec needs human review before a plan is written.
