# Navbar

The site header on every page: a `spring-brown` bar with a 4px `spring-green` accent line on top, the logo and "Spring PetClinic" wordmark on the left, and uppercase nav items on the right.

**The consumer provides** the list of nav items (label, href, optional icon) and which one is current. Mark the current item with `aria-current="page"`; it gets the `spring-green-dark` fill that runs the full `navbar-height`.

- Brand link: logo at `logo-size` with `alt=""` (the wordmark beside it names the link), wordmark in `brand`, colour `on-dark`. Links to `/`.
- Items: `nav-item` style, `on-dark`, `space-4` horizontal padding, icon `space-2` before the label. Hover uses the same `spring-green-dark` fill as the active item.
- Focus: 2px `focus-ring-on-dark` inset outline.
- Wrap it in `<header>` and `<nav aria-label="Main">`. In the Thymeleaf app this is Bootstrap's `navbar navbar-expand-lg` with the collapse toggle below `lg`; this preview shows the expanded state only.

Items in the app, in order: **Home** (house icon, links to `/`, current on the homepage) and **Veterinarians** (bulleted-list icon, current on `/vets.html`). Exactly one item is current on each page.

Do: add one item per top-level feature (Owners, Veterinarians...). Don't: put text in `spring-green` on this bar, or round the active item.
