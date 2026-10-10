# DataTable

A full-width striped table for lists of records: vets, owners, and an owner's pets on Owner Information.

**The consumer provides** a caption, the column headers and the rows. Use `<caption>` (visually hidden), `<th scope="col">`, and one `<tr>` per record.

- Header cells: `table-head` style, UPPERCASE, `ink`, with a 2px `line-strong` rule beneath (`border-strong`).
- Body cells: `body` style, `ink`, padding `space-3` × `space-4` (rows come out at 49px).
- Rows alternate `surface-stripe` and `surface`, starting striped; each row has a 1px `line` bottom border.
- An empty value shows a word, not a blank cell: the vets list writes `none` in `ink-muted`. An empty collection shows no table at all: one `ink-muted` sentence in its place ("This owner has no pets."); see DetailList.
- A record that has its own page links from its name cell only (Find Owners: the owner's name to `/owners/{id}.html`). The link is default link text in `spring-green-dark` (5.8:1 on `surface`, 5.2:1 on `surface-stripe`) with the standard 2px `focus-ring` outline; other cells stay plain text.
- In the Thymeleaf app this is Bootstrap `table table-striped` with the stripe colour overridden in `petclinic.css`.

## Actions column (intent 010)

When each row has an action that opens another page (Edit Pet on Owner Information's pets table), it goes in a last column of its own:

- Header "Actions", Title Case in the markup, UPPERCASE by CSS, `<th scope="col">` like the others. It is the one header that names no data.
- Each cell holds one action link (`.pc-col-actions`, no wrap): link text in Title Case, `spring-green-dark`, underlined, with the 2px `focus-ring` outline. No icon, no button, no box.
- The visible text is the same on every row, so add the record's name for assistive tech in a visually hidden span: `<a href="/owners/1/pets/1/edit.html">Edit Pet<span class="visually-hidden"> Leo</span></a>`. Bootstrap's `visually-hidden` in the app, `.pc-visually-hidden` in these previews. A screen reader's link list then reads "Edit Pet Leo", not a column of identical "Edit Pet".
- Only the actions the page supports appear. Owner Information shows Edit Pet and nothing else (no Add Visit).
- The column counts toward the region rule below: the pets table with Name, Birth Date, Type and Actions is four columns and sits in a region.

An action for the whole collection ("Add New Pet") is not a column; it is an action link directly under the section title, `space-3` above the table, or above the empty sentence when there are no rows, so it is in the same place whatever the count.

## Wide tables

Tables of more than three columns (Owners: Name, Address, City, Telephone, Pets; Pets with Actions) sit in a `<div class="table-responsive pc-table-region" tabindex="0" role="region" aria-label="… table">`, so at phone width the table scrolls inside the region and the page does not. The region gets the standard 2px `focus-ring` outline so keyboard users can find and scroll it. Values that must not break, like telephone numbers and dates, use Bootstrap's `text-nowrap`. Tables of up to three columns (Vets) need no region; two-column tables keep `pc-col-name` at 50%.

Don't: add vertical borders, row hover colours or rounded corners; none are in the source. Don't put an action link in a data cell beside the value, or more than one link per actions cell without a design review.
