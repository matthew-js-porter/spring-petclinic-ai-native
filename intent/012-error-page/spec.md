# Spec: Custom Error Page
Source intent: [intent.md](./intent.md)
Author: matthew-js-porter
Visual reference: [PetClinic Error Page.html](./PetClinic%20Error%20Page.html) (404, 400, 405, 500 and other 4xx at desktop; 404, 400, 405 and 500 at 320px; built with `docs/design-system/`). The wording awaits design-owner review (concern 5).

## 1. Summary
Replace Spring Boot's default error output for browser requests with one designed error page in the shared layout, keeping the HTTP status of the failed request.

- A single Thymeleaf template, `templates/error.html`, picked up by Boot's `DefaultErrorViewResolver` for every status. Boot's `BasicErrorController` stays; no custom controller, no `@ControllerAdvice`.
- The page shows the shared navbar (no item current) and footer, an `h2` naming the error, one plain sentence appropriate to the status, and two action links: the homepage and Find Owners.
- Statuses with their own wording: 404, 400, 405, 500. Every other 4xx or 5xx gets a generic sentence for its class.
- Nothing from the exception, request or error attributes is rendered: no message, exception class, trace, SQL or path. The page is chosen by status alone.
- JSON clients keep getting Boot's JSON error body (`timestamp`, `status`, `error`, `path`). Boot's defaults already leave out the message, exception, trace and binding errors, and no `server.error.*` settings are added.
- The design system gains an **ErrorPage** page pattern (README and `preview.html`), in the same PR. No new colour, radius, shadow or asset.
- Upstream's deliberate-error demo and its illustration are left out (concern 1).
- Out of scope: changing which requests fail or their status codes, error logging or monitoring, i18n, login.

## 2. Existing codebase context
- Boot 4.1 with `spring-boot-starter-webmvc` and Thymeleaf. No `templates/error*`, no `@ControllerAdvice`, no `server.error.*` settings, so Boot's whitelabel page serves HTML errors today and its JSON body serves non-HTML clients. `application.yaml` has only `spring.application.name` and cookie-only session tracking.
- Errors are raised as `ResponseStatusException` (owner or pet unknown 404 in `OwnerController`/`PetController`; page below 1 or lastName over 30 characters 400; page past the last 404 in `OwnerController`/`VetController`). Other 4xx come from Spring MVC itself: a non-numeric id (`/owners/abc.html`) is a type mismatch 400, a wrong method is 405, an unmapped path is 404, `GET /vets` with `Accept: text/html` is 406 (`VetControllerTests`).
- HTML at `*.html`, JSON at the bare path (`/owners`, `/vets`); `GET /owners/{id}` is JSON for any `Accept` (`OwnerController.showResourcesOwner`).
- The shared layout `fragments/layout :: layout(pageTitle, activeMenu, content)` takes `activeMenu`; the navbar sets `aria-current` only when it equals `home`, `owners` or `vets`, so any other value (or `null`) marks nothing current. Pages call it like `welcome.html` does.
- CSS already has `.pc-main`, `.pc-page-title`, `.pc-empty` (muted sentence) and `.pc-action` (action link, per the design system's "an action that opens another page is an action link").
- Every existing test that touches an error uses MockMvc and asserts only the status code (`VetControllerTests`, `OwnersIntegrationTests`, `VetsIntegrationTests`, the controller tests). MockMvc does not perform the servlet container's error dispatch to `/error`, so those tests never see the error page or the whitelabel page and keep passing unchanged.
- Design system today: no error or "empty page" pattern. It allows `pets.png` on the homepage only and says "no illustrations, and no other photographs" (concern 1).

## 3. Requirements

### Functional
| ID | Requirement |
|----|-------------|
| F1 | `./mvnw clean verify` succeeds with all tests passing. |
| F2 | For a browser request (`Accept: text/html`) that ends in an error, the response keeps its status and body is the error page in the shared layout (header with navbar, `main`, footer). Covers at least: 404 for an unknown owner or pet id (`/owners/999.html`, `/owners/1/pets/999/edit.html`), an unmapped path, a page past the last; 400 for a non-numeric id (`/owners/abc.html`), `page=0`, an over-long `lastName`; 405 for `POST /owners.html` or `POST /vets.html`; 500 for an unhandled exception. |
| F3 | The page title (`<title>`) is `<heading> · Spring PetClinic`; the `h2` (`pc-page-title`) is the heading from the table in 4.2. Exactly one `h2`. |
| F4 | Below the heading is one sentence in plain words, chosen by status only (table in 4.2). 404, 400, 405 and 500 each have their own; any other 4xx and any other 5xx use the class sentence. A status below 400 or missing (a direct visit to `/error`, which Boot treats as 500) renders as 500. |
| F5 | The page offers a way on: two action links, "Go to the Homepage" (`/`) and "Find Owners" (`/owners.html`, the navbar's href), identical for every status. |
| F6 | No navbar item has `aria-current`: the layout is called with `activeMenu` = `null`. |
| F7 | No internals reach the browser for any status, including 500: the HTML contains none of the request path, the exception class or message, a stack trace, SQL, or the `message`/`trace`/`exception`/`errors`/`path` model attributes. |
| F8 | JSON clients are unchanged in shape: `GET /owners/999` with `Accept: application/json` is 404 with Boot's JSON error body; `/owners`, `/owners/{id}`, `/vets` errors are JSON for `Accept: application/json` and for a client sending `*/*` (curl). The JSON body has no `message`, `trace`, `exception` or `errors` fields. |
| F9 | Every page and endpoint that works today behaves as before (status, body, headers, including the `Allow` header on a 405). |
| F10 | Unknown 4xx/5xx statuses and exceptions thrown while rendering any view end in the error page (or, if the error page itself cannot render, Boot's whitelabel page, which shows no trace or message; see concern 4). |

### Non-functional
| ID | Requirement |
|----|-------------|
| N1 | Server-side Thymeleaf only; no JavaScript added. |
| N2 | Config stays YAML. Actuator surface unchanged (`health` only). No new dependency. |
| N3 | Accessible: landmarks from the layout, one `h2`, readable heading order, visible 2px focus outline on the links (existing `.pc-action` style), contrast per the existing tokens. No `role="alert"` (it is a full page, not a live update). |
| N4 | Works at 320px: text wraps, nothing scrolls sideways. |
| N5 | Design-system conformance: square corners, no card, shadow, gradient, icon or emoji; `ink` body text, `ink-muted` only for secondary text; no exclamation marks. |

## 4. Design

### 4.1 Mechanism
- Add `src/main/resources/templates/error.html`. Boot's `DefaultErrorViewResolver` tries `error/<status>`, then `error/4xx` or `error/5xx`, then `error`; with only `error.html` present, all statuses use it. `BasicErrorController.errorHtml` sets the response status from the original request, so the status is kept (F2).
- The template reads one model attribute, `status` (an `Integer`, set by `BasicErrorController`), and switches on it with `th:switch`/`th:case` or a `th:with` lookup. It does not read `error`, `message`, `path`, `exception`, `trace` or `errors`. Do not use `#request`/`#httpServletRequest` to read the original URI or exception.
- No new Java is needed for the page. Content negotiation stays Boot's: `errorHtml` is `produces=text/html`; everything else goes to `error()` (JSON).
- `application.yaml` is not changed. No `server.error.*` properties are added: Boot's defaults already omit the message, exception, trace and binding errors, and the tests below pin that behaviour (F7, F8). Do not set `management.*`.
- No `@ExceptionHandler`/`@ControllerAdvice`: the status and exceptions stay each feature's own decision (intent: out of scope). An unhandled exception still logs through Spring's usual path; logging is unchanged.

### 4.2 Content
Heading is Title Case, the sentence is plain, short, ends with a full stop, no exclamation mark, no quotation of user input.

| Status | `<title>` / `h2` | Sentence |
|---|---|---|
| 404 | Page Not Found | We could not find the page you asked for. It may have moved, or the address may be mistyped. |
| 400 | Bad Request | The address or the details sent could not be understood. Check the link and try again. |
| 405 | Method Not Allowed | This page cannot be used in that way. Go back and use the links on the page instead. |
| 500 | Something Went Wrong | The clinic ran into a problem on its side. Try again in a moment. |
| other 4xx | Request Not Completed | The request could not be completed. Check the link and try again. |
| other 5xx | Something Went Wrong | (same as 500) |

The 404 sentence is the same for an unknown owner, pet, page number and path: the page does not say which (no enumeration, no echo). Wording is final unless the design owner changes it in review.

Layout, in `main`:
```
<h2 class="pc-page-title">Page Not Found</h2>
<p>…sentence…</p>
<ul class="list-unstyled">   (two .pc-action links, one per line)
```
`<p>` in `ink` body style; the action links use `.pc-action`. Spacing follows the existing page scale (`space-6` below the navbar via `.pc-main`, `space-5` between the heading and sentence as on the homepage, `space-3` between links). If the existing CSS lacks a class for the list, add one small `.pc-error-actions` rule to `petclinic.css` that maps to tokens, not hard-coded values.

### 4.3 Design-system change (same PR)
- `docs/design-system/components/ErrorPage/README.md` and `preview.html`: a page pattern (like OwnerForm and PetForm) composed from Navbar, Footer and the action-link style. Documents: heading and sentence table above, no current nav item, two action links, no image or icon, no status code or technical text on the page, never quote user input, 320px behaviour. The preview shows 404, 400, 405 and 500 at desktop and 320px.
- `docs/design-system/README.md`: list ErrorPage with the other page patterns, add the error-wording rules to Content ("Error pages state what happened in one plain sentence and never show internals") and keep the Iconography rule unchanged (concern 1).
- `CLAUDE.md`'s sentence listing page patterns ("plus the OwnerForm and PetForm page patterns") is updated to include ErrorPage. `design-system.json` blob ids are unchanged because no asset is added.

### 4.4 Files touched
- New: `src/main/resources/templates/error.html`, `docs/design-system/components/ErrorPage/{README.md,preview.html}`, new tests (section 6).
- Changed: `src/main/resources/static/resources/css/petclinic.css` (only if 4.2 needs the list rule), `docs/design-system/README.md`, `CLAUDE.md`.
- No change to controllers, repositories or other templates.

## 5. Security
- Information disclosure is the point of the intent (Snyk and `/security-review` check for it). Controls: the template renders status-keyed static text only (4.1); Boot's defaults omit message, exception and trace; tests assert absence of internals (6).
- The HTML never echoes the request path or any input, so there is no reflected-content or XSS surface on the page. Thymeleaf's `th:text` escaping would apply regardless.
- The JSON body still carries `path` and the reason phrase (`error`), as today. Neither is an internal; the path is the caller's own request. It is not rendered in HTML.
- A direct visit to `/error` renders the 500 page with status 500 and reveals nothing. Left as Boot's behaviour.
- No new endpoint, no state change, no session use, no CSRF/login impact (both out of scope, as in every intent so far).

## 6. Testing
Because MockMvc does not forward to `/error` (section 2), the error page is tested against a running server context; status-only tests stay as they are.

- **Error page tests**, `@SpringBootTest(webEnvironment = RANDOM_PORT)` with the Boot 4 test client (check the generated imports, as `CLAUDE.md` says), request `Accept: text/html`:
  - 404 via `/owners/999.html`, `/owners/1/pets/999/edit.html`, `/nope`, `/vets.html?page=99`: status 404, heading "Page Not Found", navbar present, footer text, no element with `aria-current`.
  - 400 via `/owners/abc.html` and `/owners.html?page=0`: status 400, "Bad Request".
  - 405 via `POST /vets.html`: status 405, "Method Not Allowed", `Allow` header present.
  - Both action links present with the right hrefs.
  - Absence of internals for each case: body contains none of `Exception`, `org.springframework`, `at java`, `trace`, the request path (e.g. `999`, `abc`), or the text of the `ResponseStatusException` reason ("does not exist", "must be 1 or greater").
- **500**: a test-only controller in `src/test` (a `@TestConfiguration` imported by that test, never in `src/main`) mapped to a throwaway path throws `IllegalStateException("jdbc:h2:mem secret SELECT * FROM owners")`. Assert status 500, "Something Went Wrong", and that neither the HTML nor an `Accept: application/json` response contains the message, `IllegalStateException`, `SELECT`, or `jdbc`. A second path throws from inside a view (or a data-access exception) to cover rendering errors if cheap.
- **JSON**: `GET /owners/999` and `/vets.html`-style failures with `Accept: application/json` and with `*/*` return JSON error bodies whose field set is exactly Boot's default minus nothing added; no `message`, `trace`, `exception`, `errors`.
- **Template unit check** (HtmlUnit or plain rendering, matching existing `*PageHtmlUnitTests`): the generic 4xx and 5xx wording, and a status below 400, render.
- **Existing tests**: not modified; run the full suite to prove F9. The new tests share a cached context where possible, but `RANDOM_PORT` needs its own context, so expect one extra startup in CI.
- Manual: with `./mvnw spring-boot:run`, open `/owners/999.html`, `/owners/abc.html`, `/nope` in a browser at 320px and desktop; `curl -i localhost:8080/owners/999` returns JSON.

## 7. Areas of concern
1. **Illustration versus design system.** The intent says "including upstream's pets illustration if it fits". Upstream's error page shows `pets.png`, the same photo as its homepage. The design system allows that photograph on the homepage only and bans illustrations and other photographs. These conflict. Decision in this spec: no image on the error page. A repeated photograph on a failure page adds weight (970 KB) and no meaning, and "if it fits" gives permission to omit it. If the design owner wants it, the Iconography rule must change in `docs/design-system/` in the same PR; the page would reuse the existing asset (no new blob).
2. **Content negotiation for `/owners/{id}`.** That endpoint returns JSON for any `Accept`, so a browser opening `/owners/999` (not `.html`) gets the HTML error page (its `Accept` is `text/html`) while a JSON client gets JSON. This matches "browser requests show the page, JSON clients keep JSON", but it is a judgement the first time a human sees it. Tests pin both behaviours. If a client sends only `*/*`, Boot returns JSON (curl); verify in the test rather than assume.
3. **MockMvc cannot see the error page.** Existing tests prove statuses only, so green tests today say nothing about the page. New coverage needs a real server context (6), which costs a context and a few seconds in CI. The alternative, invoking `/error` with hand-set request attributes in MockMvc, tests the template but not the wiring (Boot's resolver, status preservation), so it is not used as the main proof.
4. **Failure inside the error page.** If `error.html` itself throws (for example a layout change), Boot falls back to the whitelabel page, which shows no trace or message with the pinned settings but is off-brand and says "type=Internal Server Error". Keeping `error.html` free of data lookups reduces this. A test renders each status so a layout regression fails the build.
5. **Wording ownership.** Headings and sentences are new copy invented here; the design system has no precedent besides "plain, short English". Final text needs the design owner's review, and it lives in one table (4.2) plus the ErrorPage README.
6. **Parallel work.** Intent 011 (visits) is not on `main` and will add more 404 and 400 paths. Nothing here blocks it: the template is status-driven, so new failures are covered with no change. Its tests that assert status only are unaffected.
7. **No policy contradiction found** between `CLAUDE.md`, `REVIEW.md` and the design system, other than concern 1.

## 8. Assumptions
1. One template (`error.html`) with per-status wording inside it, rather than `error/404.html` etc.; fewer files and one place for the layout.
2. Statuses with bespoke wording are the four the intent lists (404, 400, 405, 500). Others (for example 406, 415) get a class-level sentence because the intent says "for any error".
3. The page never shows the numeric status, the reason phrase from Boot, or the request path; the intent only requires "plain words".
4. Headings and sentences are as in 4.2; "Go to the Homepage" and "Find Owners" are the two ways on. Find Owners links to `/owners.html`, as the navbar does.
5. No illustration (concern 1); no icon.
6. No `server.error.*` properties are added; Boot's defaults are relied on and tested.
7. "JSON clients" means requests whose `Accept` is `application/json` or `*/*`; a request with `Accept: text/html` always gets HTML, including on `/owners/{id}`.
8. A direct visit to `/error` is not treated specially.
9. The 500 path is proven with a test-only controller; nothing is added to `src/main` for it.
10. No Java change: the template needs only the `status` attribute Boot already supplies.
11. `ErrorPage` is documented as a page pattern, not a ninth component, so the "eight components" wording in the design system stays true.
