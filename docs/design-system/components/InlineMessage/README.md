# InlineMessage

A one-sentence message shown in place of a result, such as an empty search: plain `ink` body text with a 4px `line-strong` rule on its left.

**The consumer provides** the sentence, with any text the person typed escaped and in straight double quotes: `No owners found with a last name starting with "Dav".` Give it `role="status"` so screen readers announce it, and place it directly under the form it answers.

- Text: `body` style, `ink`, `space-2` × `space-4` padding, `space-5` below.
- Rule: `border-accent` (4px) in `line-strong` on the left. No fill, icon, card or shadow.
- The words carry the meaning; there is no colour-coded success, warning or error variant.

Don't: use it for validation errors on a field, add a close button, or colour the rule green or red.
