# Iteration 004b — Competition presentation

Status: scope captured from the owner interview. **004b is a provisional name**
for this presentation follow-up to [004a](004a-competitions-run-brief.md).
Implementation has not started.

## Purpose

Competition functionality works. Improve the cluttered management page and stand
scoreboard for use at the event, preserving the existing competition behavior.
The standings and event display are **one page**, combining scores and joining QR
code on a large monitor.

## Management page

- Keep the normal narrow app layout.
- Emphasize the competition's current status and next action.
- Tuck editing controls away rather than mixing them with status information.
  Choose the simplest suitable interaction during implementation.
- Keep enrolled participant names visible and tidy their presentation; no new
  participant-list disclosure or separate view is required.
- Provide a prominent **Open event display** button opening the combined display
  in a separate tab, so management can continue independently.

## Combined event display

- Fill the browser window, using the available width and height rather than the
  normal narrow app column. The operator handles browser full-screen mode;
  no in-app full-screen control is needed.
- Show the competition name and standings alongside a joining area containing
  the competition QR code, **Scan to join**, and closing time. Keep this area
  minimal; do not add rules or a joining explanation.
- Use the same layout before and during play. Before starting, show enrolled
  players with zero points.
- Each scoreboard row shows **rank, player name, points, and games completed out
  of allowance**. Do not add average response time to the rows; existing ranking
  rules, including the speed tiebreak and shared ranks, continue to apply.
- Show as many highest-ranked players as fit comfortably for the monitor size,
  with large text readable from a distance. Do not shrink the whole field to fit,
  rotate pages, or automatically scroll through lower-ranked players.
- At closing, remove the QR/joining area, show that the competition has finished,
  and give the final standings the extra space. Preserve the distinction between
  results being finalized and confirmed final results.
- Preserve automatic standings refresh and honest refresh-error/stale-result
  handling. This iteration changes presentation rather than competition rules.

## Visual guidance

Keep the current visual style. Follow the frontend
[design system](../../qubigo-frontend/doc/design-system.md) and consult its
[prototype](../../qubigo-frontend/doc/prototype/index.html) where appropriate:
dark canvas, Nunito typography, existing blue accent, tokens, cards and button
patterns. The event display deliberately extends beyond the normal narrow layout;
update the design-system guidance to describe this exception when implementing.

## Scope boundaries

- The **phone joining page stays as it is**, including its rules, join action and
  joined/progress presentation. Earlier interview ideas about simplifying it were
  superseded by the owner's decision to leave it for another iteration.
- All other pages retain their existing smaller layout.
- No new competition rules, scoring, matchmaking, permissions or lifecycle
  behavior. The event display retains existing admin access restrictions.
- No public leaderboard, new full-screen browser API, branding redesign,
  publishing or app-store release work.
- Deferrals in the [competition specification](004-competitions.md) remain deferred.

## Acceptance examples

1. An admin sees the status and next action clearly on the compact management
   page, with participant names visible and editing controls tucked away.
2. Open event display opens a separate tab. On a large monitor the combined
   standings and QR display uses the browser window rather than a phone-width
   column; the management tab remains usable.
3. Before starting, enrolled players appear at zero points beside the QR code.
   During play, the same layout shows updated standings and completed/allowed games.
4. With more participants than fit, the display keeps readable leading rows
   visible without automatic scrolling or rotation. Available space determines
   the number of rows, rather than a fixed top ten.
5. At closing, the QR area disappears and standings expand. Provisional results
   are not labelled final before finalization, including after a failed refresh.
6. Scanning the QR still reaches the existing phone joining journey unchanged.
   Other app pages retain their normal layout and existing functionality.

Exact spacing, row capacity and the arrangement of the QR area are implementation
choices guided by readability and the existing design system.
