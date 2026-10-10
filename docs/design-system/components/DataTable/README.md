# DataTable

A full-width striped table for lists of records (vets now; owners and pets later).

**The consumer provides** a caption, the column headers and the rows. Use `<caption>` (visually hidden), `<th scope="col">`, and one `<tr>` per record.

- Header cells: `table-head` style, UPPERCASE, `ink`, with a 2px `line-strong` rule beneath (`border-strong`).
- Body cells: `body` style, `ink`, padding `space-3` × `space-4` (rows come out at 49px).
- Rows alternate `surface-stripe` and `surface`, starting striped; each row has a 1px `line` bottom border.
- An empty value shows a word, not a blank cell: the vets list writes `none` in `ink-muted`.
- In the Thymeleaf app this is Bootstrap `table table-striped` with the stripe colour overridden in `petclinic.css`.
- Wide tables (more than three columns, e.g. Owners: Name, Address, City, Telephone, Pets) sit in a `<div class="table-responsive pc-table-region" tabindex="0" role="region" aria-label="… table">`, so at phone width the table scrolls inside the region and the page does not. The region gets the standard 2px `focus-ring` outline so keyboard users can find and scroll it. Values that must not break, like telephone numbers, use Bootstrap's `text-nowrap`. Two-column tables (Vets) keep `pc-col-name` at 50% and need no region.

Don't: add vertical borders, row hover colours or rounded corners; none are in the source.
