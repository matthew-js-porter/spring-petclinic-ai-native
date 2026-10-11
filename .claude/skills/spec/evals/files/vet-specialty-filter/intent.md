# Intent: Filter Vets by Specialty
Author: Eval Author

## Problem
The Veterinarians page lists every vet with their specialties, but front-desk staff can't narrow it down. When an owner phones about a dental problem, staff scroll through the whole list looking for "dentistry".

## Proposed outcome
Staff can pick a specialty on the Veterinarians page and see only the vets who have it.

Observable results:
- The project builds and all tests pass.
- The Veterinarians page has a way to choose one specialty, or "All", above the list. Choosing a specialty shows only the vets with it; "All" shows every vet, including vets with no specialty.
- The chosen specialty is kept in the page address, so a filtered list can be bookmarked and paginated.
- When no vet has the chosen specialty, the page says so in one plain sentence.
- The vets JSON is unchanged.
- The page follows the design system, is accessible, works without JavaScript and at phone width.

Out of scope: searching vets by name; editing vets or specialties; appointments.

## Affected users and systems
Front-desk staff; the Veterinarians page.

## Constraints
- Server-side rendering with Thymeleaf in the shared layout.
- Follow the design system; any new component goes into `docs/design-system/` in the same PR.

## Open questions
None.
