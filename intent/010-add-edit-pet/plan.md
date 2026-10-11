# Plan: Intent 010, Add or Edit a Pet

## Context
Spec `intent/010-add-edit-pet/spec.md` adds the first writes to `pets`. Owner Information gets an "Add New Pet" link that opens a blank form (`GET /owners/{ownerId}/pets/new.html` → `POST /owners/{ownerId}/pets`). Each pet row gets an "Edit Pet" link that opens the same form pre-filled (`GET /owners/{ownerId}/pets/{petId}/edit.html` → `POST /owners/{ownerId}/pets/{petId}`). A valid save sends a 303 to `/owners/{ownerId}.html` with a one-time flash confirmation. Invalid input redisplays the form with a summary and one message per failed field.

**What is already on `main`** (the spec's concern 1 is now resolved):
- Intent 009 is merged (PR #43): `OwnerForm`, the `@InitBinder` trimming, `form(...)` and `seeOther(...)` in `OwnerController`, `owners/ownerForm.html` with its `field(...)` fragment, the flash `message` banner in `ownerDetails.html`, `.pc-form-stacked`, `.pc-field-invalid`, `.pc-field-error`, `.pc-action`, and the owners `RESTART WITH 12`.
- The design system for 010 is merged (PR #42). The FormField select and date variants, `.pc-field-help`, the DataTable actions column, and the PetForm pattern are all in `docs/design-system/`, and `bundle.css` lines 89–105 hold their CSS. **`petclinic.css` does not implement them yet.** No doc changes are needed unless the build drifts from the docs.

Branch: `feature/010-add-edit-pet` from `main`. Never push to `main`. No comment in code, SQL or CSS mentions an intent.

## Steps

### 1. Identity reset (`src/main/resources/data.sql`)
After the pet inserts, add a comment in the style of the owners one, then `ALTER TABLE pets ALTER COLUMN id RESTART WITH 14;`. Types are not advanced.

### 2. `PetRepository` (new, `owner` package)
- `interface PetRepository extends Repository<Pet, Integer>`, with no `save` or `delete`.
- `@Modifying @Query("INSERT INTO PETS (NAME, BIRTH_DATE, TYPE_ID, OWNER_ID) VALUES (:name, :birthDate, :typeId, :ownerId)") void insert(Integer ownerId, String name, LocalDate birthDate, Integer typeId)`
- `@Modifying @Query("UPDATE PETS SET NAME = :name, BIRTH_DATE = :birthDate, TYPE_ID = :typeId WHERE ID = :id AND OWNER_ID = :ownerId") boolean update(Integer ownerId, Integer id, String name, LocalDate birthDate, Integer typeId)`. It returns a boolean, matching `OwnerRepository.update`; `false` is treated as 404.
- Class Javadoc: `Pet` has no owner field, and saving the `Owner` aggregate renumbers its pets, so writes are explicit queries.

### 3. `Clock` bean
`PetClinicApplication` gets `@Bean Clock clock()`, which returns `Clock.systemDefaultZone()`. Tests replace it with `@TestBean` and a `Clock.fixed(...)`.

### 4. `PetForm` record (new)
- `PetForm(String name, String birthDate, String typeId)`.
- Annotations: `@NotBlank(message = "Name is required.")` and `@Size(max = 30, message = "Name must be 30 characters or fewer.")` on `name`; `@NotBlank(message = "Birth Date is required.")` on `birthDate`; `@NotBlank(message = "Type is required.")` on `typeId`.
- `static PetForm empty()`.
- `static PetForm from(Pet pet)`: the birth date as ISO text and `typeId` as `String.valueOf(pet.type().getId())`.

### 5. `PetController` (new, package-private, `owner` package)
The spec suggests a separate controller (assumption 15). `OwnerController` already has eight handlers.
- Constructor takes `OwnerRepository`, `PetTypeRepository`, `PetRepository` and `Clock`.
- `@InitBinder("petForm")` registers `StringTrimmerEditor(false)`.
- Its own `findOwner(ownerId)` and `findPet(owner, petId)`, both throwing `ResponseStatusException(NOT_FOUND, …)`. A pet of another owner is not in `owner.pets()`, so it gets the same 404. They run first on every GET and POST.
- The four handlers follow the 009 shapes: `ModelAndView` return, `@Valid @ModelAttribute("petForm")`, `RedirectAttributes`.
- `checkContext(form, result, owner, petId, types)` adds `rejectValue` errors only where the field has none yet (required beats format beats rule):
  - **Birth date:** a `\d{4}-\d{2}-\d{2}` shape check, then `LocalDate.parse(text, ISO_LOCAL_DATE)` (strict, so `2024-02-30` fails; spec section 10), giving "Birth Date must be a date in the form yyyy-mm-dd." Then `isAfter(LocalDate.now(clock))` gives "Birth Date cannot be in the future."
  - **Type:** `Integer.parseInt` inside a try block, then a check that the id is in the loaded ids. Any failure gives "Type is required."
  - **Name:** `equalsIgnoreCase` against `owner.pets()` whose id is not `petId`, giving "This owner already has a pet with this name."
- On a valid form, `insert` or `update` runs and the flash `message` is set to "New Pet has been Added" or "Pet details has been edited". The handler then returns a 303 `RedirectView` to `/owners/{ownerId}.html` (copy `seeOther`).
- The `form(model, owner, petId)` helper sets `ownerName` (a plain string, so no entity reaches the view), `petTypes` (sorted by name), `heading`, `submitLabel`, `action` and `today` (ISO).

### 6. Template `owners/petForm.html` (new)
It follows `ownerForm.html`:
- Title, owner context line `<p class="pc-muted">Owner: …</p>`, the summary "Pet not saved: N field(s) need attention." (counted with `#fields.detailedErrors`), and the stacked form with "All fields are required.".
- **Name:** a text field with `maxlength="30"` and `autocomplete="off"`. Reuse `~{owners/ownerForm :: field(...)}` if its `autocomplete` argument works as-is; otherwise write it inline.
- **Birth Date:** `type="date"`, `th:max="${today}"`, `autocomplete="off"`, and a `<p id="birthDate-help" class="pc-field-help">yyyy-mm-dd</p>` helper. `aria-describedby` is `birthDate-help`, or `birthDate-help birthDate-error` when the field is invalid.
- **Type:** `<select th:field="*{typeId}" class="form-select pc-input">` with a "Select a type" prompt, then `th:each` over `petTypes` (value is the id as text, label is the name). It has the same `aria-*` attributes as the other fields.
- Button with `${submitLabel}`. Markup follows `intent/010-add-edit-pet/PetClinic Pet Forms.html`.

### 7. Owner Information (`ownerDetails.html`)
- Directly after the Pets `h3`: `<a class="pc-action" th:href="@{/owners/{id}/pets/new.html(id=${owner.id})}">Add New Pet</a>`.
- The table goes inside `<div class="table-responsive pc-table-region" tabindex="0" role="region" aria-label="Pets table">`, the same wrapper as `ownersList.html`.
- A fourth header, `<th scope="col">Actions</th>`. Each row gets `<td class="pc-col-actions"><a th:href="@{/owners/{o}/pets/{p}/edit.html(...)}">Edit Pet<span class="visually-hidden" th:text="| ${pet.name}|"></span></a></td>`.

### 8. CSS (`static/resources/css/petclinic.css`)
Port `bundle.css` lines 89–105 into the file's expanded style, using `--pc-*` variables and plain px as the file does:
- `select.pc-input` (chevron, padding) and `.pc-field-invalid select.pc-input`
- `input[type="date"].pc-input` min-height
- `.pc-field-help`
- `.pc-col-actions` nowrap
- `.pc-table a` colour and focus, if missing
- `.pc-section-title + .pc-action { margin-bottom: 12px; }`

There is no new token.

### 9. Tests
All write tests are `@Transactional` and assert pet ids are above 13.
- **`PetRepositoryTests`** (`@DataJdbcTest`, new): covers spec 5.1. Insert for owner 11 shows up through `OwnerRepository.findById`, with an id above 13. Update of pet 1 changes only its three fields. Owner 6's pets keep ids 7 and 8. A wrong owner id returns `false` and changes nothing. An unknown type id throws `DataIntegrityViolationException`.
- **`PetControllerTests`** (`@WebMvcTest(PetController.class)`, `MockMvcTester`, `@MockitoBean` repos, `@TestBean` fixed `Clock`): every bullet in spec 5.1. That covers GET new and edit, 404s (unknown owner, unknown pet, another owner's pet, on GET and POST), 400 for non-numeric ids, both 303 + flash flows with `then(pets).should().insert/update`, and the name, date and type validation tables as `@ParameterizedTest`. It also checks that extra `id`/`ownerId`/`owner`/`type` params are ignored, trimming, and `<script>` escaping on redisplay.
- **`PetFormPageHtmlUnitTests`** (new; `@SpringBootTest @AutoConfigureMockMvc @Transactional`, JS off): the blank form, the create flow from George Franklin (banner once, then gone on reload, then `/owners/1` JSON has Rex/dog), a first pet for owner 11, the edit flow on Leo, the `leo` duplicate (`aria-invalid` and `aria-describedby` pointing at real ids), an empty submit (three messages plus `role="alert"`), a future date, and only Find Owners as `aria-current`.
- **`OwnerDetailsPageHtmlUnitTests`** (update):
  - `listsThePetsWithBirthDateAndType` expects four headers.
  - The `pets(...)` helper expects a fourth cell, "Edit Pet Leo".
  - `offersEditOwnerAsTheOnlyAction` becomes "offers Edit Owner, Add New Pet and Edit Pet only". It checks the hrefs, that there is still no `main button, main form`, and that there is no "Add Visit".
  - `explainsWhenTheOwnerHasNoPets` also expects "Add New Pet".
- **`OwnersIntegrationTests`** (update): a rolled-back test that creates a pet and sees it in `/owners/{id}` and `/owners` JSON. The existing 13-pet assertion stays.

### 10. Docs
- Add a section 10 "Implementation notes and deviations" to `spec.md`. It records:
  - that 009 has shipped and the names match;
  - `PetController` as the controller;
  - `update` returning `boolean`;
  - the `ownerName` model attribute;
  - the strict date parsing;
  - that design-owner review is still pending (concern 8).
- Save this plan as `intent/010-add-edit-pet/plan.md`.

## Verification
- `./mvnw clean verify`, all green.
- `spring-tools:validate` to check for diagnostics.
- `./mvnw spring-boot:run` and the manual check in spec 5.2:
  - add, duplicate, empty submit, edit, and a first pet for owner 11;
  - `/owners/1/pets/2/edit.html` and `/owners/999/pets/new.html` both return 404;
  - keyboard focus on the links, date input and select;
  - 320px width;
  - `/actuator/health` is `UP`;
  - after a restart the seed is back.
- Commit and push the branch, then open a PR that asks for design-owner review of the previews.
