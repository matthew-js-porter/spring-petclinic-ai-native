Spring PetClinic is a plain, square, two-colour interface: a dark Spring-brown header with a thin Spring-green line on top, white pages, warm-grey striped tables, and green only where something is current or clickable. Build every page from the tokens and the four components below (Navbar, DataTable, Pagination, Footer) on top of Bootstrap 5.3.

## Content

- Write in plain, short English. No marketing voice, no exclamation marks, no emoji.
- Page titles are a single plural noun in Title Case: "Veterinarians", later "Owners". The one exception is the homepage, titled "Welcome".
- Nav items and table headers are written in Title Case in the markup ("Specialties") and set UPPERCASE by CSS; never type them in capitals.
- Data is shown as stored: lowercase specialty names (`radiology`, `dentistry surgery`), several values separated by a single space.
- An empty value is the word `none` in `ink-muted`, never a dash or a blank cell.
- The footer always reads "Spring PetClinic · a Spring Boot sample application".

## Colour

- `spring-brown` is the brand's dark: the navbar fill, and through `ink` and `line-strong`, all text and the header rule.
- `spring-green` is the brand's signature but fails as text on white (2.6:1). Use it only for the 4px accent bar (`border-accent`), the logo, and focus rings inside the navbar.
- `spring-green-dark` carries every interactive or current state: the active nav item fill, link text, enabled pagination chevrons.
- Pages are `surface` white; tables stripe with `surface-stripe` and divide rows with `line`.
- Secondary text (`none`, "Pages:", the footer) is `ink-muted`. `ink-disabled` is for disabled controls only.
- One theme. The source has no dark mode; don't invent one in a page.

## Type

- Two Google Fonts: Montserrat (`--font-display`) for the wordmark, page title, nav and table headers; Varela Round (`--font-body`) for everything else. Load them with `<link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@600;700&family=Varela+Round&display=swap" rel="stylesheet">`.
- `page-title` (32px/700) once per page as an `h2`, `ink`, `space-6` below the navbar.
- `brand` (20px/700) only for the navbar wordmark.
- `nav-item` (14px/600, 0.08em) and `table-head` (14px/700, 0.06em) are always UPPERCASE.
- `body` (16px/24px) for table cells, pagination and text; `small` (14px) for the footer.

## Layout and shape

- Content sits in Bootstrap's `.container`; the navbar spans the full width at `navbar-height` (64px) plus the 4px accent bar.
- Corners are square: `radius-none` everywhere. `radius-control` is Bootstrap's default, held back for form inputs.
- No shadows, no gradients, no cards. Structure comes from the brown bar, the 2px header rule and the 1px row lines.
- Spacing follows Bootstrap's scale: `space-3` × `space-4` table cell padding, `space-5` between a table and its pagination, `space-6` above the page title, `space-5` between the homepage title and its photo.
- The homepage photo is `hero-width` (360px) wide at most and shrinks with its container, so it never scrolls sideways on a phone.

## States and accessibility

- Current page: `aria-current="page"`. In the navbar that is the `spring-green-dark` fill; in pagination it is plain `ink` text.
- Focus: 2px solid `focus-ring` on light grounds, `focus-ring-on-dark` in the navbar. Never remove outlines.
- Use landmarks (`header`, `nav`, `main`, `footer`), a hidden `<caption>` and `<th scope="col">` on tables, and an `aria-label` on each pagination `nav`.
- Every text pair above meets WCAG AA; contrast figures are in each colour token's note.

## Iconography

- Logo: the heart-with-cross mark in `spring-green` from the Logos group, 24px (`logo-size`) beside the wordmark, `alt=""` because the wordmark names the link.
- Nav icons are thin outline glyphs before the label: a house for Home, a bulleted list for Veterinarians. They look like Bootstrap Icons (`house`, `list-ul`); the previews draw simple stand-ins, so use the real Bootstrap Icons in the app.
- Pagination uses text chevrons (« ‹ › ») rather than icons.
- One photograph, `pets.png` from the Photos group, on the homepage only: square, no border, shadow or card, with descriptive `alt` text.
- No emoji, no illustrations, and no other photographs.
