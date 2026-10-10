# FormField

A labelled text input: the label in `body` type above a square, `line-strong`-bordered input on `surface`, with an error state that is read with the field.

**The consumer provides** the label text, the input's `id`, `name`, `type`, `autocomplete` token and any `maxlength` or current value, and, on a redisplay, the error sentence. Link label and input with `for`/`id`; the label is always visible, and a placeholder never replaces it.

- Label: `body` style, `ink`, `space-1` above the input. Title Case in the markup ("First Name").
- Input: `body` style text in `ink` on `surface`, 1px (`border-hairline`) `line-strong` border, `space-2` × `space-3` padding, `radius-none`. Full width on a phone, at most `field-width` (20rem) from the `sm` breakpoint up.
- Focus: 2px solid `focus-ring` outline with a 2px offset, replacing Bootstrap's focus shadow. Never removed, error state included.

## Layouts

- **Row** (`.pc-form`), for a search: label-over-input fields and the button sit on one row, bottom-aligned, from `sm` (576px) up and stack below it, `space-3` apart, with `space-5` below the form. A search form is `<form method="get" role="search">` and works without JavaScript.
- **Stacked** (`.pc-form .pc-form-stacked`), for a record form such as New Owner and Edit Owner: one field per row, `space-3` apart, the Button below the last field and left-aligned. The whole form is at most `field-width` wide from `sm` up, full width below. Start it with a helper line in `ink-muted` when every field is required: "All fields are required." DOM order is tab order.

## Error state (`.pc-field-invalid`)

- The input's border grows to `border-strong` (2px) `line-strong`; padding drops by 1px so nothing shifts.
- Under the input, one sentence in `body` style, `ink` (13.1:1), with a `border-accent` (4px) `line-strong` rule on its left and `space-3` indent. It names the field so it makes sense on its own: "First Name is required.", "Telephone must be exactly 10 digits.", "City must be 80 characters or fewer."
- The input gets `aria-invalid="true"` and `aria-describedby` pointing at the error's `id` (`firstName-error`). Keep what the person typed (trimmed) in the input.
- One message per field; when a value is both empty and malformed, show only "… is required."
- Pair it with a form-level summary above the form, an InlineMessage with `role="alert"`: "Owner not saved: 2 fields need attention."
- The words, the heavier border and the rule carry the meaning. The system has no error colour and doesn't add one.

In the Thymeleaf app this is Bootstrap's `form-label` and `form-control` with `pc-` classes in `petclinic.css` and `--bs-border-radius: 0`; errors are `th:errors` in a `<p class="pc-field-error">`. Turn native browser bubbles off with `novalidate` so server messages are the one mechanism; keep `required` and `aria-required="true"` for assistive tech.

Don't: round the corners, use `spring-green` for the border or focus ring, put the label inside the input, colour the error text or border red, or show validation by colour alone.
