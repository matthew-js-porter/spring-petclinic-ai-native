# Spec: Homepage
Source intent: [intent.md](./intent.md)
Author: matthew-js-porter

## 1. Summary
Replace the root redirect with a real welcome page, as in upstream Spring PetClinic.

- `GET /` returns a Thymeleaf page (200, no redirect) in the shared layout, with a "Welcome" `h2` and upstream's pets illustration with meaningful alt text.
- The navbar gains a "Home" item before "Veterinarians", with a house icon, marked `aria-current="page"` on the homepage. Veterinarians stays current on the vets page. The brand link already points to `/` and now lands on the homepage.
- The pets image is added as a static asset and recorded in `docs/design-system/` (the system currently says "no illustrations").
- The vets page, `/vets` JSON and `/actuator/health` do not change.

## 2. Existing codebase context
- `RootController` maps `GET /` to `redirect:/vets.html`; `RootRedirectTests` (`@WebMvcTest`) asserts that redirect. Both are replaced.
- `templates/fragments/layout.html` is `layout(pageTitle, activeMenu, content)`. The navbar has one hand-written `<li>` (Veterinarians, inline `bi-list-ul` SVG, `aria-current` set when `activeMenu == 'vets'`). The brand link is `@{/}`. Title is `"<pageTitle> · Spring PetClinic"`.
- `templates/vets/vetList.html` uses `layout('Veterinarians', 'vets', ...)` and an `h2.pc-page-title`.
- `static/resources/` holds `css/petclinic.css` and `fonts/`; there is no `images/` directory.
- Tests: `VetsPageHtmlUnitTests` (HtmlUnit over MockMvc, JavaScript disabled), `VetControllerTests`, `VetsIntegrationTests`, `PetClinicApplicationTests`.
- `docs/design-system/README.md` (Iconography) already specifies a house icon for Home, drawn from Bootstrap Icons (`house`), but also says "No emoji, no illustrations". The Navbar README has no Home item in its examples.
- The repo has no `LICENSE` file and `pom.xml` has an empty `<license/>`.

## 3. Requirements

### Functional
| ID | Requirement |
|----|-------------|
| F1 | `./mvnw clean verify` succeeds with all tests passing. |
| F2 | `GET /` returns 200 `text/html`, rendered by Thymeleaf with the shared layout. No redirect (no 3xx, no `Location`). |
| F3 | The page has one `h2` with the text `Welcome` (class `pc-page-title`), inside `<main>`. |
| F4 | The page shows upstream's `pets.png` as an `<img>` with non-empty, descriptive `alt` text (see 4.3) that scales down to the container width without overflow at 320px. |
| F5 | The navbar lists, in order, "Home" then "Veterinarians". Home links to `/`, has the house icon, and has `aria-current="page"` on the homepage only. Veterinarians has `aria-current="page"` on `/vets.html` only. Exactly one nav item is current on each page. |
| F6 | The brand link leads to `/` (unchanged markup) and so to the homepage. |
| F7 | The page title is `Welcome · Spring PetClinic`. |
| F8 | `/vets.html`, `GET /vets` (JSON) and `/actuator/health` behave exactly as before. No actuator or `management.*` change. |
| F9 | The page is usable at phone width: the navbar collapses via the existing toggle and the image does not cause horizontal scroll. |

### Non-functional and constraints
- Server-side rendering with Thymeleaf in the existing layout; no client-side data fetching, no new JavaScript.
- No new Maven dependency. Config stays in `application.yaml`; no change needed there.
- Design system rules apply: square corners, no shadows or cards around the image, existing fonts, visible focus outlines.
- Read-only: `GET` only.
- Any new token, component or asset is added to `docs/design-system/` in the same PR, not hard-coded in CSS.

## 4. Design

### 4.1 Routing
Replace `RootController` with `WelcomeController` in the same package (`org.springframework.samples.petclinic`), tab-indented:
```java
@Controller
class WelcomeController {

	@GetMapping("/")
	String welcome() {
		return "welcome";
	}

}
```
An explicit controller is used rather than relying on Boot's template welcome-page detection, so the route is visible and testable in a `@WebMvcTest`. Delete `RootController` and `RootRedirectTests`; the new tests are in section 5. `/index.html` is not mapped (see assumptions).

### 4.2 Template `templates/welcome.html`
Same shape as `vetList.html`:
```html
<html lang="en" xmlns:th="https://www.thymeleaf.org"
	  th:replace="~{fragments/layout :: layout('Welcome', 'home', ~{::content})}">
<body>
<th:block th:fragment="content">
	<h2 class="pc-page-title">Welcome</h2>
	<img class="pc-hero" th:src="@{/resources/images/pets.png}" alt="...">
</th:block>
</body>
</html>
```
Set `width` and `height` attributes from the real image dimensions to avoid layout shift.

### 4.3 Image and alt text
- Source: upstream `spring-petclinic` `src/main/resources/static/resources/images/pets.png`, copied unchanged to `src/main/resources/static/resources/images/pets.png`. Same file name and path as upstream.
- Alt text (proposed): "A cat and a dog sitting together". The engineer must look at the actual image and correct this to describe what it shows. Upstream's `<img>` has no `alt`, so this is a deviation.
- `alt=""` is not acceptable: the intent requires meaningful alt text.
- Do not re-encode or edit the file ("as-is"). If it is unusually large, note that in the PR.
- Record the upstream commit the file was taken from in the PR description.

### 4.4 Layout and navbar (`fragments/layout.html`)
- Insert a Home `<li class="nav-item">` before Veterinarians, using the existing `pc-nav-link` markup: `th:href="@{/}"`, `th:aria-current="${activeMenu == 'home'} ? 'page' : null"`, label `<span>Home</span>` (Title Case in markup; CSS uppercases).
- Icon: inline SVG, 16x16, `fill="currentColor"`, `aria-hidden="true"`, `focusable="false"`, using the Bootstrap Icons `house` path, the same way the existing item inlines `list-ul`. No icon font or WebJar is added (see concern 3).
- Layout parameters are unchanged. The `activeMenu` value for the homepage is `'home'`.

### 4.5 CSS and design system docs
- Add `.pc-hero` to `petclinic.css`: `display: block; max-width: 100%; height: auto;` with top margin `space-5`. Only existing spacing tokens. No border, radius or shadow.
- Update `docs/design-system/` in the same PR:
  - `README.md`: Iconography changes "No emoji, no illustrations" to allow the single welcome illustration (`pets.png`) on the homepage, keeping the ban on emoji and any other illustrations. Content notes that the homepage title "Welcome" is the one exception to the plural-noun page title rule.
  - `assets/`: add `pets.png` (for example under an `Illustrations/` group) with a short note on source and licence, and register it in `design-system.json` if that file lists assets.
  - `components/Navbar/README.md`: add Home as the first item and state that it is current on `/`. Update `components/Navbar/preview.html` if it does not already show Home.

### 4.6 Configuration
None.

### 4.7 Out of scope
Owners, pets, visits and their nav entries; intro text, feature links or stats on the homepage; internationalisation; security or login; a custom error page.

## 5. Acceptance and verification

### 5.1 Tests
Replace `RootRedirectTests` with:
- `WelcomeControllerTests` (`@WebMvcTest(WelcomeController.class)`, `MockMvcTester`, Boot 4 imports): `GET /` is 200, view name `welcome`, content type compatible with `text/html`, no `Location` header.
- `WelcomePageHtmlUnitTests` (same style as `VetsPageHtmlUnitTests`: `@SpringBootTest` + `@AutoConfigureMockMvc`, JavaScript disabled):
  - `/` has title `Welcome · Spring PetClinic`, one `h2` reading `Welcome`, and an `img` whose `src` ends in `/resources/images/pets.png` and whose `alt` is non-blank.
  - The image URL itself returns 200 `image/png` (guards against a missing or misplaced asset).
  - Nav items are, in order, `Home`, `Veterinarians`; on `/` only Home has `aria-current="page"`; the brand link `href` is `/`.
- Extend `VetsPageHtmlUnitTests` (or add a test): on `/vets.html` only Veterinarians is current and Home is not.
- Existing vets, JSON and actuator tests stay green unchanged (F8).

Manual check in a real browser (HtmlUnit cannot run Bootstrap's bundle): `./mvnw spring-boot:run`, open `/`. Verify no redirect in the network tab, the house icon and current-state fill, the keyboard focus outline on Home, and at 320px width that the navbar toggle works and nothing scrolls horizontally. `curl -i localhost:8080/` is 200 and `/actuator/health` is `UP`.

## 6. Policy conformance
| Policy | How met |
|--------|---------|
| Design system | Existing tokens, Navbar and Footer; `pc-page-title`; square image, no card or shadow; docs updated in the same PR. |
| Accessibility | Landmarks via the layout, single `h2`, `aria-current` on one item, decorative icon `aria-hidden`, meaningful `alt`, focus outlines unchanged. |
| CLAUDE.md | YAML-only config untouched; actuator untouched; Java 25; Boot 4 test packages; tabs. |
| Security (Snyk, REVIEW.md) | No new dependency, no user input, no new endpoint beyond static `GET /`. |
| Workflow | Change goes through a branch and PR; plan follows after spec review. |

## 7. Areas of concern
1. **Design system says "no illustrations".** The intent resolved that the upstream pets image fits as-is, but the written rule still forbids it. The spec resolves this by amending the rule to allow this one image (4.5). A human must confirm, since this weakens a stated rule rather than only adding to it.
2. **Design-system mirror.** `docs/design-system/` is a versioned mirror of a published artifact. Editing the mirror without updating the source can cause drift. The spec follows the project rule (update in the same PR); the published source should be updated too.
3. **Icons: "use the real Bootstrap Icons" vs. no new dependency.** The design system tells implementers to use real Bootstrap Icons. A `bootstrap-icons` WebJar would add a dependency under the Snyk gate and a whole icon font for two glyphs. The spec instead inlines the `house` SVG path, as `list-ul` already is. Bootstrap Icons are MIT licensed, which is compatible; note source and licence in the PR.
4. **Image licence.** Upstream is Apache 2.0, which the intent treats as compatible. But this repo has no `LICENSE` file and an empty `<license/>` in `pom.xml`, so the project's own licence is not stated, and a repository's licence does not by itself prove the rights to every file in it. The engineer should confirm provenance and add an attribution note. The missing project licence is a separate gap this spec does not fix.
5. **"Welcome" vs. the page title rule.** The design system requires a single plural noun in Title Case; the intent requires "Welcome". Both cannot hold. The intent wins and the docs record the exception (4.5).
6. **Alt text is unverified.** The upstream image was not available when writing this spec, so the proposed alt text is a placeholder to be corrected during implementation.
7. **Upstream differences.** Upstream's welcome page uses a translated message and `img-responsive`. Here the heading is hard-coded English (i18n is out of scope) and the image is made responsive in `petclinic.css`.

## 8. Assumptions
1. Page `<title>` is "Welcome" (matching the heading), not "Home".
2. The layout `activeMenu` key for the homepage is `'home'`.
3. A dedicated `WelcomeController` and `welcome.html` are used instead of Boot's implicit welcome-page support; `RootController` and `RootRedirectTests` are removed rather than adapted.
4. Only `/` serves the page; `/index.html` and `/home` are not mapped.
5. The image is served from `/resources/images/pets.png` (upstream's path), alongside the existing `/resources/css` and `/resources/fonts`.
6. The proposed alt text is "A cat and a dog sitting together", pending a look at the file.
7. The Home icon is an inline SVG of Bootstrap Icons `house`, not a new WebJar.
8. Navbar items stay hand-written in `layout.html`; two items do not justify a data-driven list.
9. The homepage has no content beyond the `Welcome` heading and the image, in line with the intent's out-of-scope list.
10. The image is not `loading="lazy"`, since it is the main content above the fold.
11. The design-system changes in 4.5 are the right scope for "if the image or a Home nav pattern isn't covered".

## 9. Handoff notes for engineering
- Branch from `main`; never push to `main`. Keep `intent/005-homepage/` in sync and record deviations in section 10.
- Suggested order: image and CSS, controller and template, navbar item, tests, design-system docs.
- Boot 4 test imports live in `org.springframework.boot.webmvc.test.autoconfigure.*`; check the generated imports.

## 10. Implementation deviations
None yet.
