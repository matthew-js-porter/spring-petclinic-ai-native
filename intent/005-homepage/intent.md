# Intent: Homepage
Author: matthew-js-porter

## Problem
The app has no home page. Visiting the root URL (or clicking the navbar brand) silently redirects to the vets list, because intent 004 left a welcome page out of scope. A visitor lands on a table of veterinarians with no sense of what the application is, and there is no "Home" to return to. Upstream Spring PetClinic opens on a simple welcome page; this project should too, and it is the natural landing point for the owners, pets and visits features to come.

## Proposed outcome
A visitor arriving at the app sees a welcome page, as in the original Spring PetClinic.

Observable results:
- The project builds and all tests pass (`./mvnw clean verify`).
- A browser request for `/` returns the homepage directly (no redirect to the vets page), rendered server-side with Thymeleaf in the shared layout.
- The homepage shows a "Welcome" heading and upstream PetClinic's pets illustration as-is, with meaningful alt text.
- The navbar has a "Home" item before "Veterinarians"; its icon comes from the design system's Navbar icon set, and it is marked as the current page (`aria-current="page"`) on the homepage, and Veterinarians is current on the vets page. The brand link also leads to the homepage.
- The page follows the design system in `docs/design-system/` (tokens, navbar, footer, accessibility rules) and works at phone width.
- The vets page, vets JSON and `/actuator/health` behave as before.

Out of scope: owners, pets and visits (and their navigation entries); extra homepage content such as intro text, feature links or stats; internationalisation; security/login; a custom error page.

## Affected users and systems
- Clinic visitors (new landing page).
- Shared page layout and navbar, used by every page.
- The root URL handling, which currently redirects to the vets page.
- Static assets (the pets image) and, if the image or a "Home" nav pattern isn't covered, the design system docs.
- Page tests for the navbar and the new page.

## Constraints
- Server-side rendering with Thymeleaf, in the existing shared layout.
- Follow the design system; any new token, component or asset goes into `docs/design-system/` in the same PR rather than being hard-coded.
- Match upstream Spring PetClinic's welcome page where practical; note deviations in the spec.
- Reused upstream assets must be compatible with this project's licence (upstream is Apache 2.0).
- Actuator surface and config conventions unchanged (YAML only, only `health` exposed).

## Open questions
None. Resolved in review: the upstream pets illustration fits the design system as-is; the Home nav icon follows the design system's Navbar icon set.
