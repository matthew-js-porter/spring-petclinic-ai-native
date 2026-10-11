# Plan: Visits (intent 011)

## Context
Spec `intent/011-visits/spec.md` adds visits: a stored record per pet, listed on Owner Information and created from a new New Visit form. The design-system half (spec §4.6) already landed on `main` in PR #52 (`0c57bc1`): VisitList, VisitForm, the DetailList context block, `pc-subsection-title`, and the two-link actions cell are documented in `docs/design-system/` and `components/bundle.css`. What remains is the app: schema/seed, repository, views/JSON, controller, templates, `petclinic.css`, tests. Save this plan as `intent/011-visits/plan.md` once approved.

Work on branch `claude/visits-spec-5b4e44` (rename to e.g. `feature/011-visits` if preferred), PR to `main`. Tabs, UPPER CASE `@Table`/`@Column`, no intent mentions in code.

## Steps

### 1. Schema and seed
- `src/main/resources/schema.sql`: `visits` table + `visits_pet_id` index exactly as spec §4.2.
- `src/main/resources/data.sql`: after the pets restart, 4 inserts (ids 1–4: pet 7 2013-01-01 "rabies shot", pet 8 2013-01-02 "rabies shot", pet 8 2013-01-03 "neutered", pet 7 2013-01-04 "spayed" — upstream order), then `ALTER TABLE visits ALTER COLUMN id RESTART WITH 5;` with the same comment style as owners/pets. (Pets 7/8 confirmed as Samantha/Max of owner 6.)

### 2. Persistence (package `owner`)
- `Visit.java`: `@Table("VISITS") record Visit(@Id Integer id, @Column("PET_ID") Integer petId, @Column("VISIT_DATE") LocalDate date, String description)`.
- `VisitRepository.java extends Repository<Visit, Integer>` (Javadoc modelled on `PetRepository`: visits sit outside the Owner aggregate so Owner/Pet stay unchanged; insert-only, no save/delete):
  - `@Query("SELECT * FROM VISITS WHERE PET_ID IN (:petIds)") List<Visit> findByPetIds(Collection<Integer> petIds)`
  - `@Modifying @Query("INSERT INTO VISITS (PET_ID, VISIT_DATE, DESCRIPTION) VALUES (:petId, :date, :description)") void insert(Integer petId, LocalDate date, String description)`
  - Callers must not pass an empty collection (owner with no pets → skip the query; `IN ()` is invalid SQL).

### 3. Shared date parsing
- New package-private `IsoDates` (or `Dates`) in `owner` with `static Optional<LocalDate> parse(String)`: move `BIRTH_DATE_SHAPE` + strict `ISO_LOCAL_DATE` logic and its comment out of `PetController.parseBirthDate`. `PetController` calls it; birth-date behaviour unchanged (PetControllerTests prove it).

### 4. Views and JSON
- `VisitView.java`: `record VisitView(Integer id, LocalDate date, String description)` with a static `ORDER` comparator (date desc, then id desc) and `from(Visit)`.
- `PetDetailView`: add `List<VisitView> visits` as last component; update Javadoc.
- `OwnerController`: inject `VisitRepository`; in `ownerDetails`, one `findByPetIds(petIds)` call (skipped if no pets), `groupingBy(Visit::petId)`, map each pet's list sorted by `VisitView.ORDER`, empty list when none. `/owners` list JSON (`OwnerView`/`PetView`) untouched.

### 5. Form and controller
- `VisitForm.java` record `(date, description)` with the spec §4.3 constraints and messages; `static VisitForm forDate(LocalDate today)` → `(today.toString(), "")`. Javadoc as `PetForm` (only these fields bound, so ids come from the URL).
- `VisitController.java` (package-private `@Controller`), deps `OwnerRepository`, `VisitRepository`, `PetTypeRepository` (for the type name in the context block), `Clock`:
  - `@InitBinder("visitForm")` `StringTrimmerEditor(false)`.
  - `GET /owners/{ownerId}/pets/{petId}/visits/new.html` → find owner (404), find pet among `owner.pets()` (404), model `visitForm = forDate(LocalDate.now(clock))`, return `form(...)`.
  - `POST /owners/{ownerId}/pets/{petId}/visits` → owner, pet, then `@Valid` result; if `date` has no field error and `IsoDates.parse` is empty, `rejectValue("date", "format", "Date must be a date in the form yyyy-mm-dd.")`; on errors redisplay; else `visits.insert(petId, date, description)`, flash `message` = "Your visit has been booked", 303 to `/owners/{ownerId}.html`.
  - `form(model, owner, pet)` adds `ownerName`, `petName`, `petType`, `visits` (that pet's `VisitView`s ordered), `action` = `/owners/{o}/pets/{p}/visits`; returns `owners/visitForm`.
  - `findOwner`/`findPet`/`seeOther` mirror `PetController` (same 404 messages). Note: `@Valid` binding runs before the 404 checks in the method body, but no write happens before them, matching PetController — acceptable.
  - No logging of form values.

### 6. Templates
- `templates/owners/visitList.html`: fragment `visits(petName, list)` — empty → `<p class="pc-muted pc-empty">This pet has no visits.</p>`; else `table.table.table-striped.pc-table.pc-visits` with hidden caption `Visits of {petName}`, `th scope="col"` Date/Description, `td.pc-col-date` formatted `yyyy-MM-dd`. (Fragment takes the pet name for the caption, per VisitList README "consumer provides the pet's name".)
- `templates/owners/visitForm.html`: layout `('New Visit', 'owners', …)`; `h2.pc-page-title` "New Visit"; `dl.pc-detail.pc-context` Pet / Type / Owner; summary `p.pc-message role="alert"` ("Visit not saved: 1 field needs attention." / "N fields need attention.", as petForm); `form.pc-form.pc-form-stacked novalidate method=post` with "All fields are required.", Date field (copy petForm's date markup with `date-help`/`date-error`, no `max`), Description via `owners/ownerForm :: field('description', 'Description', 'text', 255, 'off')`, button "Add Visit"; then `h3.pc-section-title` "Previous Visits" + fragment.
- `templates/owners/ownerDetails.html`: second link in `.pc-col-actions` → `Add Visit<span class="visually-hidden"> {name}</span>` to `/owners/{o}/pets/{p}/visits/new.html`; after the pets region (only when pets exist) `h3.pc-section-title` "Visits", then per pet `h4.pc-subsection-title` + fragment.

### 7. CSS
- `static/resources/css/petclinic.css`: port the bundle.css additions: `.pc-col-actions a + a` margin, `.pc-subsection-title` (+ `.pc-section-title + .pc-subsection-title`), `.pc-visits .pc-col-date`, `.pc-visits td:last-child`, `.pc-detail.pc-context`. Use the existing CSS custom properties in petclinic.css (check names: it may use `--pc-*` rather than `--space-*`). No new tokens needed (subsection-title is already in tokens.json).

### 8. Tests (spec §5.1)
- New `VisitRepositoryTests` (`@DataJdbcTest`, model on `PetRepositoryTests`): seed 4; `findByPetIds(List.of(7, 8))` returns 4, pet 1 empty; `insert` → new row id > 4; insert with unknown pet id throws `DataIntegrityViolationException`.
- New `VisitControllerTests` (`@WebMvcTest(VisitController.class)`, mocked repos + fixed `Clock` as in `PetControllerTests`): all cases listed in spec §5.1.
- New `VisitFormPageHtmlUnitTests` (model on `PetFormPageHtmlUnitTests`, `@Transactional`): cases in §5.1 including the full flow from Jean Coleman's page.
- Update `OwnerDetailsPageHtmlUnitTests`: replace the `doesNotContain("Visit")` at line 112 with Add Visit link assertions (text incl. pet name, href), Visits section order, empty sentence.
- Update `OwnerControllerTests`: `@MockitoBean VisitRepository`; JSON at ~line 400 gains `"visits":[]`; add a case with visits ordered.
- Update `OwnersIntegrationTests` (lines ~62, 109, 117): add `visits` arrays to pets (Samantha/Max 2 each), assert `/owners` list unchanged.
- XSS: description `<script>&"'` posted and checked escaped in redisplay, details page and JSON.

### 9. Docs
- Add `## 10. Deviations` to `spec.md` only if something differs (e.g. fragment signature `visits(petName, list)` vs spec's `visits(list)`; `PetTypeRepository` added to VisitController deps). Save plan as `intent/011-visits/plan.md`.
- Design-system docs need no change unless implementation reveals a gap; VisitForm/VisitList READMEs note design-owner review is still pending before merge — call it out in the PR description.

## Critical files
`schema.sql`, `data.sql`, `owner/{Visit,VisitRepository,VisitView,VisitForm,VisitController,IsoDates}.java` (new), `owner/{PetDetailView,OwnerController,PetController}.java`, `templates/owners/{visitList,visitForm,ownerDetails}.html`, `petclinic.css`, tests listed above.

## Verification
- `sdk env && ./mvnw clean verify` — all green.
- `spring-tools:validate` for diagnostics.
- `./mvnw spring-boot:run`, then in the browser pane: `/owners/6.html` (visits in order, Add Visit links), Add Visit for Max → submit empty (2 messages + summary), bad date, valid → banner once, new row first; reload no banner; `/owners/6` JSON shows visits; `/owners/1/pets/7/visits/new.html` and `/owners/999/pets/1/visits/new.html` 404; resize to 320px for both pages; `curl -i localhost:8080/actuator/health` UP.
- Open PR, body ending with the Claude Code line, flagging the pending design-owner review (spec concern 5).
