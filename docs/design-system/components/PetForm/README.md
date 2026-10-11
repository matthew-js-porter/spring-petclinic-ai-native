# PetForm

The New Pet and Edit Pet page pattern (intent 010): a composition of FormField (text, date and select variants), Button, InlineMessage, action links and a DataTable actions column, not a new component. Use it as the review board for the pet form; it follows OwnerForm.

**The consumer provides** the heading ("New Pet" or "Edit Pet"), the owner's name for context, the button label ("Add Pet" or "Update Pet"), the form `action` (`/owners/{ownerId}/pets` or `/owners/{ownerId}/pets/{petId}`), the pet types in name order, today's date for the picker's `max`, the three values, and on a redisplay the field errors and the summary.

- Page: the shared layout with Find Owners current in the navbar (`aria-current="page"`), on both pages and on the redisplay. One `page-title` `h2`; the page title is "New Pet · Spring PetClinic" or "Edit Pet · Spring PetClinic".
- Order under the title: one plain line in `ink-muted` naming the owner, "Owner: George Franklin" (context, not a field); the form summary InlineMessage (`role="alert"`, only on a redisplay); then a stacked FormField form: "All fields are required." in `ink-muted`, Name, Birth Date, Type, then the Button.
- Fields:
  - Name: text, `maxlength="30"`, `autocomplete="off"` (no token fits a pet's name, and the browser must not offer the person's own).
  - Birth Date: the date variant, `max` set to today, `autocomplete="off"`, with the "yyyy-mm-dd" helper line tied by `aria-describedby`. Edit Pet fills it as `yyyy-MM-dd`.
  - Type: the select variant, "Select a type" prompt first, then `bird`, `cat`, `dog`, `hamster`, `lizard`, `snake`.
- Messages, one per field (required beats format beats rule): "Name is required.", "Name must be 30 characters or fewer.", "This owner already has a pet with this name.", "Birth Date is required.", "Birth Date must be a date in the form yyyy-mm-dd.", "Birth Date cannot be in the future.", "Type is required." Summary: "Pet not saved: N field(s) need attention."
- `<form method="post" novalidate>`; it works without JavaScript and at 320px.
- After a save the browser lands on Owner Information, which shows the confirmation InlineMessage (`role="status"`) under its title once: "New Pet has been Added" or "Pet details has been edited".
- Entry points on Owner Information: "Add New Pet", an action link directly under the Pets `section-title`, present with or without pets; "Edit Pet" in the pets table's actions column, one per row, with the pet's name hidden in the link for assistive tech (see DataTable).

No Cancel link, as on OwnerForm. "Add Visit" now sits after "Edit Pet" in the same actions cell; see VisitForm. The select and date controls are native, so their open lists and pickers look like the visitor's operating system; the closed control is square everywhere.

Not yet reviewed: there is no designer-approved board for this form. These previews are the board for the design owner's review before intent 010 merges.
