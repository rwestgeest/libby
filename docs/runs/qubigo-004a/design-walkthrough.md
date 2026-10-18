# Design discussion: qubigo-004a / draft, open and join a competition

Status: resolved
Run: qubigo-004a
Plan: factory/runs/qubigo-004a/plan.md (local run artifact)
Target: live Qubigo repository (local q symlink; qubigo/ in this sharing repository is a documentation snapshot)
Seed: [004a run brief](../../../qubigo/docs/iterations/004a-competitions-run-brief.md)

## Capability and sketch

The organizer creates a complete draft competition, edits it, then explicitly **opens it for joining**. An already signed-in person follows the competition link, sees the competition name, rules summary, games per participant and closing time, and explicitly joins. Show “You’ve joined — waiting for the competition to start.” Creating, opening or viewing never automatically joins anyone, including the organizer. Store competition configuration and participation so they survive navigation/reload.

Organizer management uses the existing admin role. Organizers share access to drafts and management actions. Regular users can join through the shared QR link, including forwarded links; ordinary competition listings show them only competitions they have joined. Drafts are not open for joining. Opening does not start play.

Conceptual collaboration (not a prescribed event transport or storage design):

- Organizer creates/edits draft → Competition owns settings and draft rules.
- Organizer opens for joining → Competition checks future closing time and makes joining possible.
- Person joins → Competition owns participation, deadline checks and repeat-join behavior → show participation confirmation.
- Existing Users/security supplies signed-in identity and admin authorization. Competition owns no user accounts. No new Games, Invitations, Notifications or onboarding integration is needed for this slice.

Implementation follows existing backend guidance: validated domain primitives at the input boundary, commands coordinating loading/persistence, immutable aggregate behavior owning state-dependent rules, and adapters mapping/storing. In particular, CompetitionName is a domain primitive. This is architectural guidance, not approval of a particular schema, event-sourcing scheme or messaging infrastructure.

## Open questions

No remaining product decision blocks planning the recorded first slice. The original questions are reconciled below. Unapproved extra guarantees must not be silently turned into requirements; bring genuinely new scope, dependencies or guarantees back for discussion.

Later-capability questions remain deferred: matching algorithm changes, invitation representation and game-start/timing semantics, onboarding/link retention, and editing during running/closed states. They do not expand this first slice.

## Decisions

All decisions below were supplied by the owner during the 2026-10-18 interview. The [original proposals and interview notes](plan.md.discussion.interview-history.md) preserve the evidence, including superseded wording.

### Ubiquitous language

| Term | Agreed meaning |
| --- | --- |
| Competition | The event participants join. |
| Organizer | Person managing the competition; existing admin authorization permits this role. |
| Participant | A person participating in a competition. |
| Player | A person playing a game. |
| Joining | The explicit action that makes someone a participant. |
| Games per participant | Number of games the competition arranges for each participant. Replaces “game allowance.” Playing fewer is fine: fewer games offer fewer chances to score, with no obligation to complete them. |
| Closing time | The configured competition deadline. |
| Suggested minimum participants | Advisory count; an organizer may start with fewer, and reaching it does not start play. |
| Draft | A created competition not yet opened for joining. |
| Open for joining | Organizer action and joining availability; distinct from whether play has started. |
| Not started → running → closed | Progress of play; joining can be open before and during running, until closing time. Drafting/opening adds a preparation distinction, not a prescribed storage model. |
| Matching | Selecting two participants to play against each other; they play as players. Algorithm details belong to later slices. |

### Drafting, opening and editing

- All four settings must be filled in to save a draft: name, closing time, games per participant, suggested minimum participants. No incomplete drafts.
- All four may be edited while draft.
- Opening for joining requires a closing time in the future. An elapsed draft deadline must be updated before opening. No separate requirement to reject saving past-time drafts was agreed.
- Once open for joining, closing time and games per participant are fixed. Name and suggested minimum participants remain editable.
- Shared organizer access is sufficient. Creator-only restrictions, additional access rules and co-organizership are deferred.

### Inputs

- CompetitionName trims surrounding spaces, rejects an empty result, and permits only A–Z, a–z, 0–9 and ordinary spaces. Different competitions may share a name.
- Games per participant is a whole number of at least 1.
- Suggested minimum participants is a whole number of at least 2.
- No business upper limits were agreed.
- Organizers enter closing time in their device’s local timezone, clearly shown. Represent the resulting deadline unambiguously; no separate timezone-selection workflow was requested.

### Joining and access

- Organizer management remains admin-authorized, but joining is not admin-only. This explicitly supersedes the seed’s initial admin-only release boundary.
- Organizers distribute the QR link to people they know. Any already signed-in person receiving it may join, including through forwarding. No recipient allowlist or forwarding restriction.
- Regular users do not discover unjoined competitions in ordinary listings; the direct link supports the agreed pre-join information.
- Joining is explicit, including for organizers. Before joining show name, rules summary, games per participant and closing time; afterward show the agreed confirmation.
- Repeated joining keeps the existing participation and shows “You’ve joined”; no duplicate participation or already-joined error.
- Refuse new joining at or after closing time, even if the competition never started. This requires deadline checks, not a closure scheduler for this slice.
- Competition-specific sign-in/registration return flows and onboarding retention are deferred. Scope this slice’s joining journey to already signed-in people; do not claim the signed-out journey is delivered.

### Later capabilities and guarantees

- Starting, matching, invitations, games, result policies, closure/scoring, reminders and player/stand views remain later work. The full iteration specification remains outstanding.
- The competition invites the matched players; only the participant selected to move first receives the actionable invitation. That player starts the game and makes the first move; game-start-to-first-move time contributes to speed. Reconcile these semantics with existing game creation when that capability is discussed.
- Separate competition invitations/presentation are acceptable to explore, not a settled implementation choice or a requirement to reuse existing invitations.
- Matching by wins or speed is a future possibility, not a change to the specification’s current matching rules.
- No duplicate-create retry guarantee, durable event delivery, workflow orchestration, auto-refresh or audit system was approved. These remain unrequested proposals, not first-slice acceptance requirements. Repeat joining and deadline checks are the specific guarantees agreed here.

### Reconciliation of the initial checkpoint

1. Journey: replace immediate admin-only creation/joining with draft → edit → open for joining → explicit joining through a link for signed-in users. Organizer authorization remains admin-based. Onboarding is explicitly deferred.
2. Inputs: replace unrestricted display name with CompetitionName and the agreed validation above. Time validation is required on opening and new joining; do not invent a prohibition on saving past-time drafts.
3. Repeat joining: agreed to retain participation successfully. The initial request to approve a package of speculative reliability machinery is not an outstanding product requirement; no such package was approved.

## Handoff

Ready for scoped planning of the first capability above. Resolution covers the recorded product decisions and explicit deferrals; it does not approve future design changes, extra infrastructure, implementation acceptance or iteration completion. Rewrite detailed planning around these decisions rather than the superseded initial sketch. Routine tasks within this scope can proceed; return to discussion for new scope, dependencies or guarantees.

Preserve the original run/plan/log evidence. settings.json’s q target resolves to the live Qubigo repository; no target mismatch or settings change is needed. Resume the existing run from Libby’s root with `bin/factory --run qubigo-004a`. The user will perform that action.

Only discussion documentation was changed in this session. No run status or plan edits, implementation, acceptance, commits, tests, or factory restart were performed.
