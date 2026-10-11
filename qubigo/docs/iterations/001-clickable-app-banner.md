# Iteration 001 — Clickable iOS app banner

Status: Agreed on 2026-10-10. Ready for implementation; this document authorizes planning scope, not production deployment.

## Context and goal

Qubigo is a 3×3×3 strategy game whose digital edition helps people become fans and eventually buy the physical game. Read `docs/vision.md` for the product vision.

The web game is available at app.qubigo.com and has a small player base, including daily players. The iOS app is available in the App Store. Android is under Google Play review and has no store link yet. The physical game is sold at qubigo.com.

This first iteration deliberately delivers one small improvement to exercise the planning and implementation process: an iOS web player can tap the existing “Qubigo works better in the app” banner and reach the Qubigo App Store listing.

There is no deadline or time budget. Keep the work bounded to this behaviour.

## Agreed scope

- Make the existing banner lead to the iOS App Store listing.
- Support iOS browsers only, including the existing iPadOS detection.
- Preserve the separate dismiss action and current visibility rules.
- Treat onboarding redesign as a separate iteration. Known problems are skipped email confirmation followed by unsuccessful sign-in, and difficulty satisfying or understanding password rules. These are recorded in `docs/backlog.md`.

## Agreed interaction

- Keep the existing banner wording and appearance, adding a clear link affordance and visible keyboard focus.
- Make the banner message area a native HTML link. Give it a generous tap area within the banner; keep the dismiss button as a separate sibling, outside the link.
- Follow a standard HTTPS App Store URL in the current browsing context. Let iOS handle opening the App Store; do not add custom URL schemes, popups, app-install detection, or automatic redirects.
- The dismiss button only dismisses the banner. It never opens the store.

Destination: `https://apps.apple.com/us/app/qubigo/id6755611357` (supplied and approved by the product owner). Use this exact URL, with no tracking parameters. Apple listings in other storefronts confirm app ID `6755611357` belongs to Qubigo by Rob Westgeest, including [the Netherlands listing](https://apps.apple.com/nl/app/qubigo/id6755611357). The US URL could not be fetched by the research tool; verify the approved URL during the real-device acceptance check.

## Existing implementation and architectural guidance

The frontend is a vanilla JavaScript SPA using custom components, Vite, Jest, Three.js and Capacitor. Read `qubigo-frontend/AGENTS.md` before implementation and follow its component and testing conventions. No backend, database, or API change is needed.

Relevant files:

- `qubigo-frontend/app/components/app-banner-component.js`: banner rendering, iOS detection, visibility and session dismissal.
- `qubigo-frontend/app/components/app-banner.module.css`: banner layout and styling.
- `qubigo-frontend/test/components/app-banner-component.test.js`: existing banner tests; extend where meaningful.
- `qubigo-frontend/app/pages/upgrade-page.js`: contains a placeholder App Store URL, `id0000000000`. Do not use that value as the destination. Updating the upgrade page is outside this iteration.

Preserve `shouldShowAppBanner` behaviour: hidden after session dismissal, inside Capacitor, on non-iOS browsers, and on routes starting with `#/game/`. Preserve the `app-banner-dismissed` sessionStorage key and hash-change handling. Use a normal anchor with a fixed trusted URL; the store URL must not come from user input. Keep the change local to the banner unless repository conventions require otherwise. Do not introduce a routing service or dependency for this link.

## Acceptance criteria

1. In an eligible iPhone or iPad browser, the existing banner appears and its message area is an actionable link to the real Qubigo iOS App Store listing.
2. Activating that link follows the approved HTTPS URL. On a real iOS device, it reaches the correct Qubigo listing.
3. The link is keyboard accessible, has a visible focus state, and exposes its purpose to assistive technology. Native link semantics are retained.
4. Activating the dismiss button hides the banner without navigating. It stays dismissed during the current browser session, including after route changes.
5. Existing visibility exclusions continue to work: non-iOS browsers, the native Capacitor app, and game routes do not show the banner.
6. The banner remains readable and its link and dismiss targets remain usable on narrow iPhone screens and iPad layouts, including safe-area insets.

## Validation requirements

Provide automated coverage for the link destination and the separation of link activation from dismissal. Retain existing visibility and session-dismissal coverage. Follow the frontend repository's required test, lint, and build checks, and report unrelated pre-existing failures explicitly.

Manually verify eligible iPhone/iPad browser layouts, route visibility, dismissal, keyboard focus, and the real store destination. Automated DOM tests alone cannot verify the iOS App Store handoff. Record device/browser and results; if a real-device check is unavailable, record it as outstanding.

Completion means the agreed acceptance criteria pass and the review evidence is recorded. Production deployment and app-store releases are outside this iteration; do not deploy as part of implementation.

## Scope boundaries and handoff

This document defines the iteration's product scope, architectural constraints, and acceptance criteria. The assembly line owns creating and recording the implementation plan, including task breakdown and execution order; the context above is input to that plan.

There are no remaining product decisions for this iteration. Deliver the implementation, automated checks, and manual acceptance evidence for review. Do not deploy to production or publish app releases. Do not add Android support, analytics, onboarding changes, upgrade-page fixes, or a wider banner redesign.

The next iteration will scope onboarding separately through further interviews. Neither the onboarding design nor its implementation is authorized by this plan.
