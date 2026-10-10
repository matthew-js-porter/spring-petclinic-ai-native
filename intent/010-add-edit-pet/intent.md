# Intent: Add or Edit a Pet
Author: matthew-js-porter

## Problem
The owner details page (intent 008) shows an owner's pets but is read-only. A visitor can't add a new pet for an owner or fix a pet's name, birth date or type. Owners can be added and edited (intent 009), but the pets are only the seeded ones. In upstream Spring PetClinic, "Add New Pet" and "Edit Pet" on the owner details page are how pets get into the system, and visits (a later intent) need pets to hang off.

## Proposed outcome
From an owner's details page, a visitor can add a pet to that owner or edit one of their pets, then return to the details page and see the change.

Observable results:
- The project builds and all tests pass (`./mvnw clean verify`).
- The owner details page has an "Add New Pet" action, and each listed pet has an "Edit Pet" action.
- Add New Pet opens a form for Name, Birth Date and Type for that owner. Type is chosen from the existing pet types (cat, dog, lizard, snake, bird, hamster).
- Edit Pet opens the same form filled in with the pet's current values.
- On a valid save the visitor is taken back to the owner's details page, which shows the new or changed pet and a success message ("New Pet has been Added" / "Pet details has been edited", as upstream).
- Validation matches upstream. Each failure redisplays the form with the visitor's input kept and a clear error next to the field:
  - name is required;
  - an owner can't have two pets with the same name, ignoring case (e.g. adding "Leo" or "leo" to George Franklin is rejected), but editing a pet without renaming it is allowed;
  - birth date is required, entered with a date picker, and can't be in the future;
  - type is required.
- An owner with no pets (the one seeded in intent 008, or one just added through intent 009) can be given a first pet, after which the empty state is replaced by the pet list.
- Adding or editing a pet for an owner that doesn't exist, or editing a pet that doesn't exist or belongs to a different owner, returns "not found", not an error page.
- Owner JSON (intent 008) reflects added and edited pets.
- Forms and actions follow the design system in `docs/design-system/`, are accessible (labelled fields, errors tied to their fields), and work at phone width.
- Find Owners, search, owner details, vets, homepage and `/actuator/health` behave as before.

Out of scope:
- Deleting pets.
- Adding visits (the Add Visit action stays off the page). Adding and editing owners belongs to intent 009.
- JSON endpoints for creating or updating pets; JSON stays read-only.
- Keeping changes across restarts. The in-memory database reloads seed data on start, so added or edited pets are lost when the app restarts. That is acceptable for now.
- Internationalisation and security/login.

## Affected users and systems
- Clinic visitors (staff using the app).
- The owner details page: new actions, success message.
- New add and edit pet form pages.
- Pet data, written for the first time (owner data is written by intent 009); pet types are read for the choice list.
- Owner JSON (content changes as pets change; shape doesn't).
- The design system, if it lacks a form layout, a select field, field-level errors or a success message.
- Page and controller tests.

## Constraints
- Server-side rendering with Thymeleaf in the existing shared layout; form submission works without JavaScript.
- Persistence through the existing Spring Data JDBC + H2 foundation; no JPA. Seed data unchanged.
- Follow the design system. Any new component or token goes into `docs/design-system/` in the same PR (FormField, Button and InlineMessage exist and should be reused). Forms, validation messages and confirmations look and behave like the owner form from intent 009.
- Match upstream's pet form (fields, URL shape, validation rules, messages) where practical; record deviations in the spec.
- No Spring Security and no CSRF protection. Anyone who can reach the app can change pet data; acceptable only while the data is fictitious, and recorded in the spec as a known concern.
- Config stays YAML only; actuator surface unchanged.

## Open questions
None.

Resolved in review: no Spring Security or CSRF while the data is fictitious; the duplicate-name check ignores case (as upstream); birth date is entered with a native date picker.
