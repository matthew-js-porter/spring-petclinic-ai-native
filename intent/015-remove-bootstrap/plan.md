# Plan: Remove Bootstrap and WebJars (intent 015)

## Context
`intent/015-remove-bootstrap/spec.md` removes `org.webjars:bootstrap` and `webjars-locator-lite`, so that every dependency in `pom.xml` is Boot-managed. Bootstrap currently supplies the Reboot reset, `.container`, table, form and button bases, `visually-hidden`, and the navbar collapse (its only JS).

Already done on `main` (commit c8c19b6): `docs/design-system/**` (README, component READMEs, Navbar collapsed state, `bundle.css`, previews, `tokens.json`) is rewritten without Bootstrap. The artifact is republished at `2Dg7pbGjKMze7Y8zaoikw2` and `CLAUDE.md` links to it. So spec C1 is resolved and this PR does not touch `docs/design-system/`. The remaining work is the code and tests, `CLAUDE.md` lines 21 and 31, and `REVIEW.md`.

Owner decision on spec 3.1: add both the `CLAUDE.md` rule and the `[minor]` `REVIEW.md` check.

Branch: `feature/015-remove-bootstrap` → one PR (steps 1 and 2 of spec 5.4 together).

## Class names (fixed by this plan; they match the design-system docs)
| Bootstrap | Replacement |
|---|---|
| `container` | `pc-container` |
| `navbar navbar-expand-lg` + `data-bs-theme` | `pc-navbar` only |
| `navbar-toggler` / `-icon` | `pc-navbar-toggle` + inline SVG |
| `collapse navbar-collapse` | `pc-navbar-menu` (`id="main-navbar"`) |
| `navbar-nav ms-auto`, `nav-item` | `<ul class="pc-nav">`, classless `<li>` |
| `table table-striped` | `pc-table` (already present, drop the others) |
| `table-responsive` | dropped (`pc-table-region` already present) |
| `text-nowrap` | `pc-nowrap` |
| `visually-hidden` | `pc-sr-only` |
| `btn` | dropped (`pc-button` already present) |
| `form-control`, `form-select` | dropped (`pc-input` already present) |
| `form-label` | dropped: a classless `<label>` styled by `.pc-field label`, as the FormField README specifies. This deviates from spec F4's suggested `pc-label`; F4 lets the plan fix the names. |

## Steps

### 1. `petclinic.css` (`src/main/resources/static/resources/css/petclinic.css`)
- Header comment: drop "on top of Bootstrap 5.3".
- Delete the `--bs-*` block (lines 48–56) and every `--bs-*` declaration (`.pc-navbar`, `.pc-navbar .navbar-toggler`, `.pc-table`, `td.pc-muted`, `.pc-button`). Delete rules that only counteracted Bootstrap: `box-shadow: none` on focus, the "restore the system's ring" comment on `.pc-button:focus-visible`, and `.pc-input:focus` colour and background resets where they are now redundant.
- Add a **reset** after `:root` (F5): `*, *::before, *::after { box-sizing: border-box }`, `body { margin: 0; font-family: var(--pc-font-body); font-size: 16px; line-height: 1.5; color: var(--pc-ink); background: var(--pc-surface) }`, `h1–h4, p { margin-top: 0 }`, `p { margin-bottom: 1rem }` (Reboot's values), `table { border-collapse: collapse }`, `img, svg { vertical-align: middle }` with `img { max-width: 100% }`, `button, input, select { font: inherit; margin: 0 }`, `[hidden] { display: none !important }`.
- **Links**: `a { color: var(--pc-spring-green-dark) }` and `a:hover { color: var(--pc-spring-brown) }`, replacing the `--bs-link-*` values.
- **`.pc-container`**: copy the block from `docs/design-system/components/bundle.css` (12px gutters, 540/720/960/1140/1320px max widths).
- **Navbar**:
  - `.pc-navbar-inner` is a flex row with `flex-wrap: wrap`, `align-items: stretch` and `justify-content: space-between`.
  - `.pc-nav` is a reset list in a flex row.
  - `.pc-navbar-toggle` is copied from `bundle.css`. It is `display: none` by default and shown only under `.pc-js` below 992px.
  - Below 992px, `.pc-js .pc-navbar-menu:not(.pc-open) { display: none }`. The open menu takes `flex-basis: 100%` and `.pc-nav` becomes a column.
  - From 992px, `.pc-nav` is a row and the menu gets `margin-left: auto`, whatever the open state.
  - Without `.pc-js`, the menu is always visible and wraps below the brand on a phone.
- **Table**:
  - `.pc-table`: `width: 100%`, `color: var(--pc-ink)`, `vertical-align: top`.
  - Cells get `border-bottom: 1px solid var(--pc-line)`.
  - `tbody tr:nth-child(odd) > *` gets the `--pc-surface-stripe` background.
  - `td.pc-muted` gets a plain `color`.
  - `.pc-table-region { overflow-x: auto }`.
  - `th { text-align: left }`.
- **Form**: replace `.pc-field .form-label` with `.pc-field label`. `.pc-input` adds `display: block; width: 100%; border-radius: 0; appearance: none`. `select.pc-input` adds a CSS `background-image` chevron (an inline SVG data URI, stroke `#34302d`) at `right 16px center`, with `background-size: 16px 12px` and `no-repeat`.
- **Button**: `.pc-button` gets plain declarations taken from the removed `--bs-btn-*` values:
  - inline-block, padding 8px 16px;
  - display font at 14px/24px, weight 600;
  - `on-dark` text on a `spring-green-dark` fill with a 1px border in the fill colour;
  - `cursor: pointer`, `border-radius: 0`, `text-decoration: none`;
  - `:hover` and `:active` switch to `spring-brown`.

  Keep the focus outline.
- Add `.pc-sr-only` (the clip technique from `bundle.css`) and `.pc-nowrap { white-space: nowrap }`.
- Compare computed styles (spec 5.3) while Bootstrap is still linked, then again after removing it.

### 2. Templates (`src/main/resources/templates/**`)
- Apply the class swaps from the table: `vets/vetList.html`, `owners/{ownerDetails,ownerForm,ownersList,petForm,visitForm,visitList}.html`.
- `fragments/layout.html`:
  - Remove the Bootstrap `<link>` and `<script>`.
  - Replace the navbar with the sketch in spec 5.2: `<div class="pc-container pc-navbar-inner">`, the brand link, the toggle button with an inline three-line SVG (from the Navbar preview: `aria-hidden`, `focusable="false"`, `currentColor` stroke), and the menu with its `ul.pc-nav`.
  - Make `main` and the footer use `pc-container`.
  - Add `<script th:src="@{/resources/js/navbar.js}"></script>` as the last element of `<body>`.
- Afterwards, `grep -rnE 'navbar-|nav-item|collapse|"container|ms-auto|btn|form-(label|control|select)|table-striped|table-responsive|text-nowrap|visually-hidden|data-bs-' src/main/resources/templates` should find nothing.

### 3. `src/main/resources/static/resources/js/navbar.js` (new, about 25 lines)
It is an IIFE written to an ES level HtmlUnit 4.21 runs: `var`, function expressions and `classList`, with no arrow functions or optional chaining (the comment says why).
- Adds `pc-js` to `document.documentElement`.
- Finds `.pc-navbar-toggle` and the panel from `aria-controls`, and returns early if either is missing.
- On click, reads `aria-expanded`, flips it, and toggles `pc-open` on the panel.
- On a document `keydown` with `key === 'Escape' || key === 'Esc'` while the menu is open, it closes the menu and focuses the toggle.
- There is no resize handler, because CSS keeps the desktop layout independent of the state.

### 4. `pom.xml`
Delete `webjars-bootstrap.version` and both `org.webjars` dependencies. Leave `tomcat.version` and `jackson-bom.version` alone (spec C4).

### 5. Tests (`src/test/java/...`)
- `vet/VetsPageHtmlUnitTests.loadsItsStylesheetsAndScripts`: assert `containsExactly("/resources/css/petclinic.css", "/resources/js/navbar.js")`, keep the check that each returns 200, and change `.nav-item a[href='/']` to `.pc-nav a[href='/']`.
- Remove the `disableJavaScript` `@BeforeEach` and its Bootstrap comment from `WelcomePage`, `OwnersPage`, `OwnerDetailsPage`, `OwnerFormPage`, `PetFormPage`, `VisitFormPage` and `VetsPage` HtmlUnit tests, so JavaScript is on by default. In `ErrorPageIntegrationTests`, drop the JS-disable line and its comment.
- Update class selectors and expected HTML:
  - `WelcomePageHtmlUnitTests:62` and `OwnersPageHtmlUnitTests:231`: `.nav-item a` becomes `.pc-nav a`.
  - `OwnerDetailsPageHtmlUnitTests:112`: `.visually-hidden` becomes `.pc-sr-only`.
  - `OwnerFormPageHtmlUnitTests:179`: `a.btn` becomes `a.pc-button`.
  - `OwnerControllerTests` (lines 76, 156–159, 340–341, 371, 398–399) and `VisitControllerTests:92`: update the expected markup strings (`pc-sr-only`, `pc-nowrap`, classless label, `class="pc-button"`, `class="pc-input"`).
- New `NavbarHtmlUnitTests` (`@SpringBootTest @AutoConfigureMockMvc`, injected `WebClient`, JS on):
  - On `/vets.html`, `<html>` has `pc-js`.
  - The toggle has `aria-controls="main-navbar"` and `aria-expanded="false"`.
  - Clicking the toggle sets it to `true` and adds `pc-open` to `#main-navbar`; clicking again sets `false`.
  - Opening, then `toggle.type(KeyboardEvent.DOM_VK_ESCAPE)`, gives `false`, and the focused element is the toggle. If HtmlUnit doesn't set `key` that way, fall back to dispatching `new KeyboardEvent('keydown', {key: 'Escape'})` via `page.executeJavaScript` and say so in a comment.

  Following spec F7, no test asserts that Bootstrap is absent.

### 6. Docs and policy
- `CLAUDE.md`:
  - Line 21: "…`petclinic.css` implements it as a hand-written stylesheet, with `static/resources/js/navbar.js` for the collapsing menu; no CSS framework or WebJars."
  - Line 31: "Pages are Thymeleaf, styled by a hand-written `static/resources/css/petclinic.css` that maps the tokens onto `pc-*` classes, plus `navbar.js` for the menu toggle. No CSS framework, no WebJars, no SCSS build."
  - Project section: "Favour dependencies whose versions Spring Boot manages. A non-Boot dependency is allowed only with a stated reason in its spec."
- `REVIEW.md:16`: replace "never properties Boot does not define such as `webjars-bootstrap.version`" with "never properties Boot does not define". Add a Conformance bullet: a `pom.xml` diff that adds a `<dependency>` or plugin with an explicit `<version>`, or one not managed by the Boot parent or BOM, is `[minor]` unless the linked `spec.md` states a reason.
- `spec.md`: note the deviations:
  - classless `<label>` instead of `pc-label`;
  - the design-system docs and artifact were already updated in c8c19b6 (artifact `2Dg7pbGjKMze7Y8zaoikw2`; F8 and C1 cite the old URL);
  - the 3.1 decision is confirmed.

  `plan.md` is saved next to the spec (hook).

## Verification
- `sdk env && ./mvnw clean verify` passes (A10, F7).
- `./mvnw dependency:tree | grep webjars` finds nothing, and `grep webjars pom.xml` finds nothing (A1).
- Run the template grep from step 2 (A4), and `grep -rni bootstrap CLAUDE.md docs src/main` finds only the icon-provenance line (A11).
- `./mvnw spring-boot:run`:
  - `curl -i localhost:8080/webjars/bootstrap/css/bootstrap.min.css` returns 404 (A3).
  - Use the `run` or Chrome skill to open each page in the A5 list at 1280px and 375px. Check that the network panel shows only `/resources/**` (A2). Compare against the `components/*/preview.html` files and take before/after screenshots for the PR.
  - Check the navbar at 375, 991 and 992px; the toggle with Tab, Enter, Space and Escape; and with JS disabled, that the links are reachable (A6–A8).
- The PR description includes the A5 checklist and screenshots, and the VoiceOver check (A7) is left to the human reviewer.
