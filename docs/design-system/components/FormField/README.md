# FormField

A labelled text input: the label in `body` type above a square, `line-strong`-bordered input on `surface`.

**The consumer provides** the label text, the input's `id`, `name`, `type` and any `maxlength` or current value. Link label and input with `for`/`id`; the label is always visible, and a placeholder never replaces it.

- Label: `body` style, `ink`, `space-1` above the input.
- Input: `body` style text in `ink` on `surface`, 1px (`border-hairline`) `line-strong` border, `space-2` × `space-3` padding, `radius-none`. Full width on a phone, at most `field-width` (20rem) from the `sm` breakpoint up.
- Focus: 2px solid `focus-ring` outline with a 2px offset, replacing Bootstrap's focus shadow. Never removed.
- In a form, wrap fields and their Button in `.pc-form`: label-over-input fields and the button sit on one row, bottom-aligned, from `sm` (576px) up and stack below it, `space-3` apart, with `space-5` below the form. A search form is `<form method="get" role="search">` and works without JavaScript.
- In the Thymeleaf app this is Bootstrap's `form-label` and `form-control` with `pc-` classes in `petclinic.css` and `--bs-border-radius: 0`.

Don't: round the corners, use `spring-green` for the border or focus ring, put the label inside the input, or show validation by colour alone.
