# Spec: Remove Bootstrap and WebJars
Source intent: [intent.md](./intent.md)
Author: matthew-js-porter

## 1. Summary
Remove the two WebJars (`org.webjars:bootstrap` and `webjars-locator-lite`) and everything the app takes from Bootstrap, so `pom.xml` has no dependency outside Spring Boot's management and no page loads Bootstrap CSS or JS. The work has four parts:

1. **Stylesheet.** `petclinic.css` becomes self-contained. It gains a small reset (replacing Bootstrap's Reboot), a `pc-container`, the table, form, button and visually-hidden rules it used to inherit, and a first-party navbar collapse layout. All values still come from `docs/design-system/tokens.json`.
2. **Navbar toggle.** The only Bootstrap behaviour in use, the collapsing menu, is replaced by one small first-party script (about 25 lines, no library) served from `static/resources/js/`. Without JavaScript the menu stays open, so navigation never depends on the script.
3. **Templates.** The ~20 Bootstrap classes are replaced by `pc-*` classes; the Bootstrap `<link>` and `<script>` are removed from `layout.html`.
4. **Docs and policy.** `CLAUDE.md`, `docs/design-system/` and its published artifact stop describing the UI as built on Bootstrap. `CLAUDE.md` and `REVIEW.md` record the standing preference for Spring Boot-managed dependencies.

Out of scope (from the intent): visual redesign, new components or pages, back-end changes, dark mode.

## 2. Existing codebase context
- `pom.xml`: `<webjars-bootstrap.version>5.3.8</webjars-bootstrap.version>`, `org.webjars:bootstrap` and `org.webjars:webjars-locator-lite` (the latter unversioned: Boot manages its version, but nothing else needs it). The remaining dependencies are Boot starters, `h2`, `htmlunit` (test) and the Boot plugin, all Boot-managed. `tomcat.version` (11.0.26) and `jackson-bom.version` (3.1.7) are explicit overrides of properties Boot defines (see C4).
- `templates/fragments/layout.html` loads `/webjars/bootstrap/css/bootstrap.min.css` (version-less, resolved by the locator) and `/webjars/bootstrap/js/bootstrap.bundle.min.js`, and uses `navbar navbar-expand-lg`, `navbar-toggler` (`data-bs-toggle="collapse"`), `navbar-collapse collapse`, `navbar-nav ms-auto`, `nav-item`, `container` and `data-bs-theme="dark"`.
- Other templates use: `visually-hidden` (6, table captions and link suffixes), `form-label` (5), `form-control` (4), `form-select` (1), `btn` (4), `table table-striped` (4), `table-responsive` (2), `text-nowrap` (2), `container` (3). Inventory by grep; the plan re-runs it.
- `petclinic.css` (520 lines) overrides Bootstrap through `--bs-*` variables (body, link, border radius, table, navbar, button), restyles `.navbar-toggler`, and relies on Bootstrap for: Reboot (box-sizing, margin and heading resets, `table` border-collapse, `img` sizing, form-control font inheritance), the `.container` widths and gutters, `.table-striped`, `.table-responsive` overflow, the `form-select` chevron, `.btn` base, the toggler icon and the `collapse` behaviour.
- Tests: all page tests (`*HtmlUnitTests`, `ErrorPageIntegrationTests`) disable HtmlUnit JavaScript with a comment that its engine cannot parse Bootstrap's bundle. `VetsPageHtmlUnitTests.loadsItsStylesheetsAndScripts` asserts three assets: `petclinic.css` and the two WebJar paths.
- `docs/design-system/` mentions Bootstrap in `README.md` (container, `--bs-border-radius`, spacing scale, `sm` breakpoint, icon provenance), `tokens.json` (`surface-stripe` usage), and the Navbar, Button, DataTable, FormField and Pagination READMEs. `components/*/preview.html` and `bundle.css` carry no Bootstrap reference. `design-system.json` blob ids belong to the published artifact.
- `REVIEW.md`'s Spring Boot overrides rule names `webjars-bootstrap.version` as an example of a property Boot does not define; that example disappears with the property.
- Renovate (`renovate.json5`) and CI workflows have no Bootstrap-specific entries. No CSP header is configured.

## 3. Decisions on the open questions

### 3.1 Where the "favour Boot-managed dependencies" preference lives: `CLAUDE.md` rule plus a `REVIEW.md` check
Recommended and specified here, pending the owner's confirmation (the intent assigns this question to matthew-js-porter; see Assumptions):
- `CLAUDE.md` (Project section): "Favour dependencies whose versions Spring Boot manages. A non-Boot dependency is allowed only with a stated reason in its spec."
- `REVIEW.md` (Conformance): flag a `pom.xml` diff that adds a `<dependency>` or plugin carrying an explicit `<version>` (or otherwise not managed by the Boot parent or BOM) unless the linked `spec.md` states a reason. Severity `[minor]`, since reviews are advisory and the dependency is allowed with a reason. A `CLAUDE.md` line alone is read by agents but not checked on human PRs; the review rule makes the preference visible at the point of change at the cost of one rule line. Not a build-time enforcer: no Maven Enforcer rule or test is added, because the preference is "not forbidden", and a hard gate would need an allow-list to maintain.

### 3.2 How page conformance is proven: automated structural tests plus a recorded manual check (not screenshot diffs)
- **Automated (CI):** page tests assert the new assets, the navbar's accessible structure and, with JavaScript enabled, the toggle behaviour (F6, F7).
- **Absence of Bootstrap and WebJars is verified manually, not by tests:** `./mvnw dependency:tree` shows no `org.webjars`, and greps of `pom.xml`, `templates/` and `static/` find no Bootstrap reference (A1-A4, A11). No test asserts that Bootstrap is absent.
- **Manual (in the PR):** the implementer opens every page at 1280px and 375px next to the matching `docs/design-system/components/*/preview.html` and ticks the checklist in section 6, attaching before/after screenshots to the PR for reviewer comparison.
- **Screenshot-diff automation is rejected:** it needs a headless browser and baseline images, a new test dependency that conflicts with the intent's goal, and the intent accepts small browser-default differences, which would make a pixel diff noisy.

### 3.3 Navbar collapse mechanism: one first-party script, progressively enhanced
| Option | Keyboard | Announces expanded state | Works without JS | Verdict |
|--------|----------|--------------------------|------------------|---------|
| Checkbox/label CSS hack | Poor (label is not focusable by default) | No (no `aria-expanded`) | Yes | Rejected: fails the a11y requirement |
| `<details>`/`<summary>` | Yes | Yes (native) | Yes | Rejected: forcing it open on desktop relies on `::details-content`, not yet baseline across browsers, and it changes the `<nav>` semantics |
| Popover API (`popovertarget`) | Yes | Partly, varies by browser | Hidden menu without JS | Rejected: the same desktop-always-visible problem, and weaker assistive-tech support |
| **Small first-party script on a `<button aria-expanded aria-controls>`** | Yes (native button) | Yes (script toggles `aria-expanded`) | Menu visible (see below) | **Chosen** |

The intent forbids new dependencies and CDNs, not first-party code, so a script file is allowed.

## 4. Requirements

### Functional
- **F1.** `pom.xml` lists no `org.webjars` dependency and no `webjars-bootstrap.version` property. No dependency is added. After the change every `<dependency>` and plugin version comes from Boot (the two Boot-property overrides are covered in C4).
- **F2.** No page, at any URL (home, `/owners.html` search and list, owner details, owner/pet/visit forms, `/vets.html`, error page including 404 and 500), references an asset under `/webjars/`, a Bootstrap file or any external host. A request to `/webjars/**` returns 404.
- **F3.** `layout.html` loads only `/resources/css/petclinic.css` and, at the end of `<body>`, `/resources/js/navbar.js`. The script is `defer`red or placed last, has no inline code and needs no `th:` expression.
- **F4.** No template carries a Bootstrap class (`navbar*`, `nav-item`, `collapse`, `container`, `ms-auto`, `btn`, `form-*`, `table`, `table-striped`, `table-responsive`, `text-nowrap`, `visually-hidden`) or a `data-bs-*` attribute. Replacements are first-party `pc-*` classes (names fixed in the plan): `pc-container`, `pc-navbar-toggle`, `pc-nav`, `pc-button`, `pc-label`, `pc-input` (already exists), `pc-table`, `pc-table-region`, `pc-nowrap`, `pc-sr-only`. Existing `pc-*` classes keep their meaning.
- **F5.** `petclinic.css` has no `--bs-*` variable, no `.navbar-*`/`.btn`/`.form-*`/`.table-*` selector and no Bootstrap mention in comments. It provides:
  - a minimal reset: `box-sizing: border-box` on all elements; body margin 0, base `font-size` 16px and `line-height` 1.5 (matching Bootstrap's body values the pages were designed on); heading and `p` margins as the pages expect; `table { border-collapse: collapse }`; `img { max-width: 100%; vertical-align: middle }`; `button, input, select { font: inherit }`; `[hidden] { display: none }`;
  - `.pc-container`: full width with 12px gutters (the value `.container` used), `margin: auto`, and `max-width` 540px from 576px, 720px from 768px, 960px from 992px, 1140px from 1200px, 1320px from 1400px. These are the breakpoints the design system and `docs/design-system/README.md` already cite (`sm` 576px, `md` 768px, `lg` 992px);
  - table styling: collapsed borders, a 1px `line` row separator, warm-grey striping on odd body rows via `tbody tr:nth-child(odd)`, text colour `ink`, `.pc-table-region { overflow-x: auto }`;
  - `.pc-button`: inline-block, the system's padding/type/colours, `cursor: pointer`, a 1px border in the fill colour, hover and active in `spring-brown`, 2px `focus-ring` outline with 2px offset;
  - `.pc-input` and its select and date variants: `width: 100%`, `display: block`, `appearance` reset for select with an own chevron (inline SVG data-URI in CSS, `ink` colour, `space-4` from the right edge as FormField specifies, not a runtime request);
  - `.pc-sr-only` (visually hidden but readable by assistive technology, the standard clip technique) and `.pc-nowrap`.
- **F6.** Navbar markup and behaviour:
  - Markup keeps `<header>`, `<nav aria-label="Main">`, the brand link, the three nav items with icons, and `aria-current="page"` on exactly the current item.
  - The toggle is `<button type="button" class="pc-navbar-toggle" aria-controls="main-navbar" aria-expanded="false" aria-label="Toggle navigation">` with an inline SVG icon (`aria-hidden`, `focusable="false"`, `currentColor`) replacing Bootstrap's background-image icon.
  - Width `>= 992px` (the `lg` breakpoint the system documents): toggle hidden, items in a row, right-aligned (replaces `ms-auto`).
  - Below 992px: toggle visible; the menu panel (`#main-navbar`) is hidden when collapsed and shown as a vertical stack, full width, when open. Items keep the `navbar-height` (64px) tap target and `spring-green-dark` hover/active fill.
  - `navbar.js` (plain ES2015+, no dependencies): on click, flips `aria-expanded` on the button and the `pc-open` class (or `hidden` attribute) on the panel. `Escape` while the menu is open closes it and returns focus to the button. Resizing to `>= 992px` leaves the desktop layout unaffected whatever the state. Space/Enter work because it is a native `<button>`.
  - **No-JS fallback:** the collapsed state is applied only when the script has run (it adds a `pc-js` class to `<html>` or sets the initial `hidden` state itself). With JavaScript off, the toggle is hidden and the menu is always visible, so all pages stay navigable.
  - Focus outlines: `focus-ring-on-dark`, 2px inset, on the toggle and nav links, as today.
- **F7.** Tests (Boot 4 packages per `CLAUDE.md`):
  - `VetsPageHtmlUnitTests.loadsItsStylesheetsAndScripts` asserts exactly `/resources/css/petclinic.css` and `/resources/js/navbar.js`, each returning 200, and the test name stays accurate.
  - A navbar test with JavaScript enabled: clicking the toggle changes `aria-expanded` `false` to `true` and back, and `Escape` closes. If HtmlUnit's engine cannot run the script, `navbar.js` is written to the ES level it supports and the test notes it; if that is not practical the toggle is tested in a real browser and the reason is documented in the test (see C3).
  - The JavaScript-disabled `@BeforeEach` comments and calls in the page tests are removed where the reason (the Bootstrap bundle) no longer holds; tests that only need static markup may keep JavaScript disabled with an accurate comment.
  - No test asserts the absence of Bootstrap or WebJars; that is checked manually (A1-A4, A11).
- **F8.** Documentation:
  - `CLAUDE.md`: line 21 and the "Pages are Thymeleaf on Bootstrap 5.3 (WebJar)" bullet are rewritten (Thymeleaf pages styled by hand-written `petclinic.css`, plus `navbar.js` for the menu; no CSS framework, no WebJars); the dependency-preference rule from 3.1 is added; the "Key rules" and accessibility bullets stay.
  - `docs/design-system/`: `README.md` (container, border radius, spacing scale, breakpoints and icon provenance reworded: "Bootstrap's `.container`" becomes the `pc-container` rule with its widths; "Set Bootstrap's `--bs-border-radius` to 0" is dropped since there is no framework default to override; the spacing scale and breakpoints are written as the system's own values), the Navbar, Button, DataTable, FormField and Pagination READMEs (the "In the Thymeleaf app this is Bootstrap's ..." sentences replaced by the `pc-*` classes), and `tokens.json` (`surface-stripe` usage loses "not Bootstrap's cool default"). The Navbar README gains the collapsed behaviour (toggle, `aria-expanded`, Escape, no-JS fallback). If `bundle.css` or a `preview.html` needs the same `pc-*` rules to stay faithful, they are updated in step.
  - `REVIEW.md`: the example of a non-Boot property ("such as `webjars-bootstrap.version`") is replaced by wording that needs no example, and the new dependency rule from 3.1 is added.
  - The published artifact (https://claude.ai/artifact/7i5ZiTpYfptjDK4MKvD7XP) is republished with the updated docs, and `design-system.json` blob ids updated only if asset files change (they should not). See C1.

### Non-functional / constraints
- **N1. Accessibility does not regress:** landmarks, hidden `<caption>` and `<th scope="col">`, `aria-current`, `aria-label` on pagination, visible 2px focus outlines on every interactive element (links, buttons, inputs, the focusable scroll region), colour contrast unchanged (token values only), and the toggle keeps today's target size (the navbar's 64px height, at least 40px wide). WCAG 2.2 AA is the bar.
- **N2. Design system rules hold:** square corners everywhere, no shadows, gradients or cards, `spring-green` decorative only, Title Case in markup and uppercase via CSS, empty values as `none`. Browser-default differences (form control rendering, date picker, default heading margins) are acceptable if these rules hold.
- **N3. No new dependency, CDN or third-party runtime request.** Fonts stay self-hosted. The chevron and toggle icons are inline (CSS data-URI or inline SVG).
- **N4. No CSP regression:** the script is an external same-origin file with no inline handlers, so a future `script-src 'self'` policy would work.
- **N5. Size:** total CSS+JS transferred per page drops (Bootstrap CSS ~230 kB and JS ~80 kB minified are removed); no build step is added (still no SCSS or bundler).
- **N6. Repo rules:** work on a branch and PR, `./mvnw clean verify` and CI green, config YAML only, comments describe code and never cite intents, Spring Data JDBC and Boot 4 notes unaffected.

## 5. Design

### 5.1 Layout of the change
| File | Change |
|------|--------|
| `pom.xml` | Delete the property and both WebJar dependencies |
| `templates/fragments/layout.html` | Drop the Bootstrap `<link>`/`<script>`, `data-bs-theme`, collapse attributes; new navbar markup (5.2); load `navbar.js` |
| `templates/**/*.html` | Class swaps per F4; no structural or text changes |
| `static/resources/css/petclinic.css` | Header comment, remove `--bs-*` block, add reset and the F5 rules, rename selectors scoped to Bootstrap classes |
| `static/resources/js/navbar.js` | New |
| `src/test/**` | F7 |
| `CLAUDE.md`, `REVIEW.md`, `docs/design-system/**` | F8 |

### 5.2 Navbar sketch
```html
<nav class="pc-navbar" aria-label="Main">
  <div class="pc-container pc-navbar-inner">
    <a class="pc-brand" ...>...</a>
    <button class="pc-navbar-toggle" type="button" aria-controls="main-navbar" aria-expanded="false" aria-label="Toggle navigation">
      <svg ... aria-hidden="true" focusable="false">...</svg>
    </button>
    <div class="pc-navbar-menu" id="main-navbar">
      <ul class="pc-nav">
        <li><a class="pc-nav-link" ...>...</a></li>
      </ul>
    </div>
  </div>
</nav>
```
The `aria-label` stays "Toggle navigation" for continuity with the current page; the button's name does not change with state (the state is carried by `aria-expanded`, avoiding a double announcement). `navbar.js` sketch: select `.pc-navbar-toggle`, add `pc-js` to `document.documentElement`, toggle `aria-expanded` and the panel's open class on click, close on `Escape`. Focus leaving the menu does not close it, so tabbing through an open menu works.

### 5.3 CSS strategy
Take the computed values Bootstrap supplied today and write them as plain declarations or `--pc-*` variables, rather than re-deriving them from memory: the implementer diffs computed styles (browser dev tools) for `body`, `h1-h4`, `p`, `a`, `table`, `th/td`, `label`, `input`, `select`, `button` before and after, per page. Keep selectors flat (class-based), and keep the existing tokens block. Delete rules that only counteracted Bootstrap (for example restoring the button focus ring, `box-shadow: none` on focus where no shadow is now set) rather than leaving dead code.

### 5.4 Order of work (for the plan)
1. Add the reset and `pc-*` rules to `petclinic.css` while Bootstrap is still loaded and swap template classes page by page, checking each page; 2. replace the navbar and add `navbar.js`; 3. remove the Bootstrap `<link>`/`<script>` and the WebJars from `pom.xml`; 4. update tests; 5. update docs, `REVIEW.md`, publish the artifact. Steps 1 and 2 can go in the same PR; the diff is large but mechanical.

## 6. Acceptance and verification
| # | Check | How |
|---|-------|-----|
| A1 | `pom.xml` has no `org.webjars` or `webjars-bootstrap.version` | Manual: `./mvnw dependency:tree` shows no `org.webjars`, and `pom.xml` has no `webjars-bootstrap.version` |
| A2 | No page requests Bootstrap or an external host | Manual: browser network panel shows only same-origin `/resources/**` on each page |
| A3 | `/webjars/**` returns 404 | Manual: `curl -i localhost:8080/webjars/bootstrap/css/bootstrap.min.css` |
| A4 | No Bootstrap class or `data-bs-*` in any template | Manual: `grep -rn` of `templates/` for the F4 classes and `data-bs-` |
| A5 | Pages conform at 1280px and 375px | Manual checklist in the PR: home, owner search with results and with none, owner details with pets and visits, add/edit owner (with errors), add/edit pet, add visit, vets page 1 and 2, 404 page. Compare with `components/*/preview.html` for square corners, no shadows, stripe colour, type, spacing, focus rings. Tables wider than the viewport scroll inside their region, not the page |
| A6 | Navbar collapses at 375px and 991px, is a row at 992px | Manual plus F7 test |
| A7 | Toggle works by keyboard (Tab, Enter, Space, Escape) and announces state | Manual with a screen reader (VoiceOver or NVDA: "collapsed"/"expanded"), plus the test on `aria-expanded` |
| A8 | With JavaScript disabled, nav links are reachable at 375px | Manual |
| A9 | Accessibility behaviour unchanged | Existing page tests for captions, scopes, `aria-current`, pagination labels pass unchanged; manual tab-through shows a 2px outline on every control |
| A10 | `./mvnw clean verify` passes | CI `build` |
| A11 | `CLAUDE.md`, `docs/design-system/` describe no Bootstrap dependency; `grep -rni bootstrap` over `CLAUDE.md`, `docs/`, `src/main/` returns nothing except the allowed icon-provenance note (C5); `src/test/` needs no exception because no test mentions the word once the `VetsPageHtmlUnitTests` asset assertion is updated | grep |
| A12 | The preference is recorded in `CLAUDE.md` and `REVIEW.md` | Review |
| A13 | Snyk and Renovate | After merge, Snyk Open Source no longer lists the WebJar; Renovate no longer opens Bootstrap PRs |

## 7. Policy conformance
- **Design system:** the system is the visual reference; this spec adds no colour, type or spacing values. Container widths, reset values and the breakpoints are the ones the system already cites, now recorded in `docs/design-system/` as the system's own (F8). Any token or component found missing during implementation (for example the toggle icon) is added to the docs in the same PR, per the constraint.
- **Brand:** logo, wordmark, Spring green rules, Title Case and fonts untouched.
- **Security (Snyk, `/security-review`):** removing a third-party front-end library shrinks the attack surface and the Snyk scope. The new script is same-origin, has no `innerHTML`, no network access and no user data. No CDN. `CLAUDE.md`'s rule against SHA-pinning and the YAML-only rule are unaffected.
- **UX/accessibility:** see N1; no regression is accepted.
- **Workflow:** branch and PR, never `main`; `build` required to pass.

## 8. Areas of concern
- **C1. Published design-system artifact.** `CLAUDE.md` requires the repo mirror and the artifact at https://claude.ai/artifact/7i5ZiTpYfptjDK4MKvD7XP to stay in step. An unattended CI run or an agent without artifact access cannot republish it. The PR that edits `docs/design-system/` must not merge until a human republishes (or the design-sync tool is run by someone with access), and the PR description should say so. If the artifact is republished first, `design-system.json` metadata may need a matching update.
- **C2. "Pixel-identical not required" versus "conforms to previews".** Removing Reboot changes default margins, line-heights and form-control rendering. Mitigation: compute-style diffing (5.3) and the manual checklist; a reviewer judges what counts as a "small browser-default difference". Residual risk: subtle spacing drift on pages not on the checklist.
- **C3. Tests cannot fully cover the toggle.** HtmlUnit's JavaScript engine is limited (the stated reason JavaScript is disabled today). If it cannot run `navbar.js`, the collapse behaviour is verified only manually, and a future regression would not fail CI. Options if that is unacceptable: a browser-based test (needs a new test dependency such as Playwright or Selenium, which conflicts with the dependency preference and needs a stated reason in its own spec). This spec chooses manual verification plus HtmlUnit where possible.
- **C4. "Every remaining dependency's version comes from Spring Boot" versus the Boot-property pins.** `tomcat.version` and `jackson-bom.version` in `pom.xml` override Boot's own versions (apparently security pins; no comment explains them). Their artifacts are Boot-managed, but the version values are not Boot's. This spec treats them as out of scope (intent 014 handles their clean-up on a Boot upgrade). If the owner reads the observable result strictly, they must be removed or justified in a follow-up; removing them here would be a back-end behaviour change the intent excludes.
- **C5. Bootstrap Icons.** The three nav icons are inline SVG paths taken from Bootstrap Icons (MIT), mentioned in `docs/design-system/README.md`. They are not a dependency, and the MIT notice does not require attribution in the markup for a few paths, but the "no Bootstrap" grep (A11) would match the provenance note. Decision: keep the icons and keep a single factual provenance sentence in the design-system README, listed as an allowed exception in A11. Alternative: redraw the icons, which is a visual change outside scope.
- **C6. Dropping `webjars-locator-lite` changes Boot's resource handling.** With the locator absent, Boot's `/webjars/**` mapping serves nothing; any hard-coded `/webjars/...` link (none found) would 404. With the JARs absent there is no content to serve; no handler configuration is needed. Verified manually (A3).
- **C7. Script and strict CSP.** None exists today; N4 keeps the app compatible with a future one.
- **C8. Policy tension: a first-party script versus "Bootstrap's only behaviour is the collapse".** The intent calls for collapse "behind a toggle that opens and closes the menu"; a no-JS-only solution cannot satisfy keyboard, `aria-expanded` and desktop layout together (3.3). A small script is the least-bad choice and is the only JavaScript in the app; reviewers should reject growth of `navbar.js` beyond the toggle.
- **C9. Large mechanical diff.** The change touches every template and the whole stylesheet; reviewing needs the before/after screenshots. Split into the two PRs suggested in 5.4 if review load is a concern.

## 9. Assumptions
- The owner has not answered the preference-location question; this spec recommends both a `CLAUDE.md` line and a `[minor]` `REVIEW.md` check (3.1) and leaves the final call to matthew-js-porter at spec review.
- Page conformance proof (the spec author's question) is a manual checklist with screenshots plus a manual dependency-tree check (no tests assert Bootstrap's absence); screenshot diffing is not built (3.2).
- A first-party JavaScript file is allowed ("no new non-Boot dependency" is read as no third-party code), and it is the navbar toggle only (3.3).
- Browser breakpoints stay at those Bootstrap used (576/768/992/1200/1400px) because the design system already cites them; collapse stays at `lg` (992px).
- Body base values (16px, line-height 1.5) and gutters (12px) are carried over from Bootstrap's to keep pages visually stable.
- The nav icons from Bootstrap Icons stay (C5), and the "Toggle navigation" accessible name is unchanged.
- Without JavaScript the menu is always visible (progressive enhancement); no `<noscript>` element is needed.
- The `tomcat.version` and `jackson-bom.version` overrides are out of scope (C4).
- The `REVIEW.md` and `CLAUDE.md` edits happen in the implementation PR, not in this spec PR.
- `webjars-locator-lite` is removed together with `bootstrap`; nothing else in the app uses WebJars.
- The class names in F4 are suggestions; the plan fixes them.

## 10. Handoff notes for engineering
- Start by listing computed styles for each element type on the current pages (5.3) so nothing is lost when Reboot goes; this is the part most likely to cause visual drift.
- Keep Bootstrap loaded while migrating templates class by class, and remove it only when the pages look right without it, then run the full test suite.
- Arrange a human to republish the design-system artifact before merge (C1) and to confirm the preference-location question (3.1).
- Estimated effort: one to two days, mostly CSS verification across pages at two widths and the toggle behaviour with a screen reader.
