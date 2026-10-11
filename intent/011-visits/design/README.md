# Visits design boards

Source of the design canvas https://claude.ai/artifact/Y7KKkoDrH7QxCJYEpB4Wqf, built with the Spring PetClinic design system (`docs/design-system/`) for spec §4.6. The `.dc.html` files render inside that canvas (they load its `support.js` and the logo by its `/_blob/` id), not standalone; `canvas.json` is the canvas layout.

| Board | Shows |
|---|---|
| `Main.dc.html` | Owner Information after a save: "Your visit has been booked", Edit Pet then Add Visit in the actions cell, the Visits section with one `h4` and VisitList per pet |
| `VisitForm.dc.html` | New Visit, blank (Max): context block, Date defaulting to today, Description, Previous Visits |
| `VisitFormErrors.dc.html` | New Visit, both fields failed (Samantha): summary, field errors, `aria-invalid` |
| `VisitFormDateError.dc.html` | New Visit, date format error with the description kept, pet without visits (Leo) |
| `PhoneOwner.dc.html` | Owner Information at 320px: collapsed navbar, stacked DetailList, pets table scrolling in its region, wrapping descriptions |
| `PhoneVisitForm.dc.html` | New Visit with errors at 320px |

On the desktop boards the submit button is drawn as a link so the canvas can play the flow; the app uses `<button type="submit">`, as the 320px board does.

Awaiting design-owner review (spec concern 5).
