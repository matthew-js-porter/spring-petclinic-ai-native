# Intent: Find Owners
Author: matthew-js-porter

## Problem
Owners are the core of PetClinic, but the app has none yet: a visitor can see vets (intent 004) and, once intent 005 lands, a welcome page, yet there is no way to see the clinic's pet owners. Upstream Spring PetClinic's "Find Owners" page is the entry point to everything about owners, pets and visits. For now this should be a read-only page with some seeded owners, so owners and their pets exist in the database and on screen before any search, editing or details work begins.

## Proposed outcome
A visitor can open a Find Owners page from the navbar and see the clinic's owners, each with Name, Address, City, Telephone and Pets.

Observable results:
- The project builds and all tests pass (`./mvnw clean verify`).
- On every start the database holds upstream PetClinic's 10 owners (e.g. George Franklin, Betty Davis) with their 13 pets and the pet types, with no manual step.
- A browser request for the owners page returns an HTML page, rendered server-side with Thymeleaf in the shared layout, listing every owner with Name (first and last), Address, City, Telephone and Pets (the names of that owner's pets, names only as upstream; an owner with no pets shows "none").
- The list is paginated like the vets page: 5 owners per page, with links between pages.
- The navbar has a "Find Owners" item, in upstream order (Home, Find Owners, Veterinarians) regardless of whether intent 005 or 006 is built first, marked as the current page (`aria-current="page"`) on this page.
- A request for owners as JSON returns all owners with their address, city, telephone and pets.
- The page follows the design system in `docs/design-system/` and works at phone width.
- The vets page, vets JSON, homepage and `/actuator/health` behave as before.

Out of scope: the last-name search form (the page is a plain list for now); adding or editing owners or pets; an owner details page (names are plain text, not links); visits; internationalisation; security/login.

## Affected users and systems
- Clinic visitors (new owners page and nav entry).
- Database schema and seed data (new owners, pets and pet types).
- Shared page layout and navbar, used by every page.
- Page and JSON tests; the patterns set here (owner/pet storage) are what later owner, pet and visit intents build on.

## Constraints
- Server-side rendering with Thymeleaf, in the existing shared layout.
- Persistence through the existing Spring Data JDBC + in-memory H2 foundation (intent 003); no JPA.
- Follow the design system; any new token or component goes into `docs/design-system/` in the same PR.
- Match upstream Spring PetClinic's owners list (columns, seed data) where practical; note deviations in the spec.
- Config stays YAML only; actuator surface unchanged (only `health`).

## Open questions
- URLs: upstream uses `/owners/find` for the search form and `/owners` for the list. With no search form, should the navbar's "Find Owners" go straight to `/owners`, and where does the JSON live? Suggested default: HTML at `/owners`, JSON by content negotiation or a sibling path, decided in the spec. Owner: Matthew.

Resolved in review: nav order is Home, Find Owners, Veterinarians; the Pets column shows pet names only.
