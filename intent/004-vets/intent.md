# Intent: Vets
Author: matthew-js-porter

## Problem
The app has a database (intent 003) but no PetClinic features yet, so there is nothing for a visitor to see beyond `/actuator/health`. Vets are the simplest PetClinic domain: they stand alone (no owners, pets or visits needed) and are read-only. Building them first gives a visitor a real page, a list of the clinic's veterinarians and their specialties, and settles how every later domain feature stores, seeds, renders and serves its data.

## Proposed outcome
A visitor can see the clinic's veterinarians and their specialties, as in the original Spring PetClinic.

Observable results:
- The project builds and all tests pass (`./mvnw clean verify`).
- On every start the database holds the original PetClinic vets and specialties (the six vets, e.g. James Carter and Helen Leary, with radiology, surgery and dentistry), with no manual step.
- A browser request for the vets page returns an HTML page, rendered server-side with Thymeleaf, listing each vet's name and specialties; a vet with no specialty shows "none".
- The page has the original PetClinic look: Bootstrap styling, shared page layout and navigation bar, with a link to the vets page.
- The list is paginated as in upstream PetClinic: 5 vets per page, with links to move between pages.
- A request for vets as JSON returns all vets with their specialties.
- `/actuator/health` still returns `UP`, and no other actuator endpoint is exposed.

Out of scope: adding, editing or deleting vets or specialties; owners, pets and visits (and their navigation entries); a welcome/home page beyond what the shared layout needs; internationalisation; security/login; file-based or production databases.

## Affected users and systems
- Clinic visitors (first user-facing page).
- Developers and agents: the patterns set here (schema, seed data, page layout, JSON, tests) are what later domain intents (owners, pets, visits) will follow.
- Application build dependencies (Thymeleaf, styling assets), database schema and data, configuration, and static/template resources.
- CI build and Snyk scans, which will see the new dependencies.

## Constraints
- Server-side rendering with Thymeleaf.
- Persistence through the existing Spring Data JDBC + in-memory H2 foundation (intent 003); no JPA.
- Match upstream Spring PetClinic's vets behaviour and look where practical; note deviations in the spec.
- Config stays YAML only (`application.yaml`).
- Actuator surface unchanged: only `health`, no details.
- New dependencies must pass the Snyk gate (no `high` or above findings).

## Open questions
- How tables and seed data are created: plain SQL scripts run at startup, or a migration tool. Decide in the spec; later intents will follow it. Owner: Matthew.
- URLs: keep upstream's `/vets.html` (HTML) and `/vets` (JSON), or something else? Suggested default: upstream's. Owner: Matthew.
- Navigation bar: show only links to pages that exist (Home, Veterinarians), or upstream's full nav with Find Owners added later? Suggested default: only existing pages. Owner: Matthew.
