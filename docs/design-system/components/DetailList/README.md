# DetailList

One record's labelled facts, such as an owner's Name, Address, City and Telephone on Owner Information, or the read-only context above a form. It is a definition list (`<dl>`), not a two-column table, so assistive tech reads each label with its value.

**The consumer provides** the label and value pairs in display order, each `<dt>`/`<dd>` pair wrapped in a `<div>`: `<dl class="pc-detail"><div><dt>Name</dt><dd>George Franklin</dd></div>…</dl>`.

- Labels (`dt`): `table-head` style on a 24px line, UPPERCASE via CSS, Title Case in the markup, `ink`.
- Values (`dd`): `body` style, `ink`. Long values wrap; they never scroll the page sideways.
- Rules: a 2px `line-strong` rule above the list (`border-strong`), as under a table header, and a 1px `line` below each pair.
- From the `md` breakpoint (768px) up, labels sit in a `detail-label-width` (12rem) column beside their values, with `space-3` × `space-4` padding. Below it, each label stacks above its value: the label keeps its top padding, the value has `space-1` above and `space-3` below.
- An empty value shows `none` in `ink-muted`, as in a DataTable.
- A related collection (an owner's pets) follows under a `section-title` `h3` as a DataTable. When the collection is empty, write one plain sentence in `ink-muted` in its place ("This owner has no pets."), not an empty table and not a bare `none`.

## Context block

Above a form that acts on a record the person did not type, a DetailList names that record: on New Visit, Pet, Type and Owner ("Samantha", "cat", "Jean Coleman"). Add `pc-context` (`<dl class="pc-detail pc-context">`) for `space-5` below it, before the summary or the form. It is the same list, never inputs, read-only fields or a box, so it cannot be mistaken for something to edit. PetForm's one-line "Owner: …" is the older form of this; the design owner decides whether it moves to the block.

Don't: stripe the rows, put the list in a card or box, add a shadow or rounded corners, or colour labels green.
