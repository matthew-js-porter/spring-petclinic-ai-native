Spring PetClinic is a plain, square, two-colour interface: a dark Spring-brown header with a thin Spring-green line on top, white pages, warm-grey striped tables, and green only where something is current or clickable. Build every page from the tokens and the eight components below (Navbar, DataTable, DetailList, Pagination, Footer, FormField, Button, InlineMessage) on top of Bootstrap 5.3. OwnerForm is a page pattern composed from them (New Owner and Edit Owner), not a ninth component.

## Content

- Write in plain, short English. No marketing voice, no exclamation marks, no emoji.
- Page titles are a single plural noun in Title Case: "Veterinarians", later "Owners". The one exception is the homepage, titled "Welcome".
- Nav items and table headers are written in Title Case in the markup ("Specialties") and set UPPERCASE by CSS; never type them in capitals.
- Data is shown as stored: lowercase specialty names (`radiology`, `dentistry surgery`), several values separated by a single space.
- An empty value is the word `none` in `ink-muted`, never a dash or a blank cell. An empty collection (an owner with no pets) is one plain sentence in `ink-muted` in place of its table: "This owner has no pets."
- Detail pages are titled with upstream's wording, "Owner Information", as the one exception to the plural-noun rule besides "Welcome".
- The footer always reads "Spring PetClinic · a Spring Boot sample application".
- Form labels and button labels are short and in Title Case in the markup: "Last Name", "Find Owner". Buttons are set UPPERCASE by CSS, like nav items.
- Messages are one plain sentence ending in a full stop that repeats what the person typed in straight double quotes: `No owners found with a last name starting with "Dav".`
- Confirmations after a save use upstream's wording in Title Case, without a full stop: "New Owner Created", "Owner Values Updated".
- Field errors name the field and say what is needed: "First Name is required.", "Telephone must be exactly 10 digits.", "Address must be 255 characters or fewer." A form summary counts them: "Owner not saved: 2 fields need attention."
- Record form headings are "New Owner" and "Edit Owner"; their buttons are "Add Owner" and "Update Owner".

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
- `section-title` (24px/700) as an `h3` for a section under the page title, such as Pets; `space-6` above, `space-3` below.
- `brand` (20px/700) only for the navbar wordmark.
- `nav-item` (14px/600, 0.08em) and `table-head` (14px/700, 0.06em) are always UPPERCASE.
- `body` (16px/24px) for table cells, pagination, form labels, input text, messages and other text; `small` (14px) for the footer.
- Button labels use the `nav-item` style (14px/600, 0.08em, UPPERCASE) in `on-dark`.

## Layout and shape

- Content sits in Bootstrap's `.container`; the navbar spans the full width at `navbar-height` (64px) plus the 4px accent bar.
- Corners are square: `radius-none` everywhere, form inputs and buttons included. Set Bootstrap's `--bs-border-radius` to 0 rather than overriding corners per component.
- No shadows, no gradients, no cards. Structure comes from the brown bar, the 2px header rule and the 1px row lines.
- Forms put each field's label above its input. A search field and its button sit on one row from Bootstrap's `sm` breakpoint (576px) up and stack below it; inputs are full width on a phone and at most `field-width` (20rem) above that. Leave `space-5` below a form before the table or message it controls.
- A record form (New Owner, Edit Owner) is stacked: one field per row, `space-3` apart, the button below, the whole form at most `field-width` wide from `sm` up.
- Spacing follows Bootstrap's scale: `space-3` × `space-4` table cell padding, `space-5` between a table and its pagination, `space-6` above the page title, `space-5` between the homepage title and its photo.
- The homepage photo is `hero-width` (360px) wide at most and shrinks with its container, so it never scrolls sideways on a phone.

## Links

- Links are `spring-green-dark` text, underlined, with the 2px `focus-ring` outline. That covers record names in a DataTable and action links.
- An action that opens another page is an action link (`.pc-action`), never a Button: "Add Owner" directly under the Find Owners title, above the search form; "Edit Owner" under the owner's DetailList. Title Case, no icon, no box.
- Buttons only submit forms. Don't make a link look like a button.

## States and accessibility

- Current page: `aria-current="page"`. In the navbar that is the `spring-green-dark` fill; in pagination it is plain `ink` text.
- Focus: 2px solid `focus-ring` on light grounds, `focus-ring-on-dark` in the navbar. Never remove outlines; inputs and buttons get theirs with a 2px offset, replacing Bootstrap's focus shadow.
- Every input has a visible `<label for>`; placeholders never stand in for labels. A search form is `<form method="get" role="search">`. A message that appears after a submit has `role="status"`; a form summary of errors has `role="alert"`. Meaning is in the words, never in colour alone.
- An invalid field: `aria-invalid="true"`, `aria-describedby` to its error sentence, a 2px `line-strong` border and a 4px `line-strong` rule beside the message. There is no error colour. Required fields carry `required` and `aria-required="true"`; forms use `novalidate` so the server's messages are the only ones.
- Use landmarks (`header`, `nav`, `main`, `footer`), a hidden `<caption>` and `<th scope="col">` on tables, and an `aria-label` on each pagination `nav`.
- Every text pair above meets WCAG AA; contrast figures are in each colour token's note.

## Iconography

- Logo: the heart-with-cross mark in `spring-green` from the Logos group, 24px (`logo-size`) beside the wordmark, `alt=""` because the wordmark names the link.
- Nav icons are thin outline glyphs before the label: a house for Home, a magnifying glass for Find Owners, a bulleted list for Veterinarians. They look like Bootstrap Icons (`house`, `search`, `list-ul`); the previews draw simple stand-ins, so use the real Bootstrap Icons in the app.
- Pagination uses text chevrons (« ‹ › ») rather than icons.
- One photograph, `pets.png` from the Photos group, on the homepage only: square, no border, shadow or card, with descriptive `alt` text.
- No emoji, no illustrations, and no other photographs.
