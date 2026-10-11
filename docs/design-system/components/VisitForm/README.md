# VisitForm

The New Visit page pattern: a context block, a two-field stacked form and the pet's previous visits. It is composed from DetailList, FormField (date and text variants), Button, InlineMessage and VisitList, not a new component, and follows PetForm.

**The consumer provides** the pet's name and type, the owner's name, the form `action` (`/owners/{ownerId}/pets/{petId}/visits`), today's date as the Date default, the two values, on a redisplay the field errors and the summary, and the pet's visits for the list.

- Page: the shared layout with Find Owners current in the navbar (`aria-current="page"`), also on the redisplay. One `page-title` `h2`, "New Visit"; the page title is "New Visit · Spring PetClinic".
- Order under the title:
  1. The context block: a DetailList (`dl.pc-detail.pc-context`) with Pet, Type and Owner ("Samantha", "cat", "Jean Coleman"), `space-5` below. These are facts, not inputs, so they are not mistaken for editable fields.
  2. The form summary InlineMessage (`role="alert"`), only on a redisplay: "Visit not saved: 2 fields need attention."
  3. A stacked FormField form: "All fields are required." in `ink-muted`, Date, Description, then the Button "Add Visit".
  4. A "Previous Visits" `section-title` `h3` and the pet's VisitList (rows or "This pet has no visits."), on the blank form and on every redisplay.
- Fields:
  - Date: the date variant with its "yyyy-mm-dd" helper line, pre-filled with today (the server's date), `autocomplete="off"`. No `max`: any real date is accepted, past or future.
  - Description: text, `maxlength="255"`, `autocomplete="off"`. Single line.
- Messages, one per field (required beats format): "Date is required.", "Date must be a date in the form yyyy-mm-dd.", "Description is required.", "Description must be 255 characters or fewer." Summary: "Visit not saved: N field(s) need attention."
- `<form method="post" novalidate>`; it works without JavaScript and at 320px, where the form stacks full width and the list's descriptions wrap.
- After a save the browser lands on Owner Information, which shows the confirmation InlineMessage (`role="status"`) once: "Your visit has been booked", and the new visit first in that pet's list.
- Entry point: "Add Visit" in the pets table's actions column on Owner Information, after "Edit Pet", with the pet's name hidden in the link (see DataTable).

The context block replaces nothing on PetForm: its "Owner: …" line stays until the design owner decides whether both forms should use the block. No Cancel or back link, as on the other forms.

Not yet reviewed: there is no designer-approved board for this page, for the two links in one actions cell, or for the Visits section. These previews are the board for the design owner's review before the visits work merges.
