# Button

The primary button: a square `spring-green-dark` block with an UPPERCASE `on-dark` label, used to submit a form.

**The consumer provides** the label in Title Case and the `type` (`submit` in a form). CSS sets it UPPERCASE; never type the label in capitals. Labels in the app: "Find Owner" (search), "Add Owner" (New Owner form), "Update Owner" (Edit Owner form), "Add Pet" (New Pet form), "Update Pet" (Edit Pet form), "Add Visit" (New Visit form).

- Label: `nav-item` style (Montserrat 14px/600, 0.08em, UPPERCASE) with a 24px line so the button matches the input's height, `on-dark` on `spring-green-dark` (5.8:1).
- Box: `space-2` × `space-4` padding, a 1px border in its own fill colour, `radius-none`, no shadow.
- Hover: `spring-brown` fill and border (13.1:1).
- Focus: 2px solid `focus-ring` outline with a 2px offset, replacing Bootstrap's focus shadow.
- Next to a FormField in a row form, place both in `.pc-form` so the button lines up with the bottom of the input. In a stacked form it sits below the last field, left-aligned, at its natural width.
- In the Thymeleaf app this is Bootstrap's `btn` with a `pc-button` class in `petclinic.css`.

This is the system's only button style, and it submits forms only. An action that opens another page ("Add Owner" on Find Owners, "Edit Owner", "Add New Pet", "Edit Pet" and "Add Visit" on Owner Information) is an action link, not a Button: see Links in the main README. "Add Visit" and "Add Owner" are both: a link that opens the form, and the Button that submits it.

Don't: round it, add an icon or shadow, use `spring-green` (white on it is 2.6:1), or make a link look like a button.
