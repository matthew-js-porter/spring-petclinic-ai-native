# Intent: Modify Owners
Author: matthew-js-porter

## Problem
Visitors can't add or change owners. Owners can be listed and searched (intents 006, 007), and a visitor will be able to open one owner's details (intent 008). But the only owners are the ones seeded at startup. A new client can't be registered, and when an owner moves or changes phone number, their record can't be fixed. In upstream Spring PetClinic, Add Owner and Edit Owner are the first write features. Adding pets and visits later builds on them.

## Proposed outcome
A visitor can register a new owner and edit an existing owner's details, as in upstream.

Observable results:
- The project builds and all tests pass (`./mvnw clean verify`).
- The Find Owners page has an "Add Owner" action that opens a blank owner form.
- An owner's details page (intent 008) has an "Edit Owner" action that opens the same form, filled in with the owner's current details.
- The form has First Name, Last Name, Address, City and Telephone.
- Saving a valid new owner shows that owner's details page. The owner can then be found by last name on Find Owners.
- Saving a valid edit shows the owner's details page with the new values. The owner's pets are not changed.
- All five fields are required. Telephone must be exactly 10 digits, as upstream. Invalid input redisplays the form, keeps what was typed, and shows a clear message next to each field that failed.
- After a save, the details page shows a short confirmation ("New Owner Created", "Owner Values Updated").
- Editing an owner that doesn't exist returns a "not found" response, as the details page does.
- Changes persist for the life of the running app. A restart resets to the seed data, as today.
- The form follows the design system in `docs/design-system/`, is accessible (labelled fields, errors tied to their fields, visible focus), works without JavaScript, and works at phone width.
- The navbar shows Find Owners as the current section on the form pages.
- Find Owners, search, owner details, owners JSON, vets, homepage and `/actuator/health` behave as before.

Out of scope: deleting owners; adding or editing pets and visits; creating or editing owners through JSON; duplicate-owner detection; internationalisation; security/login.

## Affected users and systems
- Clinic visitors (front-desk staff registering and updating clients).
- The Find Owners page (new Add Owner action) and the owner details page from intent 008 (new Edit Owner action).
- Owner data: the first writes to the database. Seed data is unchanged.
- The design system: FormField, Button and InlineMessage exist. A form layout or a success message may be new.
- Page tests.

## Constraints
- Depends on intent 008. Ship after it.
- Server-side rendering with Thymeleaf, in the existing shared layout. Plain HTML form posts, with no JavaScript required.
- Persistence through the existing Spring Data JDBC + H2 foundation; no JPA.
- Follow the design system. Any new component or token goes into `docs/design-system/` in the same PR.
- Match upstream's add/edit owner flow, URLs and validation where practical; record deviations in the spec.
- Config stays YAML only; actuator surface unchanged.

## Open questions
- Should the form guard against cross-site request forgery, given there is no login? Suggested default: follow upstream (no CSRF token) and note the decision in the spec. Owner: Matthew.
- Does the success message need its own design system component, or is it an InlineMessage variant? Suggested default: an InlineMessage variant. Decide in the spec. Owner: Matthew.
