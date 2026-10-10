# Plan: Intent 004 — Vets

## Context
Intent 004 adds the first PetClinic feature: a read-only list of vets and their specialties. It has an HTML page (`/vets.html`, paginated 5 per page) and a JSON endpoint (`/vets`). `intent/004-vets/spec.md` defines the requirements. Two newer artifacts came after the spec and set the look:
- `docs/design-system/` holds the tokens (`tokens.json`), the rules (`README.md`), and the Navbar, DataTable, Pagination and Footer components (`components/*/README.md`, `components/bundle.css`).
- `intent/004-vets/PetClinic Vets.html` is a bundled two-page mock of the target page. Unpacked, it shows the exact markup: a brown header with a 4px green top bar, a 1140px container, an h2 "Veterinarians", a striped table, the text pagination `Pages: [ 1 2 ] « ‹ › »` and a one-line footer.

This plan implements the spec. Where the spec and the design differ, it follows the design and records the deviation in the spec. Two decisions were confirmed with the user:
- **Nav:** keep the spec: the brand link plus one **Veterinarians** item, and `/` redirects 302 to `/vets.html`. The mock's "Home" item is left out.
- **Fonts:** self-host the Montserrat (600/700) and Varela Round latin woff2 files. No Google Fonts request.

Today the repo has only `PetClinicApplication` and `contextLoads()`. Work happens on a branch (`feature/004-vets`) and lands by PR. Never push to `main`.

## 1. Dependencies (`pom.xml`)
- Add `spring-boot-starter-thymeleaf`, `org.webjars:bootstrap` (`${webjars-bootstrap.version}`, the latest 5.3.x, as a property next to `java.version`), `org.webjars:webjars-locator-lite` (BOM-managed) and `spring-boot-starter-thymeleaf-test` (test scope).
- Confirm the Boot 4.1 artifact names against start.spring.io (`spring-tools:create-spring-boot-project` into the scratchpad, then compare its pom).
- Add no validation starter. Page-range checks are explicit in the controller.
- Add no Bootstrap Icons WebJar. Only one icon (`list-ul`) is needed, so its MIT SVG path goes inline in the layout.

## 2. Schema and seed (`src/main/resources/`)
- `schema.sql`: `vets`, `specialties`, `vet_specialties` with the indexes, copied exactly from spec 4.2.
- `data.sql`: explicit ids. Vets 1–6 (Carter, Leary, Douglas, Ortega, Stevens, Jenkins), specialties 1 radiology / 2 surgery / 3 dentistry, links (2,1) (3,2) (3,3) (4,2) (5,1). These match upstream's `data.sql`, so there is no deviation to record.
- Leave `application.yaml` unchanged.

## 3. Java: package `org.springframework.samples.petclinic.vet` (tabs)
- `Specialty`: `@Table("specialties") record (@Id Integer id, String name)`.
- `VetSpecialty`: `@Table("vet_specialties") record (@Column("specialty_id") AggregateReference<Specialty,Integer> specialty)`.
- `Vet`: `@Table("vets") record (@Id Integer id, String firstName, String lastName, @MappedCollection(idColumn="vet_id") Set<VetSpecialty> specialties)`.
- `VetRepository extends Repository<Vet,Integer>`: `Page<Vet> findAll(Pageable)` and `List<Vet> findAll(Sort)`. Spring Data routes these to the base implementation.
- `SpecialtyRepository extends Repository<Specialty,Integer>`: `List<Specialty> findAll()`.
- `SpecialtyView(Integer id, String name)`, `VetView(Integer id, String firstName, String lastName, List<SpecialtyView> specialties)`, `Vets(List<VetView> vetList)` (the JSON wrapper).
- `VetController` (`@Controller`, constructor injection, `PAGE_SIZE = 5`):
  - `GET /vets.html` takes `@RequestParam(defaultValue="1") int page`. It throws `ResponseStatusException(BAD_REQUEST)` if `page < 1`; a non-numeric value already gives 400 from type conversion. It reads `findAll(PageRequest.of(page-1, 5, Sort.by("id")))` and throws `NOT_FOUND` if `page > 1 && page > totalPages`. It maps the vets through a single `Map<Integer,Specialty>` built from `SpecialtyRepository.findAll()`, with specialties sorted by name. The model gets `listVets` (`Page<VetView>`) and `currentPage`. It returns the view `vets/vetList`.
  - `GET /vets`, `produces = APPLICATION_JSON_VALUE`, `@ResponseBody`, returns `Vets` with all vets sorted by id.
  - A private mapper is shared by both endpoints.
- `RootController` in the root package: `GET /` returns `"redirect:/vets.html"`.

## 4. Templates and static assets
Translate the mock's inline styles into classes on Bootstrap 5.3 markup. Every colour, size and spacing value comes from a token.
- `templates/fragments/layout.html`, fragment `layout(pageTitle, activeMenu, content)`:
  - Head: `<html lang="en">`, viewport, `<title>`, `/webjars/bootstrap/css/bootstrap.min.css`, `/resources/css/petclinic.css`.
  - Header: `<header>` with Bootstrap `navbar navbar-expand-lg` inside `.container`, and `<nav aria-label="Main">`. The brand is an inline SVG heart-and-cross mark (copied from the mock, `aria-hidden`) plus "Spring PetClinic", linking to `@{/}`. A collapse toggler has an `aria-label`.
  - Nav: one item, `list-ul` icon plus "Veterinarians", linking to `@{/vets.html}`, with `aria-current="page"` when `activeMenu == 'vets'`.
  - Body and footer: `<main class="container">` holds the content. `<footer>` reads "Spring PetClinic · a Spring Boot sample application". The page loads `/webjars/bootstrap/js/bootstrap.bundle.min.js`, which only the collapse uses.
- `templates/fragments/pagination.html`, fragment `pagination(page, url, label)`:
  - Renders only when `totalPages > 1`.
  - Content: `<nav aria-label=…>`, the "Pages:" label, `[`, numbered items, `]`, then « ‹ › ».
  - The current page is a `<span aria-current="page">`. Other pages are links built with `@{${url}(page=${n})}`. Disabled chevrons are `<span aria-disabled="true">`, not links. Enabled chevrons carry `aria-label`s (First page, Previous page, Next page, Last page).
  - The owners list will reuse this fragment.
- `templates/vets/vetList.html`:
  - Calls the layout with `th:replace`, title "Veterinarians", `activeMenu='vets'`.
  - Content: h2 "Veterinarians", then `table table-striped` inside `overflow-x:auto`. The table has a `visually-hidden` caption ("Veterinarians and their specialties, page X of Y") and `<th scope="col">` Name and Specialties.
  - Rows: the name is `th:text="${vet.firstName + ' ' + vet.lastName}"`. Specialties are the space-joined names, or `<span class="text-muted-ink">none</span>`.
  - Then the pagination fragment with label "Vets pages".
  - Use `th:text` only, never `th:utext`.
- `static/resources/css/petclinic.css` (~120 lines):
  - `@font-face` for the self-hosted fonts.
  - The colour tokens as `:root` custom properties, plus overrides of Bootstrap variables (`--bs-body-font-family`, `--bs-body-color`, `--bs-link-color`, `--bs-table-striped-bg: #f4f2f0`).
  - Navbar: brown fill, `border-top: 4px solid` green, min-height 64px. Uppercase Montserrat nav items with 0.08em tracking. Active and hover use the `#3f7223` fill. Focus rings are green on dark and dark green on light.
  - Table head: 14px/700 uppercase with a 2px brown rule. Cells are padded 12×16 with a 1px `#e0dfdb` row line.
  - Pagination: plain-text styles. Footer: border-top, centred 14px muted text.
  - Spacing: 48px above the title, 24px between the table and pagination.
  - Square corners everywhere; no shadows.
- `static/resources/fonts/`: `montserrat-latin.woff2` (variable; one file covers 600 and 700), `varela-round-latin.woff2`, and `OFL.txt`. Download the woff2 files from fonts.gstatic.com, using the latin URLs from the Google Fonts CSS2 API for `Montserrat:wght@600;700&family=Varela+Round`.

## 5. Tests (`src/test/java/.../vet/` and root; tabs, JUnit 5, AssertJ; Boot 4 imports taken from the generated starters)
- `VetRepositoryTests` (`@DataJdbcTest`):
  - The seed has six vets with the expected names. Vet 3 has dentistry and surgery; vets 1 and 6 have none.
  - `findAll(PageRequest.of(0,5,Sort.by("id")))` returns 5 vets with a total of 6, and page index 1 returns 1, with specialties populated.
  - `SpecialtyRepository.findAll()` returns 3 specialties (this check is folded in here).
- `VetControllerTests` (`@WebMvcTest(VetController.class)` with `@MockitoBean` repositories). This covers everything in spec 5.1:
  - Page 1: view, model, and content type.
  - Content: names, `dentistry surgery`, `none`, and the row count.
  - Pagination: a page-2 link, `aria-current`, disabled chevrons that are not links, and the nav link marked active.
  - Errors: `page=0`, `-1` and `abc` give 400; `page=3` gives 404.
  - JSON: shape, `[]` for no specialties, no `nrOfSpecialties`/`new`; `Accept: text/html` gives 406.
  - Read-only: POST gives 405 on both paths.
  - Escaping: `<script>` is rendered escaped.
- `RootRedirectTests` (`@WebMvcTest(RootController.class)`): `GET /` returns 302 to `/vets.html`.
- `VetsIntegrationTests` (`@SpringBootTest` + `@AutoConfigureMockMvc`):
  - The real data gives 5 rows on page 1 and 1 on page 2, and `/vets` returns six vets.
  - Bootstrap CSS, `petclinic.css` and a font file return 200.
  - Actuator: `/actuator/health` is UP with no `components`/`details`; `/actuator/info`, `/actuator/env` and `/h2-console` return 404.
- `VetsPageHtmlUnitTests` (`@SpringBootTest` + `@AutoConfigureMockMvc`) uses MockMvc's HtmlUnit integration ([MockMvc and HtmlUnit](https://docs.spring.io/spring-framework/reference/testing/mockmvc/htmlunit/mah.html)). Boot auto-configures a MockMvc-backed `org.htmlunit.WebClient` when HtmlUnit is on the test classpath, so the test injects it with `@Autowired WebClient webClient`. No server starts, and pages, CSS, JS and fonts all go through MockMvc. It runs against the real seed data and acts like a browser:
  - `webClient.getPage("http://localhost/vets.html")` returns an `HtmlPage` titled "Veterinarians". It loads with no failing resource requests, including the Bootstrap CSS/JS WebJars, `petclinic.css` and the fonts.
  - Table: `HtmlTable` has 5 body rows with the expected names. Vet 1's specialties cell reads `none`, and Linda Douglas reads `dentistry surgery`.
  - Navigation: clicking the "2" pagination anchor loads page 2, where Sharon Jenkins is the only row and page 2 has `aria-current="page"`. Clicking « (First page) returns to page 1. On page 1, « and ‹ are `span`s, not `HtmlAnchor`s. Clicking the "Veterinarians" nav anchor reloads `/vets.html`, which has `aria-current`. Clicking the brand link follows the `/` redirect and lands on `/vets.html`.
  - Navbar collapse: clicking the toggler adds `show` to the collapse element, which checks that Bootstrap's bundle runs. If HtmlUnit's JS engine can't run Bootstrap 5, drop this assertion, keep it as a manual-smoke item, and set `throwExceptionOnScriptError(false)` only in that case, noting why.
  - Escaping is covered in `VetControllerTests`. HtmlUnit adds the end-to-end DOM check that no `<script>` element exists inside `<main>`.
- `pom.xml` adds `org.htmlunit:htmlunit` in test scope, version from the Boot BOM. Confirm it is managed in 4.1; if not, pin it as a property and record that in the spec.
- Keep `PetClinicApplicationTests.contextLoads()`.

## 6. Spec and docs updates
- Edit `intent/004-vets/spec.md` to record these deviations:
  1. The look follows `docs/design-system` and the reference mock, which replaces the old concern 1 (contrast): green is an accent only, and `#3f7223` carries the interactive states.
  2. Pagination is plain text, not Bootstrap's boxed `.pagination`, and is hidden when there is only one page.
  3. The brand mark is the inline SVG heart-and-cross, not the Spring logo.
  4. Fonts are self-hosted (OFL).
  5. The nav keeps the spec's single item; the mock's "Home" is intentionally left out.
  6. Page-range checks are explicit, with no `@Min` and no validation starter.
  7. Any change to the Boot 4 starter names found during implementation.
  8. Add `VetsPageHtmlUnitTests` (HtmlUnit via MockMvc) to the 5.1 test table, and add `htmlunit` to the test dependencies.
- Save this plan as `intent/004-vets/plan.md` (per the ExitPlanMode hook).
- Add a line to `CLAUDE.md` that UI work follows `docs/design-system/`.

## Verification
1. Run `sdk env`, then `./mvnw clean verify`. All tests must pass.
2. Run `spring-tools:validate` and expect no diagnostics.
3. Run the app (`run` skill or `./mvnw spring-boot:run`):
   - Open `/vets.html` and `/vets.html?page=2` in Chrome and screenshot both at desktop width. Compare them side by side with the mock (header, table stripes, pagination, footer, fonts).
   - At about 375px width, check that the navbar collapses and opens. Keyboard-tab through it to check the focus rings.
4. Check the endpoints from the command line:
   - `curl -i localhost:8080/vets -H 'Accept: application/json'` returns six vets.
   - `curl -i localhost:8080/` returns 302.
   - `curl -i 'localhost:8080/vets.html?page=0'` returns 400, and `?page=3` returns 404.
   - `curl -i localhost:8080/actuator/health` returns UP.
5. Restart the app. Confirm the data is still seeded and no `*.mv.db` file appears.
6. Lint (`actionlint` is not needed; no workflow changes), push the branch and open the PR. The PR description lists the resolved versions (Boot, Thymeleaf, Bootstrap WebJar, H2). Confirm `build.yml` and `snyk.yml` pass. Escalate any Snyk `high` finding rather than suppressing it.
