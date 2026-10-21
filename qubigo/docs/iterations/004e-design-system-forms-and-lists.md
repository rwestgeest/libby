# Iteration 004e — Design system: forms and lists

Status: scope agreed with the owner on 2026-10-21. Implementation has not
started. Start after 004c is merged: both change
`competition-draft-page.js`.

## Why

Testing 004b showed the competition management form fields are not aligned.
The design system has no pattern for admin forms or for listing items, and
[004f](../backlog.md) (admin overview of all competitions grouped by state)
needs a list pattern.

Source inspection (2026-10-21, `main`):

- `competition-draft-page.js` renders bare `<label>`, `<input>` and
  `<p class="error-message">` triples directly in `<form>`, twice (new draft
  and inline edit). They bypass `<input-field>`, `.inputFields` and
  `.buttons`, so fields get browser defaults. The saved view uses a raw
  `<dl>`, a bare participants `<ul>`, and buttons and links in one
  unstructured stream, including a legacy `class="button"` Cancel.
- `<input-field>` cannot host this form yet. It has no `min`, `step` or
  `readonly`, and its label is `for="${id}"` while the input is
  `${id}-input`, so the label is never associated. Its `label` attribute
  renders unstyled.
- `doc/design-system.md` covers onboarding inputs (§4.2) and cards and list
  rows (§4.5), but no labelled fields, hints, read-only fields or grouped
  lists. The prototype's competition screens predate 004b and are
  flagged as drift.

## Owner decisions

1. **Forms look like the login and onboarding forms.** Admin forms use
   `<input-field>` inside `.inputFields`, with actions as `accent-pill` /
   `outline-pill` in `.buttons`, the same surface, focus and spacing as sign
   in. One difference: competition fields carry a **visible label**, because
   edit mode pre-fills values (hiding placeholders) and `datetime-local` /
   `number` inputs do not show placeholders. The label sits above the field
   as a small lightgrey caption. Onboarding forms are not changed.
2. **Extend `<input-field>`, no second input style.** Add
   `min`, `step`, `readonly`, an optional hint (e.g. the device timezone note)
   and a field-error slot wired through `aria-describedby`; fix the label
   association. The value must stay readable via `FormData` and
   `form.elements`, which the draft page relies on. Read-only fields look
   visibly fixed (no focus border, muted text) and stay readable.
3. **One list pattern: grouped card list.** A section heading per group
   (the existing `.list > .heading + .listContent`) and card rows with
   title, one secondary line and a status badge (§4.5, §4.6), with an
   empty-group message. Extract it as one shared primitive rather than a
   fifth copy of the card CSS. This is the 004f overview's pattern (draft,
   open, running, closed, closed without starting).
4. **Apply to:** the competition form (new and edit, including the edit
   form's actions as pills in `.buttons`), the participants list on the
   management page, and the player's "My competitions" list. Tables and the
   event display are deferred.

## Acceptance examples

- On a phone-width screen, new and edit competition forms show each label,
  field, hint and error left-aligned in one column with even spacing,
  visually matching the sign-in form's fields and buttons.
- Clicking a label focuses its field; field errors (including 004c's
  closing-time error) appear under their field and are announced.
- After opening, closing time and games per participant are visibly
  read-only and still submitted unchanged.
- Inputs render at 16px or larger (no iOS zoom on focus).
- The edit form's Save and Cancel are `accent-pill` / `outline-pill` in
  `.buttons`; no legacy `class="button"` remains in it.
- Participants on the management page and the player's "My competitions"
  appear as card rows; an empty list shows its empty message.
- The built screens match `doc/design-system.md` §4.2a (labelled field),
  §4.5 (grouped card list) and §4.6 (competition badges), which specify the
  agreed details, and the prototype's 004e sketch screens. The
  **Planned (004e)** markers are removed, and the §6.2 drift entries these
  resolve are removed or updated. Any deviation is agreed with the owner and
  recorded in the document, not left as a silent difference.

## Planning and implementation

- Follow the `careful-plan` line. The design walkthrough should cover the
  `<input-field>` extension (attributes, error/hint wiring, compatibility
  with onboarding pages and their tests) and where the shared card list CSS
  lives. It is not a redesign.
- The design system is the specification (owner decision 2026-10-21): the
  details were agreed in the prototype sketch (commit `85a59aac`) and written
  into the document before this run. Follow the frontend `AGENTS.md` and §7
  of the design system. Use tokens; add a token rather than a new hard-coded
  grey (the sketch's `#B8B8B8` and `#4D4D4D` are candidates).
- Test-drive behaviour (label association, read-only submission, error
  placement) with the existing page and component tests. Keep ids that tests
  and 004c/004d rely on.
- Verify visually at phone width before closing.

## Out of scope

- Competitions overview page and its admin list endpoint (004f); this
  iteration only provides the pattern and a prototype mock.
- Data table pattern, standings tables (stand page, event display) and
  moving the event display's inline styles onto tokens (it still uses
  non-existent `--bg-canvas` / `--accent` with off-palette fallbacks); a
  later iteration.
- Restructuring the rest of the saved competition page (settings `<dl>`,
  action layout).
- Restyling onboarding forms, the accept-invitation page or other §6 drift
  outside competitions.
- New competition rules, fields or copy changes beyond labels and hints.
- Admin CLI.
