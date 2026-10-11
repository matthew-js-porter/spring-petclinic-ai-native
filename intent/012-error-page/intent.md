# Intent: Custom Error Page
Author: matthew-js-porter

## Problem
When something goes wrong, the app drops out of its own design. An unknown owner or pet (`/owners/999.html`), a malformed id (`/owners/abc.html`), a wrong method, or an unexpected failure shows Spring Boot's default error page: no navbar, no way back, none of the design system. Every intent since 005 left "a custom error page" out of scope, and intents 008 to 010 added many more ways to land on a 404 or 400. Upstream Spring PetClinic shows its own error page inside the normal layout.

## Proposed outcome
A visitor who hits an error in the browser sees a page that looks like the rest of the app, says plainly what happened, and lets them carry on.

Observable results:
- The project builds and all tests pass (`./mvnw clean verify`).
- Browser requests that fail show a designed error page in the shared layout (navbar and footer), with the right HTTP status kept:
  - not found (404), e.g. an unknown owner, pet or path;
  - bad request (400), e.g. a non-numeric id or an invalid page number;
  - method not allowed (405);
  - an unexpected server error (500).
- The page states in plain words what went wrong, appropriate to the kind of error, and offers a way on (e.g. to the homepage or Find Owners). It never shows a stack trace, exception class or message, SQL, or other internals, for any error.
- No navbar item is marked as the current page on the error page.
- JSON endpoints (`/owners`, `/owners/{id}`, `/vets`) keep returning a JSON error body to JSON clients, without internals.
- The page follows the design system in `docs/design-system/` (including upstream's pets illustration if it fits), is accessible, and works at phone width.
- Every page and endpoint that works today behaves as before.

Out of scope:
- Upstream's deliberate-error demo (its "Error" navbar item and the URL that throws on purpose); the 500 page is proven by tests instead.
- Changing which requests are errors or their status codes (that is each feature's own decision).
- Error monitoring, alerting or logging changes.
- Internationalisation and security/login.

## Affected users and systems
- Clinic visitors who follow a bad link, mistype a URL or hit a failure.
- Spring Boot's error handling for HTML requests, used by every page.
- The shared layout and navbar (shown on the error page with no current item).
- The design system, if it lacks an error or "empty page" pattern.
- Tests that currently assert Boot's default error output or only the status code.

## Constraints
- Server-side rendering with Thymeleaf in the existing shared layout; no JavaScript needed.
- No internals reach the browser or JSON clients for any error, including 500s (Snyk and the security review gate check for this).
- Follow the design system; any new component, token or asset goes into `docs/design-system/` in the same PR.
- Match upstream's error page where practical; record deviations in the spec.
- Config stays YAML only; actuator surface unchanged.

## Open questions
None.

Resolved in review: JSON endpoints keep JSON error bodies; upstream's deliberate-error demo is left out.
