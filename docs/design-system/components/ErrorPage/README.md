# ErrorPage

The page a browser sees when a request fails: a heading, one plain sentence and two ways on, in the shared layout, with the failed request's HTTP status. It is a page pattern composed from Navbar, Footer and action links, not a new component. One template serves every status.

**The consumer provides** the HTTP status only. Heading and sentence are chosen from it; nothing else from the request or the error reaches the page.

| Status | `<title>` / `h2` | Sentence |
| --- | --- | --- |
| 404 | Page Not Found | We could not find the page you asked for. It may have moved, or the address may be mistyped. |
| 400 | Bad Request | The address or the details sent could not be understood. Check the link and try again. |
| 405 | Method Not Allowed | This page cannot be used in that way. Go back and use the links on the page instead. |
| 500 | Something Went Wrong | The clinic ran into a problem on its side. Try again in a moment. |
| other 4xx | Request Not Completed | The request could not be completed. Check the link and try again. |
| other 5xx, below 400, or none | Something Went Wrong | The clinic ran into a problem on its side. Try again in a moment. |

- Page: the shared layout with no navbar item current (no `aria-current` anywhere). One `page-title` `h2`, the heading from the table; the page title is "<heading> · Spring PetClinic", for example "Page Not Found · Spring PetClinic".
- Order under the title: the sentence as a `<p class="pc-error-text">` in `body` style, `ink` (not `ink-muted`: it is the page's content), `space-4` below; then a `<ul class="pc-error-actions">` with two action links (`.pc-action`), one per line, `space-3` apart: "Go to the Homepage" (`/`) and "Find Owners" (`/owners.html`, as in the navbar). The same two links for every status.
- One sentence per status, the same for every cause: an unknown owner, an unknown pet, a page past the last and a mistyped path all read "Page Not Found" and the same words, so the page never tells which records exist.
- Never on the page: the numeric status, the reason phrase, the request path or any input, an exception class or message, a stack trace or SQL. No `role="alert"`: it is a whole page, not a live update.
- At 320px the heading and sentence wrap and the links stack as they do on desktop; nothing scrolls sideways. The navbar collapses through its usual toggle.

No image or icon: the homepage photograph stays on the homepage (see Iconography in the main README), and an error page needs no decoration. No status-coloured heading or banner; the system has no error colour.

In the Thymeleaf app this is `templates/error.html`, picked up by Spring Boot for every status, calling the layout with `activeMenu` = `null` and reading only the `status` model attribute. JSON clients keep Boot's JSON error body; this pattern is for HTML only.

Not yet reviewed: the headings and sentences are new copy. These previews are the board for the design owner's review of the wording before the error page merges.
