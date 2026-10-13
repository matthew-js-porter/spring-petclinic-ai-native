Spring PetClinic is a plain, square, two-colour interface: a dark Spring-brown header with a thin Spring-green line on top, white pages, warm-grey striped tables, and green only where something is current or clickable. Build every page from the tokens and the nine components below (Navbar, DataTable, DetailList, Pagination, Footer, FormField, Button, InlineMessage, VisitList), with one hand-written stylesheet and no CSS framework. OwnerForm (New Owner, Edit Owner), PetForm (New Pet, Edit Pet), VisitForm (New Visit) and ErrorPage (any failed request) are page patterns composed from them, not further components.

## Content

- Write in plain, short English. No marketing voice, no exclamation marks, no emoji.
- Page titles are a single plural noun in Title Case: "Veterinarians", later "Owners". The one exception is the homepage, titled "Welcome".
- Nav items and table headers are written in Title Case in the markup ("Specialties") and set UPPERCASE by CSS; never type them in capitals.
- Data is shown as stored: lowercase specialty names (`radiology`, `dentistry surgery`), several values separated by a single space.
- An empty value is the word `none` in `ink-muted`, never a dash or a blank cell. An empty collection is one plain sentence in `ink-muted` in place of its table: "This owner has no pets.", "This pet has no visits."
- Detail pages are titled with upstream's wording, "Owner Information", as the one exception to the plural-noun rule besides "Welcome". Error pages are titled with what happened ("Page Not Found", "Something Went Wrong").
- Error pages state what happened in one plain sentence and offer a way on; they never show internals (status code, path, input, exception, trace or SQL) and never say which record was missing. The wording table is in ErrorPage.
- The footer always reads "Spring PetClinic · a Spring Boot sample application".
- Form labels and button labels are short and in Title Case in the markup: "Last Name", "Find Owner". Buttons are set UPPERCASE by CSS, like nav items.
- Messages are one plain sentence ending in a full stop that repeats what the person typed in straight double quotes: `No owners found with a last name starting with "Dav".`
- Confirmations after a save use upstream's wording in Title Case, without a full stop: "New Owner Created", "Owner Values Updated".
- Field errors name the field and say what is needed: "First Name is required.", "Telephone must be exactly 10 digits.", "Address must be 255 characters or fewer." A form summary counts them: "Owner not saved: 2 fields need attention."
- Record form headings are "New Owner", "Edit Owner", "New Pet", "Edit Pet" and "New Visit"; their buttons are "Add Owner", "Update Owner", "Add Pet", "Update Pet" and "Add Visit".
- Pet confirmations keep upstream's wording exactly, as intent 010 fixes it: "New Pet has been Added", "Pet details has been edited". Pet field errors follow the same pattern as owner ones: "Name is required.", "Birth Date cannot be in the future.", "Type is required.", "This owner already has a pet with this name."
- The visit confirmation keeps upstream's wording, "Your visit has been booked". Visit field errors: "Date is required.", "Date must be a date in the form yyyy-mm-dd.", "Description is required.", "Description must be 255 characters or fewer."
- A select's empty first option is a prompt: "Select a type". A date field shows its format as a helper line, "yyyy-mm-dd".

## Colour

- `spring-brown` is the brand's dark: the navbar fill, and through `ink` and `line-strong`, all text and the header rule.
- `spring-green` is the brand's signature but fails as text on white (2.6:1). Use it only for the 4px accent bar (`border-accent`), the logo, and focus rings inside the navbar.
- `spring-green-dark` carries every interactive or current state: the active nav item fill, link text (including record names in a DataTable that open the record's page), enabled pagination chevrons, the primary button fill.
- Pages are `surface` white; tables stripe with `surface-stripe` and divide rows with `line`.
- Secondary text (`none`, "Pages:", the footer) is `ink-muted`. `ink-disabled` is for disabled controls only.
- One theme. The source has no dark mode; don't invent one in a page.

## Type

- Two Google Fonts: Montserrat (`--font-display`) for the wordmark, page title, nav and table headers; Varela Round (`--font-body`) for everything else. Load them with `<link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@600;700&family=Varela+Round&display=swap" rel="stylesheet">`.
- `page-title` (32px/700) once per page as an `h2`, `ink`, `space-6` below the navbar.
- `section-title` (24px/700) as an `h3` for a section under the page title, such as Pets, Visits or Previous Visits; `space-6` above, `space-3` below.
- `subsection-title` (18px/700) as an `h4` inside a section: one per pet in Visits on Owner Information; `space-5` above, `space-2` below.
- `brand` (20px/700) only for the navbar wordmark.
- `nav-item` (14px/600, 0.08em) and `table-head` (14px/700, 0.06em) are always UPPERCASE.
- `body` (16px/24px) for table cells, pagination, form labels, input text, messages and other text; `small` (14px) for the footer.
- Button labels use the `nav-item` style (14px/600, 0.08em, UPPERCASE) in `on-dark`.

## Layout and shape

- Content sits in `.pc-container`: full width with 12px gutters, centred, at most 540px from 576px, 720px from 768px, 960px from 992px, 1140px from 1200px and 1320px from 1400px. The navbar spans the full width at `navbar-height` (64px) plus the 4px accent bar.
- Breakpoints are the system's own: `sm` 576px (search rows, form widths), `md` 768px, `lg` 992px (the navbar collapses below it), then 1200px and 1400px for the container only.
- Corners are square: `radius-none` everywhere, form inputs and buttons included.
- No shadows, no gradients, no cards. Structure comes from the brown bar, the 2px header rule and the 1px row lines.
- Forms put each field's label above its input. A search field and its button sit on one row from the `sm` breakpoint (576px) up and stack below it; inputs are full width on a phone and at most `field-width` (20rem) above that. Leave `space-5` below a form before the table or message it controls.
- A record form (New Owner, Edit Owner, New Pet, Edit Pet, New Visit) is stacked: one field per row, `space-3` apart, the button below, the whole form at most `field-width` wide from `sm` up.
- Spacing uses the `space-*` scale: `space-3` × `space-4` table cell padding, `space-5` between a table and its pagination, `space-6` above the page title, `space-5` between the homepage title and its photo.
- Records of records sit in sections, never in a table cell: a pet's visits are a VisitList under a Visits `h3` and the pet's `h4`, below the pets table, not inside its row.
- A form that acts on a record the person did not type (New Visit) names it first in a context block, a DetailList of read-only facts, `space-5` above the form.
- The homepage photo is `hero-width` (360px) wide at most and shrinks with its container, so it never scrolls sideways on a phone.

## Building it

The system needs no CSS framework, no WebJar and no CDN. Pages are plain HTML (Thymeleaf in the app) styled by one hand-written stylesheet that maps these tokens onto `pc-*` classes and starts with a small reset: `box-sizing: border-box`, body margin 0 at 16px with a 1.5 line height, `border-collapse: collapse` on tables, `font: inherit` on form controls, `[hidden] { display: none }`. The one script is the navbar toggle (see Navbar), and every page works without it. Element classes: `pc-container`, `pc-navbar`, `pc-navbar-toggle`, `pc-navbar-menu`, `pc-nav`, `pc-table`, `pc-table-region`, `pc-input`, `pc-button`, `pc-nowrap`, `pc-sr-only` (visually hidden, still read by assistive technology).

## Links

- Links are `spring-green-dark` text, underlined, with the 2px `focus-ring` outline. That covers record names in a DataTable and action links.
- An action that opens another page is an action link (`.pc-action`), never a Button: "Add Owner" directly under the Find Owners title, above the search form; "Edit Owner" under the owner's DetailList; "Add New Pet" directly under the Pets section title, with or without pets; "Go to the Homepage" and "Find Owners" on an error page. Title Case, no icon, no box.
- Per-row actions go in a DataTable actions column ("Edit Pet", then "Add Visit", `space-3` apart, at most two), with the record's name added in a visually hidden span so each link reads uniquely ("Edit Pet Leo", "Add Visit Leo").
- Buttons only submit forms. Don't make a link look like a button.

## States and accessibility

- Current page: `aria-current="page"`. In the navbar that is the `spring-green-dark` fill; in pagination it is plain `ink` text.
- Focus: 2px solid `focus-ring` on light grounds, `focus-ring-on-dark` in the navbar. Never remove outlines; inputs and buttons get theirs with a 2px offset; nothing draws a focus shadow.
- Selects and date inputs are native controls styled like text inputs (square box, same border and focus); their open lists and pickers stay the browser's.
- Every input has a visible `<label for>`; placeholders never stand in for labels. A search form is `<form method="get" role="search">`. A message that appears after a submit has `role="status"`; a form summary of errors has `role="alert"`. Meaning is in the words, never in colour alone.
- An invalid field: `aria-invalid="true"`, `aria-describedby` to its error sentence, a 2px `line-strong` border and a 4px `line-strong` rule beside the message. There is no error colour. Required fields carry `required` and `aria-required="true"`; forms use `novalidate` so the server's messages are the only ones.
- Use landmarks (`header`, `nav`, `main`, `footer`), a hidden `<caption>` and `<th scope="col">` on tables, and an `aria-label` on each pagination `nav`.
- Every text pair above meets WCAG AA; contrast figures are in each colour token's note.

## Iconography

- Logo: the heart-with-cross mark in `spring-green` from the Logos group, 24px (`logo-size`) beside the wordmark, `alt=""` because the wordmark names the link.
- Nav icons are thin outline glyphs before the label: a house for Home, a magnifying glass for Find Owners, a bulleted list for Veterinarians. The app inlines their SVG paths, taken from Bootstrap Icons (`house`, `search`, `list-ul`, MIT licence), as `aria-hidden` `currentColor` icons; nothing is loaded from that project. The previews draw simple stand-ins.
- The navbar toggle is a three-line menu glyph drawn the same way (inline SVG, `currentColor`, 1.5px stroke).
- Pagination uses text chevrons (« ‹ › ») rather than icons.
- One photograph, `pets.png` from the Photos group, on the homepage only: square, no border, shadow or card, with descriptive `alt` text.
- No emoji, no illustrations, and no other photographs.
