# Iteration 004d — QR code on the event display

Status: scope agreed with the owner on 2026-10-20. Implementation has not started.

## Why

[004b](004b-competition-presentation.md) asked for a joining area on the
combined event display with the competition QR code, **Scan to join**, and
closing time. Testing showed the QR code is missing: the joining area renders
a grey placeholder box with the text "QR".

Source inspection (2026-10-20):

- `competition-event-display-page.js` `_renderJoiningArea` writes a
  `#qr-placeholder` box instead of a QR image.
- `app.js` constructs `CompetitionEventDisplayPage` without a
  `CompetitionSharing` instance, although the page's constructor accepts one.
- `CompetitionSharing` (`urlFor`, `qrFor`) already produces the joining URL and
  QR data URL, and the management page (`competition-draft-page.js`
  `renderSharing`) uses it successfully, including an error fallback.

This is a bug fix against 004b's acceptance examples; no product rules change.

## Scope

- Show a real, scannable QR code for the competition's joining URL in the
  event display's joining area, whenever 004b's presentation state shows the
  joining area (open and playing). Reuse `CompetitionSharing`; the QR encodes
  exactly the same URL as the management page's QR.
- Size the QR for scanning from a distance on a large monitor, consistent with
  the display's large text, without pushing standings rows out of view on a
  typical 1080p monitor.
- If QR generation fails, show the joining URL as readable text in the
  joining area instead of an empty or placeholder box.
- Automatic refresh must not flicker or regenerate the QR needlessly, and a
  stale asynchronous QR result must not appear after the display has moved
  on to the finished state (follow the `opening` guard pattern in
  `renderSharing`).
- At closing the joining area, including the QR, disappears as 004b specifies.

## Acceptance examples

1. For an open or running competition, the event display shows an `img` QR
   code (with alt text) encoding `<web app URL>/#/competitions/<id>`, next to
   **Scan to join** and the closing time. No `#qr-placeholder` remains.
2. Scanning it with a phone reaches the existing phone joining page unchanged.
3. When QR generation fails, the joining URL is shown as text.
4. After closing, the joining area and QR are gone and standings take the
   space; a QR generated late does not reappear.
5. Draft competitions and the management page are unaffected.

## Planning and implementation

- Use the `careful-plan` line. This is a routine, single-capability fix: no
  new scope, dependency or guarantee decisions are expected, so the design
  walkthrough should be brief.
- Follow the frontend `AGENTS.md`. Test-drive with the existing event-display
  page tests, injecting a fake `CompetitionSharing` as the management page
  tests do.
- Keep the change small. Do not restyle the event display or move its inline
  styles into the design system; that belongs to 004e.

## Out of scope

- Closing-time safeguards and close now (004c), design-system form and list
  patterns (004e), competitions overview (004f), admin CLI.
- Changing the joining URL, the phone joining page, or QR content.
