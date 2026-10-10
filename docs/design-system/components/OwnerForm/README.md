# OwnerForm

The New Owner and Edit Owner page pattern (intent 009): a composition of FormField, Button, InlineMessage and action links, not a new component. Use it as the review board for the owner form and as the model for later record forms; PetForm (intent 010) follows it.

**The consumer provides** the heading ("New Owner" or "Edit Owner"), the button label ("Add Owner" or "Update Owner"), the form `action` (`/owners` or `/owners/{id}`), the five values, and on a redisplay the field errors and the summary.

- Page: the shared layout with Find Owners current in the navbar (`aria-current="page"`), on both pages and on the redisplay. One `page-title` `h2`; the page title is "New Owner · Spring PetClinic" or "Edit Owner · Spring PetClinic".
- Order under the title: the form summary InlineMessage (`role="alert"`, only on a redisplay), then a stacked FormField form: "All fields are required." in `ink-muted`, First Name, Last Name, Address, City, Telephone, then the Button.
- Fields: `autocomplete` `given-name`, `family-name`, `street-address`, `address-level2`, `tel-national`; Telephone is `type="tel" inputmode="numeric" maxlength="10"`. `maxlength` mirrors the columns: 30, 30, 255, 80, 10.
- `<form method="post" novalidate>`; it works without JavaScript and at 320px.
- After a save the browser lands on Owner Information, which shows the confirmation InlineMessage (`role="status"`) under its title once: "New Owner Created" or "Owner Values Updated".
- Entry points are action links: "Add Owner" directly under the Find Owners title, above the search form; "Edit Owner" under the owner's DetailList.

No Cancel link: leaving without saving is the navbar or the browser's back button (a design call left open).
