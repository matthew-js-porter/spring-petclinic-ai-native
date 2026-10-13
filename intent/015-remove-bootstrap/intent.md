# Intent: Remove Bootstrap and WebJars
Author: matthew-js-porter

## Problem
The app pulls Bootstrap in through two WebJars (`org.webjars:bootstrap`, pinned by `webjars-bootstrap.version`, and `webjars-locator-lite`). They are the only dependencies in `pom.xml` that Spring Boot does not manage, so they need their own version pin, their own Renovate updates and their own vulnerability tracking, separate from the Boot upgrades that cover everything else. In the owner's words: "I'd like to try to stick to just spring boot dependencies if possible."

The app gets little from Bootstrap. The design system (square corners, no shadows, cards or gradients) already overrides most of what Bootstrap provides, the pages mostly use the app's own `pc-*` styles, and the only Bootstrap behaviour in use is the navbar's collapsing menu on narrow screens. About twenty Bootstrap classes remain in the templates: the page container, tables (striped, responsive), form fields and labels, buttons, the navbar and its collapse, and a few utilities such as visually hidden text.

## Proposed outcome
The app has no Bootstrap and no WebJars, and every page still follows the design system.

Observable results:
- `pom.xml` lists no WebJars dependency and no `webjars-bootstrap.version`; every remaining dependency's version comes from Spring Boot.
- No page requests a Bootstrap asset, from the app or from any external host.
- Every page (home, owner search and list, owner details, owner/pet/visit forms, vets, error) conforms to `docs/design-system/` and its component previews at desktop and phone widths. Small browser-default differences are acceptable as long as the system's rules hold; pixel-identical output is not required.
- On narrow screens the navbar still collapses behind a toggle that opens and closes the menu, works by keyboard, and announces its expanded or collapsed state to screen readers.
- The accessibility behaviour the design system requires is unchanged: landmarks, hidden table captions, `aria-current`, labelled pagination, visible focus outlines.
- `./mvnw clean verify` passes, with the page tests checking the new assets instead of WebJar paths.
- `CLAUDE.md` and `docs/design-system/` no longer describe the UI as built on Bootstrap 5.3.
- A standing project preference is recorded: favour Spring Boot-managed dependencies. A non-Boot dependency is not forbidden, but it needs a stated reason in its spec.

Out of scope:
- Visual redesign, new components or new pages.
- Changing back-end dependencies or behaviour.
- Dark mode or any other theme.

## Affected users and systems
- Everyone using the PetClinic UI, including keyboard and screen-reader users and those on phones.
- The Thymeleaf templates, the shared layout and navbar, the app stylesheet, and the self-hosted fonts.
- `docs/design-system/` and its published artifact (https://claude.ai/artifact/7i5ZiTpYfptjDK4MKvD7XP), which must stay in step.
- Page tests (`VetsPageHtmlUnitTests` asserts the WebJar asset paths).
- `pom.xml`, Renovate (one fewer dependency to manage) and Snyk.

## Constraints
- The design system is the visual reference; any token or component it lacks is added to `docs/design-system/` in the same PR rather than hard-coded.
- No new non-Boot dependency and no CDN or third-party runtime request to replace Bootstrap.
- Accessibility must not regress.
- The usual repo rules apply: branch and PR, CI green, YAML-only config.

## Open questions
- Where the "favour Spring Boot dependencies" preference lives and how strongly it is enforced: a `CLAUDE.md` line only, or also a `REVIEW.md` check that flags a new non-Boot dependency without a stated reason. Owner: matthew-js-porter.
- How page conformance is proven: screenshot comparison against the design-system previews, a manual check, or both. Owner: spec author.
