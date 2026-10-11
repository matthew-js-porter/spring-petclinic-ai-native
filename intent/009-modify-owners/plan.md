# Plan: Intent 009, Modify Owners (Add Owner / Edit Owner)

## Context
Spec `intent/009-modify-owners/spec.md` adds the app's first runtime writes: an "Add Owner" link on Find Owners leading to a blank form (`GET /owners/new.html` → `POST /owners`), and an "Edit Owner" link on the details page leading to a pre-filled form (`GET /owners/{id}/edit.html` → `POST /owners/{id}`). Valid saves redirect with 303 to `/owners/{id}.html`, which shows a flash confirmation once. Invalid input redisplays the form with a summary and a message on each failed field.

Decisions already made:
- Concern 1: URLs follow option A.
- Concern 2: use `spring-boot-starter-validation`.
- Concern 3 (just decided): the writes go on `OwnerRepository`, as the spec's default says.
- Concern 4: settled by the merged design-system PR #40 (`.pc-action` links, the `.pc-field-invalid` state, InlineMessage for confirmations). The docs and `bundle.css` already hold the form CSS. `petclinic.css` does not have it yet.

Branch: `feature/009-modify-owners` from `main`. Never push to `main`.

## Steps

### 1. Dependency and identity reset
- `pom.xml`: add `spring-boot-starter-validation` (version managed by Boot).
- `src/main/resources/data.sql`: after the owner inserts, add a comment and `ALTER TABLE owners ALTER COLUMN id RESTART WITH 12;`.

### 2. `OwnerForm` record (new, `owner` package)
- Five `String` components.
- Each has `@NotBlank(message = "First Name is required.")` and `@Size(max = 30, message = "First Name must be 30 characters or fewer.")`. Last Name max is 30, Address 255, City 80.
- Telephone has `@NotBlank` plus `@Pattern(regexp = "(\\d{10})?", message = "Telephone must be exactly 10 digits.")`. The optional group lets an empty value pass the pattern, so an empty telephone shows only the "required" message (spec 4.5). The pattern does not use `UNICODE_CHARACTER_CLASS`, so `\d` matches ASCII digits only.
- Add `static OwnerForm empty()` and `static OwnerForm from(Owner)`.

### 3. `OwnerRepository` write methods
- `Owner save(Owner owner)`. Javadoc: call it only with `id == null` and empty `pets`. Saving an existing aggregate deletes and re-inserts its pets, which renumbers them.
- `@Modifying @Query("UPDATE OWNERS SET FIRST_NAME = :firstName, LAST_NAME = :lastName, ADDRESS = :address, CITY = :city, TELEPHONE = :telephone WHERE ID = :id") boolean update(Integer id, String firstName, String lastName, String address, String city, String telephone)`. It never touches `PETS`.
- Rewrite the class Javadoc. The interface is now "reads plus this narrow write surface", not read-only.

### 4. `OwnerController` (`src/main/java/.../owner/OwnerController.java`)
- `@InitBinder("ownerForm")` registers `StringTrimmerEditor(false)`.
- `GET /owners/new.html` renders the form with `OwnerForm.empty()`.
- `GET /owners/{id}/edit.html` renders the form with `OwnerForm.from(findOwner(id))`.
- `POST /owners`:
  - Takes `@Valid @ModelAttribute("ownerForm") OwnerForm form, BindingResult result, RedirectAttributes flash`.
  - On errors, return the form view.
  - Otherwise call `owners.save(new Owner(null, ..., Set.of()))`, add the flash attribute `message` = "New Owner Created", and 303 to `/owners/{id}.html`.
- `POST /owners/{id}`:
  - Call `findOwner(id)` first, so an unknown id is 404 before anything is written. A non-numeric id is already 400 through the `Integer` path variable binding.
  - Then validate, call `owners.update(...)`, and flash "Owner Values Updated".
- Shared private helpers:
  - `findOwner(id)`: pull the `findById(...).orElseThrow(404)` out of `ownerDetails` and reuse it.
  - `form(model, ownerId)`: sets `heading`, `submitLabel`, `action` and `ownerId`, and returns `"owners/ownerForm"`.
  - `seeOther(id)`: returns a `RedirectView` set to `HttpStatus.SEE_OTHER`. Handlers return `ModelAndView` so they can return either the view or the redirect.
- `showOwner` is unchanged; flash attributes reach the model on their own.
- Nothing logs form values.

### 5. Templates
- New `templates/owners/ownerForm.html`:
  - Uses `layout(${heading}, 'owners', …)`.
  - Markup follows spec 4.3 and `docs/design-system/components/OwnerForm/preview.html`: summary `p.pc-message[role=alert]`, then a `novalidate` form with classes `pc-form pc-form-stacked`, helper line `p.pc-form-help`, five `.pc-field` blocks, and a `btn pc-button` submit.
  - Summary text counts the fields with errors (`#lists.size(#fields.detailedErrors())`, at most one per field) and pluralises: "Owner not saved: 1 field needs attention." / "Owner not saved: N fields need attention."
  - Inputs carry `autocomplete`, `maxlength`, `required`, and `aria-required`. On error they also get `aria-invalid` and `aria-describedby` pointing at `{field}-error`.
  - All output goes through `th:text` or `th:errors`.
- `ownerDetails.html`: add `<p th:if="${message}" class="pc-message" role="status" th:text="${message}">` under the `h2`. Add `<p><a class="pc-action" th:href="@{/owners/{id}/edit.html(id=${owner.id})}">Edit Owner</a></p>` under the `dl`.
- `ownersList.html`: add `<a class="pc-action" th:href="@{/owners/new.html}">Add Owner</a>` under the `h2`, above the search form.

### 6. CSS
Port the intent-009 block from `docs/design-system/components/bundle.css` into `static/resources/css/petclinic.css`:
- `.pc-form-stacked` and `.pc-form-help`
- `.pc-field-invalid .pc-input` and `.pc-field-error`
- `.pc-action`

Write it in the same style as the file (literal px values, `--pc-*` variables). In `.pc-form-stacked`, override the `sm` rules for `.pc-form` (row) and `.pc-field` (fixed width). Update the InlineMessage comment ("answer or result of the last action").

No new tokens: the design-system docs are already done. Check that `design-system.json` and README need no further edits.

### 7. Tests
Every test that writes is `@Transactional`. New ids are asserted as `> 11`.
- `OwnerRepositoryTests`:
  - `save` of a new owner returns an id above 11 and the owner has no pets.
  - `update` changes the five columns.
  - Updating owner 6 keeps her pets with ids 7 and 8, named Samantha and Max.
  - `update` on 999 returns false and changes nothing.
- `OwnerControllerTests` (`@WebMvcTest`): every case in spec 5.1. That includes the routing of `new.html`, 404 and 400 on GET and POST with `verify(owners, never()).save/update`, the telephone edge cases (including Arabic-Indic digits), trimming, ignored `id` and `pets` parameters, the 303 status and flash attributes, and the JSON endpoints still working.
- New `OwnerFormPageHtmlUnitTests` (`@SpringBootTest @AutoConfigureMockMvc @Transactional`, JS off): blank form, create flow plus reload plus search, edit flow on George Franklin (Leo kept), empty submit (five errors, summary, aria wiring, kept values), a single bad telephone, `<script>` escaping, no `style` attributes or inline scripts, and nav `aria-current`.
- `OwnerDetailsPageHtmlUnitTests`: replace `offersNoEditAddOrVisitActions`. Assert an Edit Owner link exists, and that there is still no `main button`, no `main form`, and no pet or visit actions.
- `OwnersPageHtmlUnitTests`: assert the Add Owner link on page 1, page 3, search results and no-results.
- `OwnersIntegrationTests`: a created owner appears in `/owners` and `/owners/{id}` JSON (`@Transactional` on that test).

### 8. Docs
- Add a spec section 10, "Implementation notes and deviations". It covers:
  - concern 3 decided as writes on the repository
  - the 008 conflicts superseded
  - the telephone pattern trick
  - the design-owner review outcome (still pending; preview states exist in PR #40)
- Save this plan as `intent/009-modify-owners/plan.md`.
- Grep `CLAUDE.md` for "read-only" (none expected).

## Critical files
- `pom.xml`, `src/main/resources/data.sql`
- `src/main/java/org/springframework/samples/petclinic/owner/{OwnerController,OwnerRepository,OwnerForm}.java`
- `src/main/resources/templates/owners/{ownerForm,ownerDetails,ownersList}.html`
- `src/main/resources/static/resources/css/petclinic.css`
- Tests under `src/test/java/org/springframework/samples/petclinic/owner/`
- `intent/009-modify-owners/spec.md`, `plan.md`

## Verification
- `sdk env`, then `./mvnw clean verify` (F1). Run `spring-tools:validate` to check for diagnostics.
- Manual check (spec 5.2): `./mvnw spring-boot:run`.
  - Add Owner: submit empty, then a bad telephone only, then valid data. Reload and confirm the message is gone. Search for the new owner.
  - Edit George Franklin and confirm Leo is still listed.
  - `/owners/999/edit.html` returns 404.
  - Check at 320px and with JavaScript off.
  - Restart and confirm the new owner is gone.
  - `/actuator/health` is UP.
- Commit on the branch and open a PR. The Snyk gate must pass with the new dependency. Ask the design owner to review the preview states (concern 8).
