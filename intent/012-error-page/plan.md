# Plan: Custom Error Page (intent 012)

## Context
Browser requests that fail (unknown owner/pet, bad id, wrong method, unhandled exception) currently show Spring Boot's whitelabel page: no navbar, no way back, off-brand. Spec `intent/012-error-page/spec.md` replaces it with one Thymeleaf `error.html` in the shared layout, chosen by status only, leaking no internals, while JSON clients keep Boot's JSON error body.

The design-system half of the spec (§4.3) is **already on `main`** (PR #53): `docs/design-system/components/ErrorPage/{README.md,preview.html}`, the README content/action-link rules, `tokens.json` usages, `bundle.css` rules `.pc-error-text` / `.pc-error-actions`, and the `CLAUDE.md` page-pattern sentence. So this PR is the app side only, plus a spec note recording that.

Branch: `feature/012-error-page` (matches `feature/010-add-edit-pet` etc.). PR to `main`.

## Changes

### 1. `src/main/resources/templates/error.html` (new)
- Same shape as `welcome.html`, calling `fragments/layout :: layout(heading, null, ~{::content})`. `th:replace` runs before `th:with` on the same element, so compute the heading as an inline conditional expression on `${status}` in the layout call, and the heading/sentence for the content fragment with a `th:with` inside the `th:fragment="content"` block. Tests prove each status renders.
- Status mapping (spec §4.2 / ErrorPage README):
  - 404 → "Page Not Found" / "We could not find the page you asked for. It may have moved, or the address may be mistyped."
  - 400 → "Bad Request" / "The address or the details sent could not be understood. Check the link and try again."
  - 405 → "Method Not Allowed" / "This page cannot be used in that way. Go back and use the links on the page instead."
  - other 400–499 → "Request Not Completed" / "The request could not be completed. Check the link and try again."
  - 500, other 5xx, <400 or null → "Something Went Wrong" / "The clinic ran into a problem on its side. Try again in a moment."
- Content: `<h2 class="pc-page-title">`, `<p class="pc-error-text">`, `<ul class="pc-error-actions">` with `<li><a class="pc-action" th:href="@{/}">Go to the Homepage</a></li>` and `<li><a class="pc-action" th:href="@{/owners.html}">Find Owners</a></li>`.
- Reads only `status`. No `message`/`error`/`path`/`trace`/`exception`/`errors`, no `#request`. No comments mentioning intents.

### 2. `src/main/resources/static/resources/css/petclinic.css`
Port the two rules from `docs/design-system/components/bundle.css:122-124` with the literal values the file already uses (12px = space-3, 16px = space-4), placed after the `.pc-action` rules (~line 419):
```css
/* Error page: the sentence, then its two action links stacked space-3 apart */
.pc-error-text { margin: 0 0 16px; overflow-wrap: anywhere; }
.pc-error-actions { display: flex; flex-direction: column; align-items: flex-start; gap: 12px; margin: 0; padding: 0; list-style: none; }
.pc-error-actions .pc-action { margin: 0; }
```
(match the file's existing multi-line formatting and comment style).

### 3. Tests (new, root package `org.springframework.samples.petclinic`, next to `WelcomePageHtmlUnitTests`)
**`ErrorPageIntegrationTests`**: `@SpringBootTest(webEnvironment = RANDOM_PORT)` with `@LocalServerPort` and Spring's `RestClient` (spring-web is already on the classpath). Boot 4's `TestRestTemplate`/`RestTestClient` live in `spring-boot-resttestclient`, which the pom lacks, and N2 rules out new dependencies. Use `.exchange(...)` so non-2xx statuses don't throw; a helper returns status, headers and body. Parse HTML bodies with HtmlUnit (already a test dependency) through `StringWebResponse` into a JS-disabled `WebClient`, so the same `querySelectorAll` assertions as the existing page tests apply.
- `@Import` a nested `@TestConfiguration` registering a test-only `@RestController`/`@Controller` with `GET /test-errors/boom` throwing `IllegalStateException("jdbc:h2:mem secret SELECT * FROM owners")` (and, if cheap, `/test-errors/view` returning a view name whose template doesn't exist → rendering failure 500).
- Parameterised HTML cases (`Accept: text/html`):
  - 404: `/owners/999.html`, `/owners/1/pets/999/edit.html`, `/nope`, `/vets.html?page=99` → "Page Not Found"
  - 400: `/owners/abc.html`, `/owners.html?page=0`, `/owners.html?lastName=` + 31 chars → "Bad Request"
  - 405: `POST /vets.html`, `POST /owners.html` → "Method Not Allowed", `Allow` header present
  - 500: `/test-errors/boom` → "Something Went Wrong"
  - other 4xx: `GET /vets.html` is fine; use a 406/415 path — `GET /vets` with `Accept: text/html` is 406 per `VetControllerTests` → "Request Not Completed" (check it actually reaches `errorHtml`; if Boot renders it differently, use a test-controller `ResponseStatusException(HttpStatus.CONFLICT)` instead).
  - direct `/error` → 500 "Something Went Wrong".
  For each: status kept; `<title>` = "<heading> · Spring PetClinic"; exactly one `h2`, `main h2.pc-page-title` text; `p.pc-error-text` sentence; header nav + `footer.pc-footer` present; no element with `aria-current`; the two `.pc-error-actions a.pc-action` with hrefs `/` and `/owners.html`; no `role="alert"`.
- Absence of internals in every HTML body: none of `Exception`, `org.springframework`, `at java`, `trace`, `does not exist`, `must be 1 or greater`, `jdbc`, `SELECT`, `secret`, nor the path-specific fragments (`999`, `abc`, `nope`).
- JSON (F8): `GET /owners/999` with `Accept: application/json` and with `*/*` → 404, `Content-Type` JSON, body parsed (Jackson `JsonMapper`, already present) has keys exactly `{timestamp,status,error,path}`; same for `/test-errors/boom` (500) asserting no `message`/`trace`/`exception`/`errors` and the secret strings absent. Also `/vets?page=0` JSON 400.

**Template-level check (spec §6 "template unit check")** is covered by the parameterised cases above (404/400/405/500/other-4xx/direct-/error); a further 5xx-other case via test controller `ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)` → "Something Went Wrong". No separate rendering harness needed.

Existing tests are not modified.

### 4. `intent/012-error-page/spec.md`
Add a short "Deviations" note: §4.3 design-system changes landed earlier in PR #53 rather than this PR; tests use `RestClient` + `@LocalServerPort` (no `spring-boot-resttestclient` dependency); assumption 11's "eight components" now reads nine. Save the approved plan as `intent/012-error-page/plan.md` (per the ExitPlanMode hook).

## Verification
1. `sdk env` then `./mvnw clean verify` — all tests green, including existing ones untouched (F1, F9).
2. Run `spring-tools:validate` for diagnostics.
3. `./mvnw spring-boot:run`, then:
   - `curl -i -H 'Accept: text/html' localhost:8080/owners/999.html` → 404 HTML with "Page Not Found"
   - `curl -i localhost:8080/owners/999` → 404 JSON `{timestamp,status,error,path}`
   - `curl -i -X POST -H 'Accept: text/html' localhost:8080/vets.html` → 405 page with `Allow`
4. Use the `run` skill / browser to eyeball `/owners/999.html`, `/owners/abc.html`, `/nope` at desktop and 320px against `intent/012-error-page/PetClinic Error Page.html`.
5. Commit, push `feature/012-error-page`, open PR; CI (build, Snyk, Claude review/security-review) must pass.
