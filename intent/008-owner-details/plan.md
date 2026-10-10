# Plan: Intent 008 — Owner Details

## Context
Find Owners (intents 006 and 007) lists owners but gives no way to see one owner. Intent 008 adds a read-only details page at `/owners/{id}.html` and JSON at `/owners/{id}`, links each name on Find Owners to it, seeds a pet-less owner 11, and adds a `DetailList` component and a `section-title` type token to the design system. Visual reference: `intent/008-owner-details/PetClinic Owner Details.html`.

The spec predates 007's merge (section 2 says 007 has no code). 007 is now on `main` (PR #34), so this PR is the second to merge: it owns the name-link check on search results (spec 4.4, concern 6). That goes in spec section 10 as a deviation.

Branch: `feature/008-owner-details`. Land it through a PR and never push to `main`.

## 1. Seed data: `src/main/resources/data.sql`
Add owner 11 after owner 10: `(11, 'Helen', 'Ostrander', '4120 Monroe St.', 'Fitchburg', '6085554412')`. Add no pets for it, and leave `schema.sql` unchanged.

## 2. Repositories (package `owner`)
- `OwnerRepository`: add `Optional<Owner> findById(Integer id);`. Keep it read-only.
- New `PetTypeRepository extends Repository<PetType, Integer>` with `List<PetType> findAll();`, mirroring `vet/SpecialtyRepository.java`.

## 3. Views and controller
- New records:
  - `OwnerDetailView(Integer id, String firstName, String lastName, String address, String city, String telephone, List<PetDetailView> pets)`
  - `PetDetailView(Integer id, String name, LocalDate birthDate, String type)`
  - Each gets a one-line Javadoc, like `OwnerView`.
- `OwnerController`:
  - Inject `PetTypeRepository` alongside `OwnerRepository`.
  - `@GetMapping("/owners/{id}.html") String showOwner(@PathVariable Integer id, Model model)` puts `owner` in the model and returns `owners/ownerDetails`.
  - `@GetMapping(path = "/owners/{id}", produces = APPLICATION_JSON_VALUE) @ResponseBody OwnerDetailView showResourcesOwner(@PathVariable Integer id)`.
  - Private `ownerDetails(Integer id)`:
    - `findById`, or throw `ResponseStatusException(NOT_FOUND, "owner " + id + " does not exist")`.
    - Build a type map: `petTypes.findAll()` into `Map<Integer, String>`.
    - Map pets to `PetDetailView` with `pet.type().getId()`, sorted by name then id (a `Comparator<PetDetailView>` next to `BY_NAME`).
  - Check that `/owners/{id}.html` resolves to the `.html` mapping, not `/owners/{id}` with `id="1.html"`. Spring 7 has no suffix matching, so it should, and a test proves it. A non-numeric id gives a 400 from the type mismatch.

## 4. Templates
- New `templates/owners/ownerDetails.html`:
  - `layout('Owner Information', 'owners', ~{::content})`.
  - Then the `h2.pc-page-title` and a `dl.pc-detail` holding Name, Address, City and Telephone, each `dt`/`dd` pair wrapped in a `div` if the grid needs it.
  - Then `h3.pc-section-title` "Pets". If there are pets, a `table.table.table-striped.pc-table` follows, with no responsive wrapper. It has a hidden caption `|Pets of ${first} ${last}|` and `th scope="col"` headers Name, Birth Date and Type. Birth Date uses `#temporals.format(pet.birthDate, 'yyyy-MM-dd')`.
  - Otherwise `<p class="pc-muted pc-empty">This owner has no pets.</p>`.
  - Use `th:text` only.
- `templates/owners/ownersList.html`: the Name `td` wraps an `<a th:href="@{/owners/{id}.html(id=${owner.id})}" th:text="|…|">`. This one row serves both the list and search results.

## 5. Design system and CSS
- `docs/design-system/tokens.json`: add a type style `section-title`, 24px/30px/700 display, `ink`, used as an `h3` under the page title. Give it `space-6` above and `space-3` below, matching the reference.
- New `docs/design-system/components/DetailList/README.md` and `preview.html`, following the InlineMessage/Button format:
  - Grid with a 12rem label column from `md` (768px) up. Labels stack above values below that.
  - 2px `line-strong` rule on top, 1px `line` between rows.
  - `dt` uses the `table-head` style (UPPERCASE via CSS, `ink`) and `dd` uses `body`. `space-3` × `space-4` padding. When stacked, `dt` has no bottom padding or border and `dd` has `space-1` top padding.
  - Empty collection: one muted sentence, not an empty table.
- `docs/design-system/components/bundle.css`: `.pc-detail` and `.pc-section-title` rules in token vars.
- `docs/design-system/README.md`: "seven components" becomes eight, plus a `section-title` line in Type and a content rule for the empty-collection sentence (spec concern 2).
- `docs/design-system/components/DataTable/README.md`: add the pets example, the name-link rule (`spring-green-dark`, standard focus ring) and the empty-collection note.
- `design-system.json`: update `lastChange` only. No new assets, so the blob ids are unchanged.
- `static/resources/css/petclinic.css`: add a "Section title" section and a "Detail list" section ported to the `--pc-` vars:
  - `.pc-section-title`: margin 48px 0 12px.
  - `.pc-detail`: stacked by default, with a `@media (min-width: 768px)` two-column grid.
  - `.pc-empty`: margin 0, padding 12px 0.
  - Name links pick up `--bs-link-color` and `a:focus-visible` from the existing rules, so they need no new link CSS.
  - No new colours.

## 6. Tests (tabs, Boot 4 test packages, AssertJ, `MockMvcTester`)
- `OwnerRepositoryTests`:
  - Update the existing tests: total 11, page index 2 = [11].
  - Add `findById` tests: 1 is George Franklin with Leo, 11 has no pets, 999 is empty, 6 has Max and Samantha.
  - New `PetTypeRepositoryTests` (`@DataJdbcTest`) checks the six types.
- `OwnerControllerTests` (add `@MockitoBean PetTypeRepository`):
  - `/owners/1.html` returns 200, `text/html` and view `owners/ownerDetails`.
  - 404 for 999 on both paths; `abc` is 400 on both.
  - JSON has `birthDate` `"2010-09-07"` and `type` `"cat"`, also with `Accept: text/html` and `*/*`.
  - A pet-less owner shows the message and no `<table`.
  - `<script>` and `&` in the name and address are escaped.
  - `/owners` JSON pets stay `{id, name}`.
  - The name cell links to `/owners/1.html`.
- New `OwnerDetailsPageHtmlUnitTests` (real seed, JS off):
  - Title, one `h2`, the `dt`/`dd` values and the Leo row.
  - Order: Coleman's Max then Samantha, Rodriquez's Jewel then Rosy.
  - Owner 11 shows the empty message and no table.
  - Only Find Owners is `aria-current`.
  - No edit, add or visit links or buttons.
- `OwnersPageHtmlUnitTests`:
  - Name cells are links and other cells have no anchors. Clicking George Franklin opens his details.
  - There are 3 pages, and page 3 shows Helen Ostrander with `none`.
  - The "no Next on last page" check moves to page 3.
  - A search for `Davis` gives rows whose names are links (the 007 crossover).
- `OwnersIntegrationTests`:
  - `/owners` returns 11 owners and 13 pets.
  - `/owners/6` has two pets.
  - `/owners/999.html` returns 404.

## 7. Spec and plan bookkeeping
- Add section 10 "Deviations" to `intent/008-owner-details/spec.md`:
  - 007 merged first, so this PR re-verifies the link on search results.
  - Any CSS or naming differences, such as `.pc-empty`.
  - The design-review outcome for `DetailList` (pending at PR time).
- Save this plan as `intent/008-owner-details/plan.md`.

## Verification
1. `sdk env`, then `./mvnw clean verify` passes.
2. `spring-tools:validate` reports no diagnostics.
3. `./mvnw spring-boot:run`, then in the browser:
   - `/owners.html`: click George Franklin and check the details, Leo's row, the Find Owners nav state, tab order and focus outlines.
   - At 320px the list stacks and nothing scrolls horizontally.
   - `?page=3`, then Helen Ostrander: the empty state.
   - Search `Davis`, then a name link.
   - Compare with the visual reference.
4. `curl -i` checks: `/owners/1` is JSON, `/owners/999.html` is 404, `/owners/abc` is 400, `/actuator/health` is UP.
5. Push the branch and open the PR. Check that `build.yml`, `snyk.yml` and the Claude review pass. Ask the design owner to review `DetailList/preview.html` (concern 8).
6. Ask before updating the published design-system artifact (`PzhUJ3xDPAVpZPiwVrbZpT`) with `DetailList` and `section-title`, because that is outward-facing.
