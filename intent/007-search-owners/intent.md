# Intent: Search Owners
Author: matthew-js-porter

## Problem
Visitors need to be able to search for owners. Intent 006 added a Find Owners page, but it only lists every owner, 5 per page. To find one owner, a visitor has to page through the whole list. That already takes effort with the 10 seeded owners and gets worse as the clinic grows. Upstream Spring PetClinic's Find Owners page is built around a last-name search; ours leaves it out.

## Proposed outcome
A visitor on the Find Owners page can type a last name and see only the owners whose last name starts with it.

Observable results:
- The project builds and all tests pass (`./mvnw clean verify`).
- The Find Owners page has a labelled last-name search field and a "Find Owner" button above the owners list.
- Searching "Davis" lists Betty Davis and Harold Davis and no one else. The search matches the start of the last name, as upstream does ("Dav" finds both), and ignores case.
- A blank search lists all owners, as the page does today.
- A search with no matches shows a clear "not found" message near the search field instead of an empty table.
- A single match is shown in the list like any other result. It does not redirect anywhere.
- Results are paginated like the current list (5 per page), and the pagination links keep the search term.
- The search term stays in the field after searching, and a search can be bookmarked or shared as a URL.
- The owners JSON endpoint accepts the same last-name filter and returns only the matching owners. With no filter it returns all owners, as today.
- The page follows the design system in `docs/design-system/`, is accessible (labelled field, visible focus), and works at phone width.
- The vets page, vets JSON, homepage, navbar and `/actuator/health` behave as before.

Out of scope: an owner details page and the upstream redirect to it on a single match; a separate search form page; searching by first name, city, telephone or pet; adding or editing owners; internationalisation; security/login.

## Affected users and systems
- Clinic visitors using Find Owners.
- The owners page and owners JSON endpoint from intent 006.
- Owner lookup in the database (filtering by last name).
- The design system, if it lacks a form field or button component.
- Page and JSON tests.

## Constraints
- Server-side rendering with Thymeleaf, in the existing shared layout; the page must work without JavaScript.
- Persistence through the existing Spring Data JDBC + H2 foundation; no JPA.
- Follow the design system. Any new component or token (form input, button, inline message) goes into `docs/design-system/` in the same PR.
- Match upstream search behaviour where practical; record deviations (no redirect on a single match, search on the list page) in the spec.
- Config stays YAML only; actuator surface unchanged.

## Open questions
- How does the JSON filter appear (a query parameter on the existing endpoint, or something else)? Suggested default: the same `lastName` query parameter as the page. Decide in the spec. Owner: Matthew.
- Should the "not found" case keep showing an empty table, or show only the message? Suggested default: only the message. Owner: Matthew.
