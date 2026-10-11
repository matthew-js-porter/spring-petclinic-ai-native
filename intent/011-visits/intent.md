# Intent: Visits
Author: matthew-js-porter

## Problem
The app records owners and their pets (intents 008 to 010) but nothing about the care those pets receive. A visitor can't see when a pet last came in or why, and can't record a new visit. In upstream Spring PetClinic, visits are the point of the clinic: each pet on the owner details page lists its visits, and "Add Visit" is how they are recorded. Intents 008 and 010 both left visits to "a later intent"; this is it.

## Proposed outcome
From an owner's details page, a visitor can see each pet's visit history and add a visit to any pet, then return to the details page and see it.

Observable results:
- The project builds and all tests pass (`./mvnw clean verify`).
- Seed data includes upstream's four visits: Samantha (rabies shot 2013-01-01, spayed 2013-01-04) and Max (rabies shot 2013-01-02, neutered 2013-01-03), both pets of Jean Coleman. Every other pet starts with none.
- The owner details page shows, for each pet, its visits (date and description), most recent first, and a clear empty state for a pet with no visits.
- Each pet has an "Add Visit" action beside "Edit Pet".
- Add Visit opens a form that shows which pet (and whose) the visit is for, and the pet's previous visits, with fields for Date and Description. Date is entered with a date picker, defaults to today, and may be any date (future dates are allowed for booking ahead, as upstream).
- On a valid save the visitor is taken back to the owner's details page, which shows the new visit and a success message ("Your visit has been booked", as upstream).
- Validation: description is required, date is required and must be a real date. Each failure redisplays the form with the visitor's input kept and a clear error next to the field.
- Adding a visit for an owner that doesn't exist, or a pet that doesn't exist or belongs to a different owner, returns "not found".
- Owner JSON (intent 008) includes each pet's visits; existing fields keep their shape.
- The form, the visit lists and the new action follow the design system in `docs/design-system/`, are accessible (labelled fields, errors tied to their fields, each "Add Visit" link names its pet), and work at phone width.
- Find Owners, search, add/edit owner, add/edit pet, vets, homepage and `/actuator/health` behave as before.

Out of scope:
- Editing or deleting visits; cancelling a booked visit.
- Assigning a vet to a visit, visit types, times of day, or any scheduling or calendar view.
- JSON endpoints for creating visits; JSON stays read-only.
- Keeping changes across restarts (the in-memory database reloads the seed on start).
- Internationalisation and security/login.

## Affected users and systems
- Clinic visitors (staff using the app).
- The owner details page: visit lists per pet, the Add Visit action, the success message.
- A new Add Visit form page.
- Visit data: a new kind of stored record, with seed rows, belonging to a pet.
- Owner JSON (gains visits per pet).
- The design system, if it lacks a pattern for a nested list of records per pet or for a form that shows read-only context above it.
- Page, controller, repository and JSON tests; tests that assert the details page has no Add Visit action change.

## Constraints
- Server-side rendering with Thymeleaf in the existing shared layout; form submission works without JavaScript.
- Persistence through the existing Spring Data JDBC + H2 foundation and plain SQL scripts; no JPA. Existing seed data unchanged; visits are added to it.
- Forms, validation messages and confirmations look and behave like the owner and pet forms from intents 009 and 010. Any new component or token goes into `docs/design-system/` in the same PR.
- Match upstream's visit feature (fields, URL shape, messages) where practical; record deviations in the spec.
- No Spring Security and no CSRF protection, as in 009 and 010; acceptable only while the data is fictitious, and recorded in the spec as a known concern.
- Config stays YAML only; actuator surface unchanged.

## Open questions
None.

Resolved in review: seed upstream's four visits; a visit date may be in the future (as upstream), defaulting to today.
