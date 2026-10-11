# InlineMessage

A one-sentence message about the result of the last action: an empty search, a saved record, or a form that was not saved. Plain `ink` body text with a 4px `line-strong` rule on its left.

**The consumer provides** the sentence and its role. Any text the person typed is escaped and in straight double quotes. There are three uses, all in the same style:

| Use | Where | Role | Example |
| --- | --- | --- | --- |
| Result | directly under the form it answers | `status` | `No owners found with a last name starting with "Dav".` |
| Confirmation | under the page title of the page a save redirected to, shown once | `status` | `New Owner Created` · `Owner Values Updated` · `New Pet has been Added` · `Pet details has been edited` · `Your visit has been booked` |
| Form summary | under the page title, above a form redisplayed with errors | `alert` | `Owner not saved: 2 fields need attention.` · `Pet not saved: 3 fields need attention.` · `Visit not saved: 2 fields need attention.` |

- Text: `body` style, `ink`, `space-2` × `space-4` padding, `space-5` below.
- Rule: `border-accent` (4px) in `line-strong` on the left. No fill, icon, card or shadow.
- The words carry the meaning; there is no colour-coded success, warning or error variant. A confirmation looks exactly like a result.
- A confirmation travels with the redirect (a flash attribute), never in the URL, and is gone on reload. The summary is announced by `role="alert"` without JavaScript; don't move focus to it.
- The pet and visit confirmations keep upstream's wording and casing exactly ("Pet details has been edited", "Your visit has been booked"), even though they differ from the owner ones; changing them needs the intent changed.
- Pluralise the summary: "1 field needs attention", "2 fields need attention".

Don't: use it for the error on a single field (that is the FormField error state), add a close button, or colour the rule green or red.
