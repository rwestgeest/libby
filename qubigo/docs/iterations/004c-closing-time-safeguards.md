# Iteration 004c — Closing-time safeguards

Status: scope agreed with the owner on 2026-10-20. Implementation has not started.

## Why

While testing 004b, the competition "Ypelaer 2" was saved with closing time
**2926**-10-19 instead of 2026-10-19 (a year typo in the date field). It was
opened and started, so it stayed "running", and starting the next competition
was refused with "Another competition is running". Automatic closure worked
as designed; the gaps were:

- the closing time accepts implausible values;
- the admin cannot see the mistake before opening;
- closing time is fixed after opening, so there is no in-app way to recover.

The data was repaired by hand. This iteration prevents a repeat at
Spellenspektakel (7–8 November 2026). The [competition specification](004-competitions.md)
remains the full product requirement; this brief adds two owner decisions.

## Owner decisions

1. **Plausible closing time.** A closing time must be in the future and at
   most **14 days** after the moment it is checked. Enforce this on the
   backend when a draft's settings are saved and when a competition is opened
   for joining, and show the same rule as a field error in the form. Existing
   stored competitions are not migrated or revalidated.
2. **Close now.** An admin may end a **running** (started, not finalized)
   competition early. Closing now has exactly the effect of automatic closure
   at that moment: results freeze, whoever has the turn in an unfinished game
   loses (0/3), pending invitations can no longer create competition games,
   matchmaking stops, ordinary play continues. The action asks for
   confirmation and cannot be undone; reopening remains out of scope. This
   amends the specification's "closure happens automatically at the configured
   time" and "closing time is fixed" rules for this one admin action only.

## Acceptance examples

- Saving or opening with a closing time in the past, or more than 14 days
  ahead (e.g. year 2926), is refused with a clear message, in the UI and on the
  backend. Exactly 14 days ahead is accepted.
- Before opening, the management page shows the closing time with full date
  including the year, in the device timezone, so a typo is visible.
- An admin closes a running competition now: after confirmation it is
  finalized with the same results automatic closure would give at that moment,
  and a new competition can then be started.
- A move admitted after the close-now moment does not change competition
  results; ordering against close-now uses the same protected interval as
  automatic closure.
- Non-admins cannot close a competition (backend enforced).
- Close now is not offered for draft, open-but-not-started or finalized
  competitions.

## Planning and implementation

- Follow the `careful-plan` line. The design walkthrough should only cover how
  close-now fits the existing closure design (reuse `CloseCompetitions` /
  `Competition` finalization rather than a second closure path).
- Follow backend guidance and ADRs 0002, 0005, 0007 and 0008. The 14-day rule
  belongs with the domain (closing time is a validated domain input), with
  the clock supplied, not read inside the aggregate.
- Test-drive the rules; reuse the existing closure test fixtures.
- Keep 004b's management layout: close now belongs with the tucked-away
  controls or next-action area, not as a prominent default action.

## Out of scope

- Event-display QR code (004d), form alignment and design-system patterns
  (004e), competitions overview (004f) — separate chunks.
- Closing an open competition that never started; extending or editing the
  closing time after opening; reopening.
- Admin CLI (planned after the overview).
