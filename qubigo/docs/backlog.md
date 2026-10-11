# Qubigo Backlog

Cross-cutting feature work not yet assigned to an iteration. Items are ordered by rough priority (highest first). See `docs/iterations/` for scoped iteration plans.

---

## Short-term (next 1-2 iterations)

| Feature | Notes | Status |
|---|---|---|
| **Competitions** | Themed events, leaderboards, tournament play to engage players. Brainstorm in `docs/iterations/002-competitions.md`. | placeholder |
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

*Iteration plans: `docs/iterations/001-polish-bugs.md`, `docs/iterations/002-competitions.md`*
