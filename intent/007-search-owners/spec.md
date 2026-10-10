# Spec: Search Owners
Source intent: [intent.md](./intent.md)
Author: matthew-js-porter
Visual reference: [PetClinic Search Owners.html](./PetClinic%20Search%20Owners.html) (blank search, "Davis" results, "zzz" not found and phone width, built with `docs/design-system/`)

## 1. Summary
Add the upstream-style last-name search to the Find Owners list from intent 006, on the same page.

- `GET /owners.html?lastName=Dav` lists only owners whose last name starts with the term, ignoring case, 5 per page, ordered by id. A blank or missing term lists all owners, as today.
- A labelled "Last Name" field and a "Find Owner" button sit above the list in a plain GET form. The term stays in the field, the pagination links keep it, and the URL can be bookmarked or shared.
- No matches: the table is replaced by a "not found" message next to the field (resolves open question 2).
- `GET /owners?lastName=Dav` applies the same filter to the JSON (resolves open question 1). With no filter it returns all owners as today.
- Form field, button and inline message are added to `docs/design-system/` in the same PR.
- Everything else (vets page and JSON, homepage, navbar, `/actuator/health`) is unchanged. No new dependency, no JavaScript, no config change.

## 2. Existing codebase context
- `OwnerController` (package-private): `GET /owners.html` takes `page` (1-based, default 1; `<1` is 400, beyond the last page is 404, page 1 is always valid), calls `OwnerRepository.findAll(Pageable)` sorted by id, maps `Owner` to `OwnerView`, renders `owners/ownersList`. `GET /owners` returns `Owners(ownerList)` for all owners via `findAll(Sort)`.
- `OwnerRepository extends Repository<Owner, Integer>` (read-only by design). The `owners_last_name` index already exists in `schema.sql`.
- `ownersList.html`: `h2.pc-page-title`, a `.table-responsive.pc-table-region` (focusable region) around `table#owners.pc-table`, then `fragments/pagination :: pagination(page, url, label)`. The pagination fragment builds links as `@{${url}(page=${n})}`, so it cannot carry other query parameters today.
- Seed owners: last names Franklin, Davis (Betty), Rodriquez, Davis (Harold), McTavish, Coleman, Black, Escobito, Schroeder, Estaban.
- Tests from 006: `OwnerRepositoryTests`, `OwnerControllerTests`, `OwnersPageHtmlUnitTests`, `OwnersIntegrationTests`.
- The design system had no form field, button or message component, and reserved `radius-control` for inputs. Both are resolved in `docs/design-system/` (concerns 1 and 2).

## 3. Requirements

### Functional
| ID | Requirement |
|----|-------------|
| F1 | `./mvnw clean verify` succeeds with all tests passing. |
| F2 | `/owners.html` shows, above the list and below the `h2`, a `<form method="get" action="/owners.html" role="search">` with a `<label for="lastName">Last Name</label>`, `<input type="search" id="lastName" name="lastName" maxlength="30" autocomplete="off">` and a `<button type="submit">Find Owner</button>`. It works without JavaScript. |
| F3 | A non-blank `lastName` returns only owners whose last name starts with the trimmed term, case-insensitively. `Davis` and `Dav` return Betty Davis and Harold Davis only; `davis` and `DAVIS` do the same; `Dx` returns none. |
| F4 | A blank, whitespace-only or missing `lastName` returns all owners, identical to intent 006 output. |
| F5 | Results are ordered by owner id and paginated at 5 per page. Every pagination link (numbered, first, previous, next, last) carries the `lastName` term, URL-encoded, plus `page`. With no term, links are identical to today. |
| F6 | After a search the field holds the (trimmed) term. It is HTML-escaped. |
| F7 | If the filter matches nobody, the table and pagination are not rendered. A message is shown directly under the search form: `No owners found with a last name starting with "<term>".` (term escaped). The response is 200. |
| F8 | A single match is rendered as a one-row list. There is no redirect. |
| F9 | `page` rules are unchanged and apply to the filtered result: `page<1` or non-numeric is 400; `page>1` beyond the filtered page count is 404; `page=1` is always valid, including for zero results. |
| F10 | A `lastName` longer than 30 characters (the column length) after trimming returns 400, with no stack trace. The input's `maxlength="30"` prevents this in the browser. |
| F11 | `GET /owners?lastName=<term>` returns 200 `application/json` with only the matching owners in `{"ownerList":[...]}`, same matching rules as F3, F4 and F10, unpaginated, ordered by id. No matches gives `{"ownerList":[]}` with 200. |
| F12 | The page is a bookmarkable GET. Reloading, or opening a shared link, shows the same result. |
| F13 | The page follows the design system (4.5), is keyboard and screen-reader usable (visible label, visible 2px focus outline on field and button, not-found message announced) and works at 320px with no page-level horizontal scroll. |
| F14 | Page title (`Owners · Spring PetClinic`), `h2` "Owners", navbar state, vets page, vets JSON, homepage and `/actuator/health` are unchanged. |

### Non-functional and constraints
- Thymeleaf in the existing layout; no JavaScript; no client-side fetching.
- Spring Data JDBC + H2 only; no JPA; no new Maven dependency; no `application.yaml` change; actuator unchanged.
- Read-only, `GET` only. The repository stays free of write methods.
- Tabs for Java, Boot 4 test packages. Any new token or component is documented in `docs/design-system/` in the same PR.

## 4. Design

### 4.1 Repository
Add derived queries to `OwnerRepository`:
```java
Page<Owner> findByLastNameStartingWithIgnoreCase(String lastName, Pageable pageable);
List<Owner> findByLastNameStartingWithIgnoreCase(String lastName, Sort sort);
```
No hand-written SQL, so the term is always a bound parameter (no injection surface). The blank case calls the existing `findAll` methods, so the no-search path is untouched. Case-insensitive prefix matching cannot use the plain `owners_last_name` index; that is fine at this scale (concern 5).

**LIKE wildcards.** A term of `%` or `_` must be treated as literal characters, not wildcards, so that `%` does not list everyone (A7). Whether Spring Data JDBC escapes `StartingWith` terms was not verified while writing this spec. The engineer adds tests with `%`, `_` and `\` (expect no matches against the seed) and, if the framework does not escape them, escapes the term before the call or uses a `@Query` with `ESCAPE`.

### 4.2 Controller
`showOwnerList(@RequestParam(defaultValue = "1") int page, @RequestParam(required = false) String lastName, Model model)`:
1. Validate `page` as today.
2. `term = lastName == null ? "" : lastName.strip()`; if `term.length() > 30`, throw 400.
3. Query the filtered or unfiltered repository method with `PageRequest.of(page - 1, PAGE_SIZE, BY_ID)`.
4. Keep the existing 404 rule (`page > 1 && page > totalPages`).
5. Model: `listOwners` (as today) and `lastName` (the trimmed term, empty string when none).

`showResourcesOwnerList(@RequestParam(required = false) String lastName)` applies steps 2 and 3 with the `Sort` variants. It uses the same parameter name as the page, so page and JSON filter identically.

Both methods share a small private helper that normalises and validates the term, so the two paths cannot diverge. The search term is never logged.

### 4.3 Templates
`ownersList.html` (visual reference: [PetClinic Search Owners.html](./PetClinic%20Search%20Owners.html)):
- After the `h2`, add the search form (4.5). `th:value="${lastName}"` on the input (escaped).
- Wrap the table region and pagination in `th:if="${listOwners.totalElements > 0}"`. Otherwise render the message element with `role="status"`. With no term and zero owners in the database the message reads `No owners found.`; this cannot happen with the seed data.
- The pagination call becomes `pagination(${listOwners}, '/owners.html', 'Owners pages', ${lastName})`.
- The table caption keeps `page N of M`; when filtered it adds the term: `Owners with last name starting with "Dav", page 1 of 1`.

`fragments/pagination.html` gains an optional fourth parameter `query` and builds links as `@{${url}(page=${n},lastName=${query})}` only when `query` is non-empty, otherwise as today. The vets page calls the fragment with three arguments and must keep working: either update the vets call to pass `''`, or add a thin wrapper fragment. The engineer picks one, and a test proves the vets pagination links are unchanged. The `step` sub-fragment needs the same parameter. Thymeleaf's link builder URL-encodes parameter values.

### 4.4 URLs
- Page: `GET /owners.html?lastName=Dav&page=2`.
- JSON: `GET /owners?lastName=Dav`.
- Deviations from upstream, as the intent requires: the search lives on the list page (upstream has `/owners/find` plus `/owners?lastName=`); a single match does not redirect to an owner details page (none exists); a not-found message replaces the empty table.

### 4.5 Design system and CSS
New documentation in `docs/design-system/` (`README.md`, a `components/<Name>/README.md` and `preview.html` each, and `bundle.css`/`tokens.json` only if tokens change):

- **FormField**: visible label in `body` type, `ink`, above the input; input is full width on phone and at most 20rem on desktop; 1px `line-strong` border, `surface` fill, `space-2`/`space-3` padding, 2px solid `focus-ring` outline on focus, never removed. Label and input are linked with `for`/`id`. Corners are square (concern 1).
- **Button (primary)**: `spring-green-dark` fill, `on-dark` white text (about 6:1, AA), Montserrat 14px/600 UPPERCASE via CSS with the label in Title Case in markup ("Find Owner"), square corners, no shadow, hover `spring-brown` fill, 2px `focus-ring` outline with a 2px offset.
- **InlineMessage**: plain `ink` body text under the field with a 4px `line-strong` left rule; no colour-only meaning, no icon, no card, no shadow. `role="status"`.
- Layout: field and button sit on one row from `sm` up and stack on phone, `space-5` below the form before the table or message.

`petclinic.css` maps these onto Bootstrap 5.3 (`form-control`, `btn`) with `pc-` classes. No new colours, so `tokens.json` should not change unless the engineer adds a named token for the input border.

### 4.6 Security and privacy
- Inputs are `lastName` (trimmed, max 30) and `page`. Bound parameter only; no string-built SQL; wildcards handled per 4.1.
- Reflected input appears in the input `value`, the not-found message and the caption. All use `th:text`/`th:value` (escaped), never `th:utext`. Tests assert that `<script>` and quote characters are escaped.
- Plain GET with no state change, so no CSRF concern. No cookies or sessions.
- Search terms are last names and go in the URL (the intent asks for bookmarkable URLs), so they may appear in server and proxy access logs and browser history. This is acceptable with the fictitious seed data; see concern 3.
- Unauthenticated access to owner names, addresses and phone numbers is unchanged from intent 006 and remains acceptable only for fictitious data. Search makes enumeration easier (prefix `a`, `b`, ...), which the unpaginated JSON already allowed.
- No new dependency, so the Snyk gate is unaffected. Snyk Code may flag the reflected parameter; the escaping above is the answer, and any finding is triaged rather than suppressed.

### 4.7 Out of scope
Owner details page and single-match redirect; a separate `/owners/find` page; searching by first name, city, telephone or pet; adding or editing owners; internationalisation; Spring Security or login; autocomplete or JavaScript enhancement.

## 5. Acceptance and verification

### 5.1 Tests
- `OwnerRepositoryTests`: `Davis`, `Dav`, `davis`, `DAVIS` give owners 2 and 4; `Dx` gives none; `D` gives owners 2 and 4 only; `E` gives Escobito and Estaban; `%` and `_` give none; a page request of size 5 over the filtered result reports the right totals; pets are still populated and the query count stays bounded.
- `OwnerControllerTests` (`@WebMvcTest`, `MockMvcTester`, mocked repository): blank, missing and whitespace terms call the unfiltered method; `lastName=Dav` calls the filtered method with the trimmed term, `PAGE_SIZE` and id order; the model contains `lastName`; no match gives 200 with the not-found message and no table; a 31-character `lastName` gives 400; `page=0` and `abc` still give 400; `page=2` with one page of results gives 404; `page=1` with zero results gives 200; JSON with and without `lastName`; JSON no match gives `{"ownerList":[]}`; `<script>` in the term is escaped in the HTML.
- `OwnersPageHtmlUnitTests` (JS off): the form exists with a labelled `lastName` input and a "Find Owner" button, using GET to `/owners.html`; submitting `Davis` shows exactly Betty Davis and Harold Davis and the field keeps `Davis`; `dav` gives the same; a blank submit shows 5 rows with pagination; `zzz` shows the message and no table or pagination; the `Davis` result has no pagination. No seed prefix matches more than 5 owners, so the pagination-keeps-term test uses the mocked-repository `@WebMvcTest` (or rows inserted in a test transaction) and asserts every link contains `lastName=`. The vets page pagination links are unchanged.
- `OwnersIntegrationTests`: `/owners?lastName=Davis` has 2 owners; `/owners` still has 10; `/owners?lastName=zzz` has 0.
- Existing vets, homepage, actuator and context tests stay green unchanged (F14).

### 5.2 Manual check
`./mvnw spring-boot:run`; open `/owners.html`. Search `Davis`, `dav`, `zzz`, blank and `%`. Tab through field and button and confirm visible focus outlines; confirm the label is read and the not-found message announced. Reload and paste the URL into a new tab. At 320px confirm the form stacks, nothing scrolls the page sideways, and the navbar toggle works. `curl 'localhost:8080/owners?lastName=Davis'` returns two owners; `/actuator/health` is `UP`.

## 6. Policy conformance
| Policy | How met |
|--------|---------|
| Design system | Existing tokens and components reused; new form field, button and inline message documented in `docs/design-system/` in the same PR; square corners, no shadows, `spring-green-dark` for the interactive fill, Title Case in markup with UPPERCASE via CSS. |
| Accessibility | Visible `<label>`, `role="search"`, `role="status"` message, visible 2px focus outlines, AA contrast, works at phone width and without JavaScript. |
| CLAUDE.md | YAML-only config and actuator untouched; Spring Data JDBC; UPPER CASE `@Table`/`@Column` names unchanged; Java 25; Boot 4 test packages; tabs. |
| Security (Snyk, REVIEW.md) | Bound parameters, validated length, escaped output, no new dependency, read-only. |
| Workflow | Branch and PR; the plan is written only after human review of this spec. |

## 7. Assumptions
Made without a human to ask; each can be reversed in review.
- A1. The JSON filter is the same `lastName` query parameter on `GET /owners` (the intent's suggested default).
- A2. No matches shows only the message, not an empty table (the intent's suggested default). Pagination is hidden too.
- A3. No-match responses are HTTP 200, not 404, for both page and JSON; an empty result is a valid answer.
- A4. The term is trimmed of leading and trailing whitespace before matching and before it is shown back in the field. Internal whitespace is kept.
- A5. The label text is "Last Name" and the button "Find Owner", as the intent says; the `h2` stays "Owners" as in intent 006.
- A6. Terms longer than 30 characters (the `last_name` column length) return 400 rather than an empty result.
- A7. `%` and `_` are literal characters, not LIKE wildcards.
- A8. Results stay ordered by owner id, not by last name, keeping 006's order and a stable JSON.
- A9. The input is `type="search"` with `autocomplete="off"`; there is no JavaScript.
- A10. Inputs and buttons use square corners, in line with the "square everywhere" rule (concern 1).
- A11. The pagination fragment is extended with an optional query argument rather than duplicated.
- A12. The seed data has no prefix matching more than 5 owners, so pagination with a search term is tested with a mocked repository or extra rows inserted in a test transaction, not by changing `data.sql`.

## 8. Areas of concern
1. **Design system contradicts itself on corners.** `README.md` says corners are square everywhere (`radius-none`) yet also that `radius-control` is "held back for form inputs". Both cannot hold for a visible input. This spec chooses square (A10) because the rule is stated absolutely and a rounded input beside a square button would look inconsistent. **Resolved:** the design owner confirmed square; `radius-control` is removed and the README now says corners are square everywhere, inputs and buttons included.
2. **Design-system gap.** There are no form field, button or message components. They are specified here and must be added with previews in the same PR; the published design system artifact should also receive them to avoid drift from the mirror. **Resolved:** FormField, Button and InlineMessage (plus a `field-width` token) are in `docs/design-system/` and published to the artifact.
3. **Search terms in URLs.** A bookmarkable GET puts last names in access logs, proxy logs and browser history. Acceptable for fictitious data; if real owner data is ever loaded, reconsider (POST with redirect, log scrubbing) together with authentication, which remains out of scope.
4. **Wildcard handling is unverified.** Whether Spring Data JDBC escapes `%` and `_` in `StartingWith` was not confirmed; the tests in 5.1 decide, with the fallback in 4.1.
5. **Index use and scale.** Case-insensitive prefix search does not use the plain `owners_last_name` index. Irrelevant at 10 rows; revisit (functional index or collation) if the clinic grows. The JSON stays unpaginated as in 006 and still grows without bound.
6. **Shared pagination fragment.** Changing it risks the vets page; a test must prove the vets links are unchanged.
7. **Behaviour differs from upstream.** No redirect on one match, no separate find page, and a message instead of upstream's validation-style error. Recorded here as the intent requires.
