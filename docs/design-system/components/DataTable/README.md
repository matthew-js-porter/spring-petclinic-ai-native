# DataTable

A full-width striped table for lists of records: vets, owners, and an owner's pets on Owner Information.

**The consumer provides** a caption, the column headers and the rows. Use `<caption>` (visually hidden), `<th scope="col">`, and one `<tr>` per record.

- Header cells: `table-head` style, UPPERCASE, `ink`, with a 2px `line-strong` rule beneath (`border-strong`).
- Body cells: `body` style, `ink`, padding `space-3` × `space-4` (rows come out at 49px).
- Rows alternate `surface-stripe` and `surface`, starting striped; each row has a 1px `line` bottom border.
- An empty value shows a word, not a blank cell: the vets list writes `none` in `ink-muted`. An empty collection shows no table at all: one `ink-muted` sentence in its place ("This owner has no pets."); see DetailList.
- A record that has its own page links from its name cell only (Find Owners: the owner's name to `/owners/{id}.html`). The link is default link text in `spring-green-dark` with the standard 2px `focus-ring` outline; other cells stay plain text.
- In the Thymeleaf app this is Bootstrap `table table-striped` with the stripe colour overridden in `petclinic.css`.
- Wide tables (more than three columns, e.g. Owners: Name, Address, City, Telephone, Pets) sit in a `<div class="table-responsive pc-table-region" tabindex="0" role="region" aria-label="… table">`, so at phone width the table scrolls inside the region and the page does not. The region gets the standard 2px `focus-ring` outline so keyboard users can find and scroll it. Values that must not break, like telephone numbers, use Bootstrap's `text-nowrap`. Tables of up to three columns (Vets; Pets: Name, Birth Date, Type) need no region; two-column tables keep `pc-col-name` at 50%.

Don't: add vertical borders, row hover colours or rounded corners; none are in the source.
