# Button

The primary button: a square `spring-green-dark` block with an UPPERCASE `on-dark` label, used to submit a form.

**The consumer provides** the label in Title Case ("Find Owner") and the `type` (`submit` in a form). CSS sets it UPPERCASE; never type the label in capitals.

- Label: `nav-item` style (Montserrat 14px/600, 0.08em, UPPERCASE) with a 24px line so the button matches the input's height, `on-dark` on `spring-green-dark` (5.8:1).
- Box: `space-2` × `space-4` padding, a 1px border in its own fill colour, `radius-none`, no shadow.
- Hover: `spring-brown` fill and border (13.1:1).
- Focus: 2px solid `focus-ring` outline with a 2px offset, replacing Bootstrap's focus shadow.
- Next to a FormField, place both in `.pc-form` (see FormField) so the button lines up with the bottom of the input.
- In the Thymeleaf app this is Bootstrap's `btn` with a `pc-button` class in `petclinic.css`.

This is the system's only button style. Don't: round it, add an icon or shadow, use `spring-green` (white on it is 2.6:1), or make a link look like a button.
