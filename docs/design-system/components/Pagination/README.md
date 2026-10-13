# Pagination

The page control under a list: `Pages: [ 1 2 ]` followed by first, previous, next and last chevrons.

**The consumer provides** the current page, the page count and a URL per page (`@{/vets.html(page=${n})}` in Thymeleaf). Wrap it in `<nav>` with an `aria-label` naming the list ("Vets pages").

- Label and brackets: `body`, `ink-muted`.
- Current page: plain text in `ink`, `aria-current="page"`, not a link.
- Other pages: underlined links in `spring-green-dark`. Chevrons are links in `spring-green-dark` without underline, each with an `aria-label`.
- Disabled chevrons (first/previous on page 1, next/last on the last page) are `ink-disabled` text, not links. That colour is 2.2:1, allowed only because the control is disabled.
- Items sit `space-2` apart; the row sits `space-5` below the table.

Don't: draw boxed page buttons; PetClinic's pagination is plain text.
