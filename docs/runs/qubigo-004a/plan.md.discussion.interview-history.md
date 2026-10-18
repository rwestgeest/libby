# Design discussion: qubigo-004a / admin creates and joins a competition
Status: open
Run: qubigo-004a
Plan: /home/rob/dev/libby/factory/runs/qubigo-004a/plan.md
Target: /home/rob/dev/qubigo
Seed: /home/rob/dev/libby/q/docs/iterations/004a-competitions-run-brief.md

## Capability and sketch

Proposed journey for owner review: an authenticated admin opens an admin-only competition list, creates a competition with display name, closing date/time, per-participant game allowance and advisory minimum, then reaches its details. Details show the rules summary, allowance, closing time and explicit Join action. Creation or viewing does not enroll the admin. Joining persists their own membership; reload shows “Joined — waiting for manual start.” Creation immediately makes enrollment available to admins; no draft, edit or start action in this slice. All competition reads and writes require ADMIN on the backend as well as in the UI.

Conceptual domain events → reacting policies → owning contexts (not a commitment to a message bus or event storage):
- CompetitionCreated → make the created competition available for admin discovery/details → Competitions.
- ParticipantJoined → show membership confirmation/waiting state → Competitions. No matchmaking or game creation reaction yet.
- Existing authenticated identity/ADMIN authorization → admit or deny access; supply the current participant identity → existing Users/security boundary, consumed by Competitions.

Proposed new cross-context dependency: Competitions uses the existing current-user identity/security capability. It owns competition configuration and membership, not user accounts. No new Games, Invitations, Notifications or Onboarding dependency in this increment. Later outline remains public enrollment/onboarding → manual start and matching/invitations → game-result policy and closure/ranking → reminders and player/stand views. A later Games event policy may update Competitions without Games depending on Competitions; not decided here.

Architecture check: read backend AGENTS and ADRs 0002/0005/0007/0008. Traced GameDto.asValidMoveParameters → GamesController.postMakeMove → typed MakeMoveParameters → MakeMove loading game/current user → Game.move/withMove immutable rules → SqliteBasedGameRepository event storage. Position/Piece conversion failures are field-specific; Name construction enforces its own validity but its user-name restrictions are not automatically competition-name requirements. Proposed competition path follows those responsibilities: DTO conversion constructs valid domain values, commands coordinate, immutable Competition behavior owns creation/join rules (including relevant time/membership facts), repository adapters map/store. No SQL-owned use case or command-to-command wrapper is proposed. This does not prescribe event sourcing for Competitions.

Simplest adequate approach: persist competition configuration and membership, read them on navigation/reload, and add no background workers or cross-context event delivery. Candidate guarantees beyond explicit seed requirements, all awaiting owner choice:
- Repeat Join yields the same membership, not an error or duplicate. Reason: ordinary repeated clicks/reload recovery. Omission: caller may see an already-joined failure. Cost: small domain/persistence uniqueness handling; not a general exactly-once protocol.
- Reject creation with an already-past closing time and reject joining at/after closing. Reason: avoid an unusable new competition or enrollment after its advertised deadline. Omission: inert/past-deadline membership can be recorded. Cost: injected time plus boundary behavior tests; no automatic closure machinery now.
- No duplicate-create retry guarantee, durable event delivery, concurrent workflow orchestration, auto-refresh or audit system proposed. Omission can leave duplicate competitions after an ambiguous create retry; nothing in this slice requires additional machinery. Adding such guarantees increases storage/protocol/testing costs and requires a separate owner choice.

## Open questions

- Latest scope change: first-slice joining is available to regular users through a shared QR link, superseding admin-only joining. Clarify whether anyone signed in who obtains the link may join (including a forwarded link), and scope link/authentication flow before planning. Ordinary users should only discover joined competitions through normal listings.

- Owner’s latest direction supersedes immediate opening: creating a competition makes a **draft**, followed by a separate action to open joining. Settle the action name, permitted changes while draft, and first-slice journey before detailed planning. Questions 1–3 below retain original proposals and must be reconciled with this decision.

1. Owner: approve or amend the journey and context sketch above, including immediate admin enrollment availability, no auto-join on create, persistent membership confirmation and the sole new Users/security dependency? This initial review is required even if no extra guarantee is wanted.
2. Owner: settle creation input semantics. Suggested minimum: nonblank display name (no inherited user-name character restrictions), positive whole-number allowance, advisory minimum of at least two, explicit timezone/offset for closing (UI displays the local timezone). No arbitrary business upper caps or unique-name rule proposed. Approve or amend, including whether past-time creation and joining at/after closing are rejected. These choices do not authorize a closure scheduler or start workflow.
3. Owner: should repeat Join succeed with existing membership (recommended), or return an already-joined failure? Approve the stated omission of duplicate-create retry guarantees and other reliability machinery, or identify a concrete guarantee needed now.

## Decisions

- 2026-10-18 — First-slice authentication scope: owner deferred competition-specific sign-in/registration return flows and onboarding retention. The first-slice joining journey is for people already signed in who follow the competition link. Signed-out link handling is not settled by this deferral; do not expand this slice into the full public onboarding journey.

- 2026-10-18 — QR-link access: owner agreed that **any signed-in person who obtains the competition link may join**, including recipients of a forwarded link, subject to the agreed joining lifecycle/deadline rules. No recipient allowlist or forwarding restriction is required.

- 2026-10-18 — Owner superseded the seed’s admin-only release boundary: regular users can join through a competition QR link, shared with people the organizers know. Regular users do not discover competitions they have not joined through ordinary competition listings. Joining does not require the admin role; organizer management still uses that role. The intended QR-link page necessarily supports seeing the agreed pre-join information, but exact link access/forwarding behavior and onboarding scope need clarification. Controlled QR distribution is the stated release approach; no allowlist, secret-token guarantee or additional authorization mechanism has been agreed.

- 2026-10-18 — First-slice joining presentation: owner agreed to show **competition name, rules summary, games per participant and closing time** before explicit joining. After joining, show **“You’ve joined — waiting for the competition to start.”** This confirmation applies to the first slice, which does not implement starting or running play.

- 2026-10-18 — Organizer participation: owner agreed that an organizer must **join explicitly** to become a Participant. Creating or opening a Competition does not automatically join its organizer.

- 2026-10-18 — Closing-time entry: owner agreed that organizers enter closing time in their **device’s local timezone**, with the timezone clearly shown. Conversion must retain an unambiguous deadline; no timezone-selection workflow was requested.

- 2026-10-18 — CompetitionName normalization: owner specified that **surrounding spaces are trimmed**. Internal spaces remain permitted. Owner subsequently agreed that an empty name after trimming is rejected. Together, CompetitionName trims surrounding spaces, requires a nonempty result, and permits only A–Z, a–z, 0–9 and ordinary internal spaces.

- 2026-10-18 — Competition-name uniqueness: owner agreed that different Competitions may have the **same name**; CompetitionName has no uniqueness requirement.

- 2026-10-18 — Competition name: owner requires a **CompetitionName domain primitive** enforcing only alphanumeric characters and spaces. This supersedes the proposed unrestricted nonblank display name. Owner subsequently specified ASCII letters only: **A–Z, a–z**, alongside **0–9** and ordinary spaces; accented/non-ASCII letters are excluded. Owner subsequently confirmed duplicate competition names are allowed.

- 2026-10-18 — Numeric input validation: owner agreed that **games per participant** must be a whole number of at least **1**, and **suggested minimum participants** must be a whole number of at least **2**. No upper limits were decided.

- 2026-10-18 — Joining deadline: owner agreed that new joining is refused **at or after closing time**, including when the Competition was never started. This settles joining deadline behavior without prescribing a closure scheduler.

- 2026-10-18 — Repeated joining: owner agreed that joining again succeeds with the existing participation and shows **“You’ve joined”**, without duplicate participation or an already-joined error. This settles the repeat-Join part of original question 3; duplicate-create retries and other proposed reliability guarantees remain unapproved.

- 2026-10-18 — Opening validation: owner agreed to refuse **open for joining** unless the closing time is in the future; the organizer must update an elapsed closing time while still in draft. This decision applies to opening, not a separate prohibition on saving past-time drafts.

- 2026-10-18 — Draft completeness: all four settings (name, closing time, games per participant, suggested minimum participants) must be filled in before saving a draft. Incomplete drafts are not part of this slice. Exact value validation remains to be settled.

- 2026-10-18 — Organizer access: keep the first slice simple with shared organizer access to drafts and their agreed management actions, using the existing admin role. No creator-only access rule or organizer assignment is needed now. Additional access rules and co-organizership are deferred by the owner.

- 2026-10-18 — Post-opening editing: owner agreed that the organizer may still change the **name** and **suggested minimum participants** after opening for joining, to keep administration flexible. **Closing time** and **games per participant** remain fixed once joining is possible. Editing behavior in later lifecycle stages has not been separately discussed.

- 2026-10-18 — Draft behavior: the organizer may change all four creation settings while the Competition is a **draft**: name, closing time, games per participant, and suggested minimum participants. Post-opening changes to name and suggested minimum participants were subsequently agreed; existing specification fixes closing time and games per participant once joining is possible.

- 2026-10-18 — Ubiquitous language: **open for joining** is the organizer’s action that takes a Competition out of **draft** and makes joining possible. Opening does not start play. Draft editing is agreed for all four creation settings; name and suggested minimum participants remain editable after opening.

- 2026-10-18 — Owner changed the proposed first-slice journey: creation produces a **draft**; opening for joining is a separate action. This supersedes the proposal of immediate joining availability on creation and extends the first-slice discussion to drafting/opening. Draft editing rules and the name of the opening action remain unresolved. Earlier proposals and seed deferrals are retained as historical evidence, not current owner approval.
- 2026-10-18 — Later-capability clarification: the **competition invites** the matched players, but only the participant selected to move first receives the actionable invitation (Harry in the owner’s example). That player starts the game and makes the first move; time from game start to that move counts toward speed. Separate competition invitations/presentation are acceptable to explore, not a settled implementation choice. Invitation behavior remains outside the first creation/joining slice; reconcile timing with existing game creation when discussing that later capability.

- 2026-10-18 — Ubiquitous language: **matching** selects two Participants to play against each other; they play the Game as Players. Owner agreed this term without prescribing a matching algorithm. Matching by wins or speed is an idea for later discussion, not an agreed requirement or a replacement for the existing specification’s matching rules.

- 2026-10-18 — Ubiquitous language: competition progress is **not started → running → closed**. **Open for joining** describes joining availability separately: a competition can be open for joining before starting and while running, until closing time. Owner accepted this distinction and terminology. This does not prescribe separate stored state fields.

- 2026-10-18 — Ubiquitous language: **suggested minimum participants** is the agreed term for the advisory participant count. The organizer may start with fewer; reaching this count does not start the competition automatically.

- 2026-10-18 — Ubiquitous language: **closing time** is the agreed term for the configured competition deadline.

- 2026-10-18 — Ubiquitous language: **Organizer** is the competition role of the person who creates and starts a competition. A person with the existing **admin** role can act as an organizer; the domain term and authorization role are distinct. This naming decision does not introduce organizer assignment or a new permission system.

- 2026-10-18 — Ubiquitous language: a person **joins** a Competition and becomes a Participant; **joining** is the agreed action term.

- 2026-10-18 — Ubiquitous language: a person in a **Competition** is a **Participant**; a person in a **Game** is a **Player**. These names express the person’s role in each context.

- 2026-10-18 — Ubiquitous-language interview: owner confirmed **Competition** as the domain noun.
- 2026-10-18 — Owner rejected **game allowance** as misleading: the competition creates or arranges invitations for the expected number of games per participant; the emphasis is not permission to play a maximum. The owner subsequently agreed **games per participant** as the term: “This competition has five games per participant.” Use it instead of “game allowance” in subsequent planning and product language; historical evidence above retains the original wording.
- 2026-10-18 — Owner clarified that playing fewer than the configured number is fine: these are simply the games the participant managed to play. Fewer games mean fewer opportunities to score and therefore probably less chance of winning; there is no obligation to complete an expected programme. This clarifies meaning without approving the other proposed journey, validation or reliability choices.

Intended decision scope: the complete first admin-only creation/details/explicit-joining capability, including its UI, input conversion, aggregate behavior, persistence, authorization and tests. Routine implementation tasks within settled scope reuse these decisions. Public onboarding, start/matching, game policies, closure/scoring, reminders and stand display remain outside this discussion's approval scope.

## Handoff

Current interview focus: first-slice drafting, opening and joining. Opening action terminology is settled: **open for joining**. Draft editing is settled for all four settings. Post-opening editing is settled: name and suggested minimum participants remain editable; closing time and games per participant are fixed. Draft access is shared among organizers through the existing admin role; ownership restrictions and co-organizership are deferred. Draft completeness is settled: all four settings are required to save. Opening requires a future closing time. Repeated joining is settled: retain existing participation and show “You’ve joined”. Joining deadline is settled: no new joining at or after closing time, even if never started. Numeric minima are settled: games per participant ≥1 and suggested minimum participants ≥2, both whole numbers. CompetitionName must be a domain primitive accepting only alphanumeric characters and spaces. Allowed name characters are settled: A–Z, a–z, 0–9 and ordinary spaces. Duplicate competition names are allowed. Surrounding spaces are trimmed. Empty names after trimming are rejected. Closing-time entry uses the organizer’s device-local timezone, clearly shown. Organizer participation requires explicit joining; creation/opening does not join them. Pre-join information and post-join confirmation are settled. Latest owner direction replaces admin-only joining with QR-link joining for regular users and joined-only ordinary discovery. Forwarded-link joining is allowed for signed-in users. Competition-specific authentication/onboarding return flows are deferred; scope joining to already signed-in users. Reconcile capability sketch/open questions with the agreed first slice before further elaboration. Persistence/context sketch and omission of extra reliability machinery still need scoped review. Reconcile original open questions before resolution. Ubiquitous-language decisions do not constitute blanket approval of the original journey or validation/reliability proposals.

Blocked pending repository/product owner review and answers to questions 1–3 at this exact path. Plan initializer findings were read from plan.md and record.jsonl: initial design checkpoint required, no owner decisions. No planner/reviewer findings sidecars or prior discussion existed in the run directory; supplied previous validator findings were empty.

Run identification: directory qubigo-004a; settings.json selects careful-plan and the seed above, but its target is /home/rob/dev/libby/q. Actual live target is /home/rob/dev/qubigo on feature/004a-competitions; no settings change or product relocation inferred. Product work must remain in the live target. Only this discussion was created; plan and product files are unchanged. No implementation, acceptance, completion, commits or tests performed.

From Libby's root: Pick up the open design discussion for run qubigo-004a.
