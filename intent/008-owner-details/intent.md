# Intent: Owner Details
Author: matthew-js-porter

## Problem
The app needs an owner details page. Find Owners (intent 006) lists each owner on one line, with pets shown by name only. A visitor can't open an owner to see their full record or their pets' details: birth date and type are stored but shown nowhere. In upstream Spring PetClinic, the owner details page is where everything about an owner and their pets lives. Later work on editing owners, adding pets and visits all starts from it.

## Proposed outcome
From the Find Owners list, a visitor can click an owner's name and see that owner's details and pets on a page of its own.

Observable results:
- The project builds and all tests pass (`./mvnw clean verify`).
- On the Find Owners page, each owner's name links to that owner's details page, including in search results (intent 007).
- An owner's details page shows their Name (first and last), Address, City and Telephone.
- It lists each of the owner's pets with Name, Birth Date and Type. Birth dates are shown as `yyyy-MM-dd`, as upstream. For example, George Franklin's page shows Leo, born 2010-09-07, a cat. Jean Coleman's page shows both Samantha and Max.
- The seed data gains one new owner with no pets, so on every start an owner's page can show a clear empty state instead of an empty table.
- An owner's page has a stable URL that can be bookmarked or shared.
- Asking for an owner who doesn't exist returns a "not found" response, not an error page or a blank page.
- A request for an owner's details as JSON returns the owner's address, city, telephone and pets, including each pet's birth date (`yyyy-MM-dd`, as upstream) and type.
- The page follows the design system in `docs/design-system/`, is accessible, and works at phone width.
- The navbar shows Find Owners as the current section on this page.
- Find Owners, owners JSON, vets, homepage and `/actuator/health` behave as before.

Out of scope:
- Visits (none are stored yet; a later intent).
- Edit Owner, Add New Pet, Edit Pet and Add Visit actions. These are left off the page, not shown disabled.
- Redirecting to the details page when a search finds exactly one owner.
- Internationalisation and security/login.

## Affected users and systems
- Clinic visitors.
- The Find Owners page (names become links).
- Owner and pet data, read-only; pet birth date and type are shown for the first time.
- Seed data: one new owner with no pets. Any count of owners that tests or pages rely on goes from 10 to 11, which also changes pagination.
- The shared layout and navbar.
- The design system, if it lacks a layout for showing one record's details.
- Page and JSON tests.

## Constraints
- Server-side rendering with Thymeleaf, in the existing shared layout.
- Persistence through the existing Spring Data JDBC + H2 foundation; no JPA. Seed data stays upstream's, plus the one pet-less owner.
- Follow the design system. Any new component or token (e.g. a detail or definition-list layout) goes into `docs/design-system/` in the same PR.
- Match upstream's owner details page (fields, URL shape) where practical; record deviations (no visits, no action buttons, the extra seeded owner) in the spec.
- Config stays YAML only; actuator surface unchanged.

## Open questions
- Should this build on intent 007 (search), or can it ship independently? Owner names should link from search results too, so the order of merging affects the work. Suggested default: independent; whichever ships second handles links in the other's view. Owner: Matthew.
- Who is the pet-less owner (name, address, city, telephone)? Suggested default: an upstream-style Madison-area owner, chosen in the spec, with a last name that doesn't collide with intent 007's search examples (e.g. not "Davis"). Owner: Matthew.

Resolved in review: the "no pets" state is covered by a new seeded owner, not by tests only; birth dates use upstream's `yyyy-MM-dd`.
