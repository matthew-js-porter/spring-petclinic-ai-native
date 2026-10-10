# Plan: Intent 007 — Search Owners

## Context
Intent 006 shipped a paginated Find Owners list (`/owners.html`) and JSON (`/owners`). Intent 007 adds an upstream-style last-name prefix search on the same page and the same filter on the JSON, per `intent/007-search-owners/spec.md`. The design-system half is already merged (PR #31): FormField, Button, InlineMessage and the `field-width` token are in `docs/design-system/` and `components/bundle.css` (`.pc-form`, `.pc-field`, `.pc-input`, `.pc-button`, `.pc-message`). The visual reference is `intent/007-search-owners/PetClinic Search Owners.html`. What remains is the app code, CSS mapping, tests and the spec/plan bookkeeping.

Work on branch `feature/007-search-owners`, land via PR. Never push to `main`.

## 1. Repository — `owner/OwnerRepository.java`
Add the two derived queries from spec 4.1 (no `@Query`, bound parameters):
```java
Page<Owner> findByLastNameStartingWithIgnoreCase(String lastName, Pageable pageable);
List<Owner> findByLastNameStartingWithIgnoreCase(String lastName, Sort sort);
```
Wildcards: the `%`/`_`/`\` repository tests decide. If Spring Data JDBC does not escape them (I expect it does, via its `Escaper` on LIKE parts), escape `\`, `%`, `_` with `\` in the controller helper before the call (H2's default LIKE escape is `\`) and record that in the spec. Keep the repository read-only.

## 2. Controller — `owner/OwnerController.java`
- `static final int MAX_LAST_NAME = 30;`
- Private helper `static String searchTerm(String lastName)`: `null` → `""`, else `strip()`; length > 30 → `ResponseStatusException(BAD_REQUEST, "lastName must be 30 characters or fewer")`. Never logs the term.
- `showOwnerList(@RequestParam(defaultValue = "1") int page, @RequestParam(required = false) String lastName, Model model)`: existing page check → `term = searchTerm(lastName)` → `PageRequest.of(page - 1, PAGE_SIZE, BY_ID)` → `term.isEmpty() ? owners.findAll(pageable) : owners.findByLastNameStartingWithIgnoreCase(term, pageable)` → existing 404 rule → model `listOwners` (mapped) and `lastName` (term).
- `showResourcesOwnerList(@RequestParam(required = false) String lastName)`: same helper, `Sort` variants.
- Reuse existing `toView`, `BY_ID`, `PAGE_SIZE`.

## 3. Templates
`templates/owners/ownersList.html` (after the `h2`):
```html
<form method="get" th:action="@{/owners.html}" role="search" class="pc-form">
	<div class="pc-field">
		<label for="lastName" class="form-label">Last Name</label>
		<input type="search" id="lastName" name="lastName" maxlength="30" autocomplete="off"
			   class="form-control pc-input" th:value="${lastName}">
	</div>
	<button type="submit" class="btn pc-button">Find Owner</button>
</form>
```
- Wrap the table region + pagination in `<th:block th:if="${listOwners.totalElements > 0}">`; `th:unless` renders `<p class="pc-message" role="status">` with `th:text` = `|No owners found with a last name starting with "${lastName}".|` when a term is set, else `No owners found.`
- Caption: when `lastName` non-empty, `|Owners with last name starting with "${lastName}", page X of Y|`; otherwise unchanged.
- Pagination call: `pagination(${listOwners}, '/owners.html', 'Owners pages', ${lastName})`.
- `th:text`/`th:value` only, never `th:utext`.

`templates/fragments/pagination.html`:
- Signature becomes `pagination (page, url, label, query)`; the `step` sub-fragment already sees `url` from the enclosing scope and will see `query` the same way.
- Each `href` becomes `${#strings.isEmpty(query)} ? @{${url}(page=${n})} : @{${url}(page=${n},lastName=${query})}` (same for `step` with `target`), so links without a term are byte-identical to today and Thymeleaf URL-encodes the term.
- `templates/vets/vetList.html`: pass `''` as the fourth argument (chosen over a wrapper fragment: one fragment, one explicit call site). A test proves vets links are unchanged.

## 4. CSS — `static/resources/css/petclinic.css`
Port the bundle.css rules into the app's `--pc-` token names and tab style, under a `/* Form field */`, `/* Button */`, `/* Inline message */` section:
- `--pc-field-width: 20rem;` in `:root` (Size).
- `.pc-form` (flex column, gap 12px, margin 0 0 24px; row + `align-items: flex-end` from 576px), `.pc-field` (column, gap 4px; width `--pc-field-width` from 576px), `.pc-field .form-label` (margin 0, body 16/24, ink).
- `.pc-input` on `form-control`: padding 8px 12px, 1px `--pc-line-strong` border, surface bg, ink, 16/24; `:focus` 2px `--pc-focus-ring` outline, offset 2px, `box-shadow: none`, keep border colour.
- `.pc-button` on `btn`: Montserrat 14px/24px 600, 0.08em, uppercase, padding 8px 16px, `--pc-on-dark` on `--pc-spring-green-dark` with matching 1px border; hover/active `--pc-spring-brown`; focus-visible 2px ring offset 2px (existing `button:focus-visible` rule already covers it; add `box-shadow: none` for Bootstrap's `.btn:focus-visible`).
- `.pc-message`: margin 0 0 24px, padding 8px 16px, body 16/24, ink, `border-left: 4px solid var(--pc-line-strong)`.
- `--bs-border-radius: 0` already set; no new colours. `tokens.json` unchanged (field-width already there).

## 5. Tests (tabs, Boot 4 test packages, AssertJ)
- `OwnerRepositoryTests` (`@DataJdbcTest`): `Davis`/`Dav`/`davis`/`DAVIS` and `D` → ids 2, 4; `Dx` → none; `E` → Escobito, Estaban; `%`, `_`, `\` → none (parameterised); `PageRequest.of(0, 5, id)` over `E` → 2 elements, 1 page, pets populated; bounded query count like the existing `readsAPageWithQueriesBoundedByThePageSize`.
- `OwnerControllerTests` (`@WebMvcTest`, `MockMvcTester`, `@MockitoBean`): missing/blank/whitespace `lastName` → `findAll(Pageable)` only (verify, `never()` on filtered); `lastName=%20Dav%20` → filtered called with `"Dav"`, size 5, id sort, model `lastName=Dav`, field value `Dav`; no match → 200, message text, no `<table`, no pagination; 31 chars → 400 (page and JSON), 30 chars ok; `page=0`/`abc` still 400; `page=2` with one filtered page → 404; `page=1` zero results → 200; pagination links with a term (stub 6 Davis-like owners) all contain `lastName=Dav` and `page=`; term with a space/`&` is URL-encoded in links; JSON with/without term, no match → `{"ownerList":[]}`; `<script>"` term escaped in input value, message and caption.
- `VetControllerTests`: assert page links are exactly `/vets.html?page=2` (no `lastName`).
- `OwnersPageHtmlUnitTests` (JS off, real seed): form is GET to `/owners.html` with labelled `lastName` input (`maxlength` 30) and "Find Owner" button; type `Davis` + click → exactly Betty and Harold Davis, field keeps `Davis`, no pagination nav; `dav` same; blank submit → 5 rows + pagination; `zzz` → `role="status"` message, no table, no pagination; URL after submit contains `lastName=Davis` (bookmarkable, reload gives same rows).
- `OwnersIntegrationTests`: `/owners?lastName=Davis` → 2, `/owners` → 10, `/owners?lastName=zzz` → 0.
- Existing vets, welcome, actuator and context tests stay green unchanged.

## 6. Spec and docs
- Edit `intent/007-search-owners/spec.md` only for real deviations: the pagination choice (pass `''` from vets), the wildcard outcome (resolve concern 4 with what the tests showed), anything else found during implementation.
- No `docs/design-system/` changes expected; if the CSS mapping exposes a gap, update docs + `design-system.json` in the same PR.
- Save this plan as `intent/007-search-owners/plan.md`.

## Verification
1. `sdk env`, then `./mvnw clean verify` — all green.
2. `spring-tools:validate` — no diagnostics.
3. `./mvnw spring-boot:run`, open `/owners.html` in Chrome: search `Davis`, `dav`, `zzz`, blank, `%`; tab to field and button (2px outlines); compare to the visual reference at desktop and 320px (form stacks, no horizontal page scroll, navbar toggles); reload/paste the URL.
4. `curl 'localhost:8080/owners?lastName=Davis'` → 2 owners; `?lastName=zzz` → `{"ownerList":[]}`; 31-char term → 400; `/vets.html` links unchanged; `/actuator/health` UP.
5. Push branch, open PR; confirm `build.yml`, `snyk.yml` and the Claude review pass; triage (don't suppress) any Snyk Code finding on the reflected parameter.
