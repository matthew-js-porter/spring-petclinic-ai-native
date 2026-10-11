# Intent: Delete a Pet
Author: Eval Author

## Problem
Front-desk staff can add and edit pets but can't remove one. When a pet is registered by mistake, or an owner asks for a pet's record to be removed, the pet stays on the owner's details page forever. Upstream Spring PetClinic has no delete either, so staff have no workaround.

## Proposed outcome
From an owner's details page, a visitor can delete one of that owner's pets and come back to the details page without it.

Observable results:
- The project builds and all tests pass.
- Each pet on Owner Information has a "Delete Pet" action. Deleting asks for confirmation first, on a page that names the pet and its owner.
- After confirming, the visitor is back on the owner's details page, the pet is gone, and a short confirmation reads "Pet has been deleted".
- A pet with visits can be deleted; its visits go with it.
- Deleting a pet that doesn't exist, or one that belongs to a different owner, returns "not found" and changes nothing.
- The owner's other pets, and the owners JSON apart from the removed pet, are unchanged.
- The confirmation page and action follow the design system, work without JavaScript and at phone width.

Out of scope: undoing a delete; deleting owners or visits on their own; JSON endpoints for deleting.

## Affected users and systems
Front-desk staff; the owner details page; pet and visit data.

## Constraints
- Server-side rendering with Thymeleaf in the shared layout; plain HTML forms.
- Persistence through the existing Spring Data JDBC + H2 foundation.
- Follow the design system; any new component goes into `docs/design-system/` in the same PR.
- No Spring Security or CSRF protection, as for the other forms while the data is fictitious.

## Open questions
None.
