# FormField

A labelled input, text, date or select: the label in `body` type above a square, `line-strong`-bordered control on `surface`, with an error state that is read with the field.

**The consumer provides** the label text, the control's `id`, `name`, `type`, `autocomplete` token and any `maxlength` or current value, any helper line, and, on a redisplay, the error sentence. Link label and control with `for`/`id`; the label is always visible, and a placeholder never replaces it.

- Label: `body` style, `ink`, `space-1` above the control. Title Case in the markup ("First Name", "Birth Date").
- Control: `body` style text in `ink` on `surface`, 1px (`border-hairline`) `line-strong` border (13.1:1), `space-2` × `space-3` padding, `radius-none`. Full width on a phone, at most `field-width` (20rem) from the `sm` breakpoint up.
- Focus: 2px solid `focus-ring` outline with a 2px offset (5.8:1); no focus shadow. Never removed, error state included.

## Variants

- **Text** (`<input type="text">`, also `tel` and `search`): the base control.
- **Select** (`<select class="pc-input">`): the same box, plus a small `ink` chevron at the right, `space-4` in, with the right padding widened to `space-3` + `space-5` so long options don't run under it. The chevron is drawn by the stylesheet itself (`appearance: none` plus a CSS background, no image request): no icon file, no custom list. The first option is an empty prompt, `<option value="">Select a type</option>`, selected until the person chooses; the options follow in name order, labelled as stored (`bird`, `cat`, `dog`, `hamster`, `lizard`, `snake`). The open list is the browser's own and is not styled; it keeps native keyboard and screen reader behaviour.
- **Date** (`<input type="date">`): the same box at the text input's height; the picker is the browser's. A helper line below it, "yyyy-mm-dd" (`.pc-field-help`, `small` style in `ink-muted`, 7.1:1), shows the format for browsers that fall back to a plain text box, and is tied to the input with `aria-describedby`. `max` may hint today's date to the picker where the date cannot be in the future (Birth Date); the server still checks. A date that may be any day (a visit's Date) has no `max` and may be pre-filled, with today on New Visit. Use `autocomplete="off"`.

The control box is square on every platform; native pickers and option lists are the operating system's and may be rounded. That is accepted rather than replaced with a scripted control.

## Layouts

- **Row** (`.pc-form`), for a search: label-over-input fields and the button sit on one row, bottom-aligned, from `sm` (576px) up and stack below it, `space-3` apart, with `space-5` below the form. A search form is `<form method="get" role="search">` and works without JavaScript.
- **Stacked** (`.pc-form .pc-form-stacked`), for a record form such as New Owner, Edit Owner, New Pet, Edit Pet and New Visit: one field per row, `space-3` apart, the Button below the last field and left-aligned. The whole form is at most `field-width` wide from `sm` up, full width below. Start it with a helper line in `ink-muted` when every field is required: "All fields are required." DOM order is tab order.

## Error state (`.pc-field-invalid`)

Applies to all three variants.

- The control's border grows to `border-strong` (2px) `line-strong`; padding drops by 1px so nothing shifts (the select keeps room for its chevron).
- Under the control (and under a helper line, if any), one sentence in `body` style, `ink` (13.1:1), with a `border-accent` (4px) `line-strong` rule on its left and `space-3` indent. It names the field so it makes sense on its own: "First Name is required.", "Birth Date cannot be in the future.", "Type is required.", "Date is required.", "Description is required."
- The control gets `aria-invalid="true"` and `aria-describedby` pointing at the error's `id` (`birthDate-error`), after the helper's id when there is one (`birthDate-help birthDate-error`). Keep what the person typed (trimmed) in the input, and keep the chosen option selected.
- One message per field: required beats format beats rule. A value that is empty shows only "… is required."
- Pair it with a form-level summary above the form, an InlineMessage with `role="alert"`: "Owner not saved: 2 fields need attention.", "Pet not saved: 3 fields need attention.", "Visit not saved: 2 fields need attention."
- The words, the heavier border and the rule carry the meaning. The system has no error colour and doesn't add one.

Markup: each field is a `.pc-field` holding a classless `<label>` and a `.pc-input` control (`width: 100%`, `display: block`), text, date and select alike; errors are `th:errors` in a `<p class="pc-field-error">`. Turn native browser bubbles off with `novalidate` so server messages are the one mechanism; keep `required` and `aria-required="true"` for assistive tech.

Don't: round the corners, use `spring-green` for the border or focus ring, put the label inside the input, replace the native select or date picker with a scripted one, colour the error text or border red, or show validation by colour alone.
