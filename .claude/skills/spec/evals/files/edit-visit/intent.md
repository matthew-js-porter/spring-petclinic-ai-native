# Intent: Edit a Visit
Author: Eval Author

## Problem
Visits can be booked but not corrected. When staff type the wrong date or a misleading description, the mistake stays on the owner's details page and on the New Visit page's list of previous visits.

## Proposed outcome
A visitor can open one of a pet's visits, change its date or description, and see the corrected visit.

Observable results:
- The project builds and all tests pass.
- Each visit listed on Owner Information has an "Edit Visit" action that opens a form filled in with the visit's date and description.
- The form validates exactly as New Visit does (date required, a real date in yyyy-mm-dd; description required, at most 255 characters), keeps what was typed and shows the same messages.
- A valid save returns to the owner's details page with the corrected visit and the confirmation "Visit has been updated".
- Editing a visit that doesn't exist, or a visit that belongs to a different pet, or a pet of a different owner, returns "not found" and changes nothing.
- The visit keeps its id, and the owner's pets and other visits are unchanged.
- The form follows the design system, works without JavaScript and at phone width.

Out of scope: deleting visits; moving a visit to another pet; JSON for visits.

## Affected users and systems
Front-desk staff; Owner Information; visit data.

## Constraints
- Server-side rendering with Thymeleaf in the shared layout; plain HTML forms.
- Persistence through the existing Spring Data JDBC + H2 foundation.
- Follow the design system and reuse the VisitForm pattern.

## Open questions
None.
