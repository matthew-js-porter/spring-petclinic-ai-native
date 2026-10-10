# Plan: Intent 006 — Find Owners

## Context
Intent 006 adds a read-only owners list, the first step toward upstream PetClinic's "Find Owners" area. `intent/006-find-owners/spec.md` gives the requirements (F1–F11). `PetClinic Owners.html` is the visual reference. The work copies the intent 004 vets pattern: Spring Data JDBC records, a read-only `Repository`, view records, a `.html` page plus JSON on separate paths, and the shared layout and pagination fragments.

State of `main`: intent 005 has merged, so `layout.html` already has Home and then Veterinarians, and `WelcomeController` exists. Find Owners goes **between Home and Veterinarians** (spec 4.5, first case).

Work happens on branch `feature/006-find-owners` and lands by PR. Never push to `main`.

## 1. Schema and seed (`src/main/resources/`)
- `schema.sql`: append `types`, `owners`, `pets` and their indexes, exactly as in spec 4.1.
- `data.sql`: append 6 types, 10 owners and 13 pets with explicit ids, using the values in spec 4.1.
- Fetch upstream `spring-petclinic` `src/main/resources/db/h2/data.sql` (WebFetch) and diff it against the spec's values (concern 8). Record the result, "no differences" included, in spec section 10.
- No `application.yaml` change.

## 2. Java: package `org.springframework.samples.petclinic.owner` (tabs; mirror `vet/`)
- `PetType`: `@Table("TYPES") record (@Id Integer id, String name)`. There is no `PetTypeRepository` unless a test needs one (spec assumption 12). The repository test checks the 6 types with a `JdbcClient`/`JdbcTemplate` count, so no repository is added.
- `Pet`: `@Table("PETS") record (@Id Integer id, String name, LocalDate birthDate, @Column("TYPE_ID") AggregateReference<PetType, Integer> type)`.
- `Owner`: `@Table("OWNERS") record (@Id Integer id, String firstName, String lastName, String address, String city, String telephone, @MappedCollection(idColumn = "OWNER_ID") Set<Pet> pets)`.
- `OwnerRepository extends Repository<Owner, Integer>`: `Page<Owner> findAll(Pageable)` and `List<Owner> findAll(Sort)`, with the same Javadoc note as `VetRepository`.
- `PetView(Integer id, String name)`, `OwnerView(id, firstName, lastName, address, city, telephone, List<PetView> pets)`, and `Owners(List<OwnerView> ownerList)` (JSON wrapper, like `vet/Vets.java`).
- `OwnerController`: package-private `@Controller`, `PAGE_SIZE = 5`, `BY_ID`. The page validation is copied from `VetController.showVetList` (`vet/VetController.java`):
  - `GET /owners.html` puts `listOwners` (`Page<OwnerView>`) in the model and returns `owners/ownersList`.
  - `GET /owners` (`produces = APPLICATION_JSON_VALUE`, `@ResponseBody`) returns `Owners`.
  - A private static `toView(Owner)` sorts pets by name and then id, so the two Luckys have a stable order. No lookup map is needed: pets live inside the aggregate.

## 3. Templates
- `templates/owners/ownersList.html`, built like `vets/vetList.html`:
  - `layout('Owners', 'owners', ~{::content})` and `h2.pc-page-title` "Owners".
  - A `<div class="table-responsive pc-table-region" tabindex="0" role="region" aria-label="Owners table">` wraps `table#owners.table.table-striped.pc-table`.
  - The caption is `Owners and their pets, page N of M`. The headers are Name, Address, City, Telephone, Pets, each `th scope="col"`, with no `pc-col-name` (spec 4.3).
  - The Telephone `td` has `text-nowrap`.
  - The Pets cell shows `none`/`pc-muted` when empty. Otherwise it is `#strings.listJoin(owner.pets.![name], ' ')`.
  - The pagination fragment follows, with `(${listOwners}, '/owners.html', 'Owners pages')`. Only `th:text` is used.
- `fragments/layout.html`: add the Find Owners `<li>` between Home and Veterinarians. It uses the inline Bootstrap Icons `search` SVG (16×16, `currentColor`, `aria-hidden`, `focusable="false"`), `th:href="@{/owners.html}"` and `th:aria-current` on `activeMenu == 'owners'`.
- `petclinic.css`: add a `.pc-table-region:focus-visible` outline only if the existing focus rule doesn't already cover the region. It reuses the existing 2px `spring-green-dark` focus values, so no new token is needed. Check this against the reference's `.table-region`.

## 4. Design-system docs (same PR)
- `docs/design-system/README.md`, Iconography: add the `search` glyph for Find Owners.
- `components/Navbar/README.md` and `preview.html`: add the Find Owners item (current on `/owners.html`) and the order Home, Find Owners, Veterinarians.
- `components/DataTable/README.md`: add a wide-table note. Tables with more than three columns get the `.table-responsive` focusable, labelled region. List Owners as an example and mention `text-nowrap` for telephone numbers.

## 5. Tests (`src/test/java/.../owner/`; Boot 4 imports as in the vet tests)
- `OwnerRepositoryTests` (`@DataJdbcTest`), modelled on `VetRepositoryTests`. It reuses that class's `StatementCounting` proxy approach by copying the nested config, since it's package-private in `vet`.
  - The seed has 10 owners in id order, 13 pets in total and 6 types (counted by SQL).
  - Specific pets: George Franklin has Leo, Eduardo Rodriquez has {Jewel, Rosy}, and Carlos Estaban has {Lucky, Sly}. The per-owner pet counts are 1,1,2,1,1,2,1,1,1,2.
  - Paging: page 0 returns 5 of 10 with pets populated, and page 1 returns owners 6–10.
  - Query bound: `<= 2 + PAGE_SIZE`. **This differs from the spec.** Spec 4.2 says "one extra query for the page", but Spring Data JDBC loads `@MappedCollection` children per aggregate, as the vets test documents. Record this in spec section 10 instead of turning on single-query loading, which isn't applied to paged `findAll`.
- `OwnerControllerTests` (`@WebMvcTest(OwnerController.class)`, `MockMvcTester`, `@MockitoBean OwnerRepository`):
  - `/owners.html` returns 200 with view `owners/ownersList` and `text/html`.
  - `/owners` returns JSON with `ownerList[0]` fields `address`, `city`, `telephone` and `pets[{id,name}]`, and no `birthDate` or `type`.
  - `page=0`, `-1` and `abc` return 400. A page past the last returns 404.
  - An owner with no pets renders `none` in `pc-muted`.
  - A `<script>` in a name is escaped.
  - `Accept: text/html` on `/owners` returns 406, as for vets.
- `OwnersPageHtmlUnitTests`: copy the `VetsPageHtmlUnitTests` setup (JS off). It checks:
  - The title, one `h2` "Owners", and the header order.
  - Page 1 rows. George Franklin's row is fully checked, and Jean Coleman shows `Max Samantha`.
  - No `<a>` in any `tbody` cell.
  - Clicking "2" shows owners 6–10, and page 2 has no Next link.
  - Nav text order is Home, Find Owners, Veterinarians.
  - Only Find Owners is current on `/owners.html`. Find Owners is not current on `/vets.html`.
  - The table region has `tabindex=0`, `role=region` and `aria-label`.
- `OwnersIntegrationTests` (`@SpringBootTest` + `@AutoConfigureMockMvc`): `/owners` returns 10 owners and 13 pets in total.
- Check whether the existing vets and welcome HtmlUnit tests assert the exact nav item list. If they do, update them to include Find Owners. That is an F8 nav change, not a behaviour change, and gets noted in section 10. Everything else stays untouched (F11).

## 6. Spec sync
- Add `## 10. Deviations` to `intent/006-find-owners/spec.md` with:
  - the upstream seed diff result;
  - the query bound of 2 + page size;
  - any `petclinic.css` focus rule added;
  - any nav assertions changed in existing tests.
- Save this plan as `intent/006-find-owners/plan.md` (ExitPlanMode hook).

## Verification
1. Run `sdk env && ./mvnw clean verify`. All tests must pass.
2. Run `spring-tools:validate` and expect no diagnostics.
3. Run `./mvnw spring-boot:run` (or the `run` skill) and check:
   - `/owners.html` and `?page=2` in Chrome, compared against `PetClinic Owners.html`.
   - Tab through the page: the nav focus rings and the table region outline should be visible.
   - At 320px, the toggler works and only the table region scrolls.
4. Check the endpoints from the command line:
   - `curl -i localhost:8080/owners` returns JSON with 10 owners.
   - `/owners.html?page=0` returns 400 and `?page=3` returns 404.
   - `/vets.html`, `/vets`, `/` and `/actuator/health` are unchanged.
5. Push the branch, open the PR, and confirm `build.yml` and `snyk.yml` pass.
