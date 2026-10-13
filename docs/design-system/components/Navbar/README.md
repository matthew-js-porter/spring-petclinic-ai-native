# Navbar

The site header on every page: a `spring-brown` bar with a 4px `spring-green` accent line on top, the logo and "Spring PetClinic" wordmark on the left, and uppercase nav items on the right.

**The consumer provides** the list of nav items (label, href, optional icon) and which one is current. Mark the current item with `aria-current="page"`; it gets the `spring-green-dark` fill that runs the full `navbar-height`.

- Brand link: logo at `logo-size` with `alt=""` (the wordmark beside it names the link), wordmark in `brand`, colour `on-dark`. Links to `/`.
- Items: `nav-item` style, `on-dark`, `space-4` horizontal padding, icon `space-2` before the label. Hover uses the same `spring-green-dark` fill as the active item.
- Focus: 2px `focus-ring-on-dark` inset outline.
- Wrap it in `<header>` and `<nav class="pc-navbar" aria-label="Main">`; the items are a `<ul class="pc-nav">` inside `<div class="pc-navbar-menu" id="main-navbar">`.

## Collapsed below `lg` (992px)

From 992px the items sit in a row, right-aligned, and the toggle is hidden. Below it the bar shows the brand and a toggle; the menu opens beneath as a full-width stack, each item keeping the `navbar-height` (64px) row and the `spring-green-dark` hover and current fill.

- Toggle: `<button type="button" class="pc-navbar-toggle" aria-controls="main-navbar" aria-expanded="false" aria-label="Toggle navigation">` holding an inline three-line SVG (`aria-hidden="true"`, `focusable="false"`, `currentColor`). At least 48px wide and the full 64px tall; `on-dark` glyph, `spring-green-dark` fill on hover and while open, square, 2px `focus-ring-on-dark` inset outline.
- The name stays "Toggle navigation" in both states; `aria-expanded` carries the state, so a screen reader says "collapsed" or "expanded".
- Behaviour, from one small first-party script (`navbar.js`, no library): a click flips `aria-expanded` and shows or hides the menu; `Escape` while open closes it and returns focus to the toggle. Enter and Space work because it is a native button. Focus leaving the menu does not close it. Widening past 992px shows the row whatever the state.
- Without JavaScript the toggle stays hidden and the menu is always shown, so every page stays navigable. The script marks the page (a `pc-js` class on `<html>`) before the collapsed state applies.
- The script has no inline code or handlers, so a `script-src 'self'` policy works.

Items in the app, in order: **Home** (house icon, links to `/`, current on the homepage), **Find Owners** (search icon, links to `/owners.html`, current there) and **Veterinarians** (bulleted-list icon, current on `/vets.html`). Exactly one item is current on each page.

Do: add one item per top-level feature (Owners, Veterinarians...). Don't: put text in `spring-green` on this bar, round the active item or the toggle, close the menu on blur, or let the script grow beyond the toggle.
