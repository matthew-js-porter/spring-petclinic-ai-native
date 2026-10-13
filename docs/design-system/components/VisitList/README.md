# VisitList

One pet's visits as a two-column DataTable (Date, Description), most recent first, or the sentence "This pet has no visits." in its place. It is shared by Owner Information and New Visit.

**The consumer provides** the pet's name (for the caption) and its visits, already ordered by date descending, then id descending. Each visit is a date and a description.

- Table: a DataTable (`pc-table pc-visits`) with a visually hidden `<caption>` naming the pet ("Visits of Samantha") and `<th scope="col">` headers "Date" and "Description", Title Case in the markup, UPPERCASE by CSS.
- Date cell: `pc-col-date`, as `yyyy-MM-dd`, no wrap, as narrow as the date. Description cell: `body` text that wraps, long words included, so the table never scrolls the page sideways at 320px. Two columns need no `pc-table-region`.
- Empty: no table, one `pc-empty` sentence in `ink-muted`: "This pet has no visits." Not `none`, not an empty table.
- No links or actions inside the list; visits are not edited or deleted. "Add Visit" lives in the pets table's actions column (see DataTable), not here.

## Where it goes

- **Owner Information**: a "Visits" `section-title` `h3` follows the pets table. Under it, one `subsection-title` `h4` per pet, in the pets table's order (name, then id), each followed by its VisitList. Lists sit under the table, never nested inside a pet's row: a table in a table cell is poorly announced by screen readers and cannot fit 320px. An owner with no pets has no Visits section; "This owner has no pets." already says it.
- **New Visit**: under the form, a "Previous Visits" `section-title` `h3` and the VisitList of the pet being booked, with no `h4` (the context block above the form already names the pet).

In the Thymeleaf app this is one fragment, `owners/visitList.html :: visits(list)`, so both pages render it the same way.

Don't: nest it in the pets table, add a pet column (the heading names the pet), add icons, colour, or a count badge, or truncate the description.
