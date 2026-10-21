# Qubigo Backlog

Cross-cutting feature work not yet assigned to an iteration. Items are ordered by rough priority (highest first). See `docs/iterations/` for scoped iteration plans.

---

## Short-term (next 1-2 iterations)

| Feature | Notes | Status |
|---|---|---|
| **Competition closing-time safeguards** | Closing time must be in the future and at most 14 days ahead; admin can close a running competition now. Prompted by a year-2926 typo that kept a competition running. See [iteration 004c](iterations/004c-closing-time-safeguards.md). | in progress (run qubigo-004c) |
| **Event display QR code** | The event display shows a placeholder instead of the joining QR code; wire in the existing QR generator. See [iteration 004d](iterations/004d-event-display-qr.md). | scoped (004d) |
| **Design system: forms and lists** | Add form-layout (labelled `<input-field>`, hints, errors, read-only) and grouped card list patterns to the design system and prototype; apply them to the competition form, participants and My competitions. Prerequisite for the competitions overview. Tables and event-display styles deferred. See [iteration 004e](iterations/004e-design-system-forms-and-lists.md). | scoped (004e) |
| **Competitions tab and overview** | Competitions tab in the bottom bar: always for admins (overview of all competitions grouped by state, plus a Joined section), for players only while they have joined an open or running competition (My competitions). Needs an admin list endpoint. See [seed 004f](iterations/004f-competitions-overview.md). | seed drafted (004f) |
| **Admin CLI** | Extend `qubigo-cli` for support: create users, competitions and invitations, list and close competitions. After 004f. | open (needs scoping) |
| **Competition presentation** | Simplify management and combine standings with joining QR in one monitor-sized event display. Phone joining page unchanged. See [iteration 004b](iterations/004b-competition-presentation.md). | done (merged to main 2026-10-20) |
| **Competitions** | Spellenspektakel competition journey; see `docs/iterations/004-competitions.md` and the [004a run brief](iterations/004a-competitions-run-brief.md). | in progress (004a: task 6 device evidence open) |
| **Clickable app banner** | iOS browsers only: make the “better in the app” banner lead to the Apple App Store. See `docs/iterations/001-clickable-app-banner.md`. Android is deferred until a Google Play link exists. | scoped (iteration 001) |
| **Onboarding redesign** | Separate iteration after the banner. Known friction: players skip email confirmation and immediately try signing in; players struggle to create a password or understand the password rules indicator. Investigate both independent signup and invitation journeys; scope and solution still to be agreed. | open (needs scoping) |
| **Game list refresh on focus** | Game list must refresh when app comes back into foreground. Design decision made. | open |
| **Challenge link privacy** | Challenge links carry an opaque id-like token instead of player name. Design decision made. | open |

## Medium-term

| Feature | Notes | Status |
|---|---|---|
| **Internationalisation (i18n)** | Extract all UI copy into translatable strings. Invitation terminology flagged as translation-sensitive. Design decision made. | open |
| **Notifications when initially refused** | Push notifications broken if user refused permission at first prompt. Needs product decision: re-prompt, in-app explanation, or accept platform default? | open (needs decision) |
| **Games/invitations cleanup policy** | Cleanup/expiry for games and invitations of deleted accounts. Needs product decision. | open (needs decision) |
| **Event logging** | First-class event logging starting with invitation/accept events. Design decision made. | open |

## Operational / infrastructure

| Work item | Notes |
|---|---|
| **Android release build (signed AAB)** | Produce signed Android AAB via build.sh on Linux host. Dockerized builder image deferred. | wip |
| **Release Android app to Google Play** | Internal testing → production. Play upload automation script exists. | todo |
| **Register real Android app-link fingerprint** | Set real SHA-256 cert fingerprint in `assetlinks.production.json` after first Play upload. | todo |
| **Make app links work in dev setup** | App links (iOS Universal Links / Android App Links) must work in Xcode emulator / cable-device against `qubigo.labs.qwan.it`. | todo |

## Completed (shipped)

Completed work is tracked in git history and `docs/design-decisions.md`. Key shipped items:
- Bottom tab-bar navigation
- App banner component (web → app nudge)
- 3D game board (Three.js)
- Sign-in, registration, password reset flows
- Game creation via invitations
- Push notifications (Capacitor)
- Demo/tutorial game
- iOS + Android Capacitor builds
- Version checking / upgrade page

---

*Iteration plans: see `docs/iterations/`.*
