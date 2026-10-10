# Plan: Intent 005 — Homepage

## Context
`GET /` currently redirects to `/vets.html` (`RootController`), so the app has no landing page and no "Home". `intent/005-homepage/spec.md` replaces the redirect with a Thymeleaf welcome page (a "Welcome" `h2` and the author's dog photo), adds a Home nav item before Veterinarians, and records the photo and Home item in `docs/design-system/`. Deviations D1–D4 (own photo, resized, 360px `hero-width` cap, photo exception) are already in spec §10. The visual reference is `intent/005-homepage/PetClinic Homepage.html` (uses `.pc-hero { display:block; width:var(--pc-hero-width); max-width:100%; height:auto; margin-top:24px }`).

Work on branch `feature/005-homepage`, land by PR. Never push to `main`. Save this plan as `intent/005-homepage/plan.md`.

Already on `main`: the 10.3 MB 3024x4032 `src/main/resources/static/resources/images/pets.png` (commit 77de44d). It stays in git history (rewriting `main` is out of scope); this PR replaces it with the web version.

## 1. Image (`static/resources/images/pets.png`)
- Resize in place to 720x960 with macOS `sips` (`sips --resampleWidth 720 pets.png`), keeping PNG and the filename. Strip metadata (`sips -d` on any EXIF/GPS properties; check with `sips -g all`).
- Check the size. A photo PNG at 720x960 is likely ~0.8–1.2 MB; if over ~500 KB, quantize with `pngquant` (via `brew install pngquant`, a local tool, not a project dependency) and eyeball the result. Record the final dimensions/size in the PR.
- Copy the result to `docs/design-system/assets/Photos/pets.png`.

## 2. CSS (`static/resources/css/petclinic.css`)
- Add `--pc-hero-width: 360px;` to the `:root` token block (next to the other tokens, with a size-token comment).
- Add after `.pc-muted`:
  ```css
  .pc-hero {
  	display: block;
  	width: var(--pc-hero-width);
  	max-width: 100%;
  	height: auto;
  	margin-top: 24px;
  }
  ```
  (literal `24px` = `space-5`, matching how the file writes spacing elsewhere.)

## 3. Controller and template
- Delete `src/main/java/.../petclinic/RootController.java`; add `WelcomeController.java` exactly as spec §4.1 (package-private, `@GetMapping("/")` → `"welcome"`, short Javadoc in the style of the old class).
- Add `src/main/resources/templates/welcome.html` per spec §4.2: `layout('Welcome', 'home', ~{::content})`, `h2.pc-page-title` "Welcome", `<img class="pc-hero" th:src="@{/resources/images/pets.png}" width="720" height="960" alt="A small grey and white long-haired dog lying on its back in a fluffy cream bed, hugging a brown and white stuffed toy dog">` (width/height from the resized file; no `loading="lazy"`).

## 4. Navbar (`templates/fragments/layout.html`)
- Insert a Home `<li class="nav-item">` before Veterinarians, copying the existing item's markup: `th:href="@{/}"`, `th:aria-current="${activeMenu == 'home'} ? 'page' : null"`, inline 16x16 SVG (`fill="currentColor" aria-hidden="true" focusable="false"`) with the Bootstrap Icons `house` path, `<span>Home</span>`.

## 5. Tests
- Delete `RootRedirectTests`; add `WelcomeControllerTests` (`@WebMvcTest(WelcomeController.class)`, `MockMvcTester`, `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`): `/` is 200, view name `welcome`, content type compatible with `text/html`, no `Location` header. Note: `@WebMvcTest` renders the template, so the slice needs Thymeleaf auto-config (it's included in the WebMvc slice).
- Add `WelcomePageHtmlUnitTests` mirroring `vet/VetsPageHtmlUnitTests` (`@SpringBootTest`, `@AutoConfigureMockMvc`, JS disabled in `@BeforeEach`):
  - title `Welcome · Spring PetClinic`; exactly one `h2`, inside `main`, text `Welcome`; one `img` with `src` ending `/resources/images/pets.png` and non-blank `alt`.
  - the image URL returns 200 with content type `image/png`.
  - `nav[aria-label='Main'] .nav-item a` texts are `Home`, `Veterinarians` in order; only Home has `aria-current="page"`; brand link `href` is `/`.
- Update `VetsPageHtmlUnitTests.navigatesFromTheNavbar`: it currently asserts the brand link lands on `/vets.html` — change to assert it lands on `/` with title `Welcome · Spring PetClinic`, and add that the Home link has no `aria-current` (only Veterinarians is current).
- `VetControllerTests`, `VetsIntegrationTests`, `VetRepositoryTests`, `PetClinicApplicationTests` unchanged.

## 6. Design-system docs (`docs/design-system/`)
- `tokens.json`: add `{"name": "hero-width", "value": "360px", "usage": "..."}` to `size.tokens`.
- `README.md`: Iconography — replace "No emoji, no illustrations." with a rule allowing the single welcome photograph (`pets.png`, homepage only, `.pc-hero`, no border/radius/shadow, meaningful `alt`) and banning emoji, illustrations and other photos. Content — note "Welcome" as the one exception to the plural-noun page title rule. Mention `hero-width` in the size/layout notes.
- `assets/Photos/pets.png` + `assets/Photos/README.md` (author's own photo, use only on the homepage, alt text).
- `design-system.json`: add `"Photos"` to `groups` and an `assetGroups.Photos` entry for `pets.png` (name, size in bytes, `image/png`; `blob` following the Logos entries' MD5-style id — compute `md5` of the file); update `lastChange`.
- `components/Navbar/README.md`: Home is the first item (house icon), current on `/`; Veterinarians current on `/vets.html`. Check `components/Navbar/preview.html` and add a Home item if missing.

## 7. Spec sync
- Add any new deviation to spec §10 (e.g. final image size if pngquant isn't used, or `sips` differences). Otherwise the spec stands.

## Verification
- `sdk env` then `./mvnw clean verify` — all tests green (F1).
- Run `spring-tools:validate` for diagnostics.
- `./mvnw spring-boot:run`; `curl -i localhost:8080/` → 200 `text/html`, no `Location`; `curl -I localhost:8080/resources/images/pets.png` → 200 `image/png`, expected size; `curl localhost:8080/actuator/health` → `UP`; `curl localhost:8080/vets` JSON unchanged.
- Real browser (via the run skill / Chrome): `/` shows heading and photo capped at 360px, house icon, Home filled as current, focus outline on Home via keyboard; `/vets.html` shows Veterinarians current; at 320px width the toggle works and there is no horizontal scroll.
- Commit, push the branch, open a PR noting: author's own photo, Bootstrap Icons `house` path (MIT), the design-system rule change needing human confirmation (spec concern 1), and that the published design-system artifact should be updated to match the mirror (concern 2).
