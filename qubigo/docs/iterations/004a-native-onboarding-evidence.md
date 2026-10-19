# 004a — installed-app account return (2b)

## Delivered scope

Added return/reopen guidance to registration and verification confirmation. A visitor verifying elsewhere is told to return to the original app/browser, sign in, and reopen the original competition link if needed. Competition preview explains that installation is optional and that reopening the original link after installation is the supported fallback. No extra installation prompt or funnel was added; the existing banner/tutorial remain unchanged.

Existing IncomingLinkHandler, Router, BrowserCompetitionReturn, AuthenticationModule and UserProfileModule already support the agreed application behavior. No navigation rewrite, cross-context transfer, account-bound destination, backend change or new dependency was needed. Joining remains explicit; organizer management and domain deadline rules are unchanged.

## Test-first observations

Extended `qubigo-frontend/test/competitions/browser-onboarding.test.js`, reusing its real pages, account-return modules and Router fixture rather than copying onboarding helpers. IncomingLinkHandler receives controlled Capacitor cold-launch and warm-open callbacks. Cold startup also exercises profile home-routing after incoming-link handling. Warm callbacks run after Router startup, including a second incoming link during pending authentication.

- `cd qubigo-frontend && npm test -- --runInBand test/competitions/browser-onboarding.test.js`: guidance examples failed before the relevant production text was added (registration return guidance and optional-install preview guidance). Existing native routing/account-return scenarios passed without production changes.
- Initial fixture feedback exposed two test-driver issues: unconditional sign-in rerender cleared failure feedback; success routing could complete before the completion helper captured the route. Helpers now render sign-in only after navigation and explicitly render completed onboarding success routes. No production change was made for these fixture issues.
- After correcting the completion helper, temporarily omitting verification confirmation guidance and running `npm test -- --runInBand test/competitions/browser-onboarding.test.js -t 'verification in an isolated browser explains'` produced the relevant failure: expected `return to the original app or browser`, received the ordinary `Awesome! Done!` / `Sign in and start playing!` confirmation. Restoring the minimal guidance passed.

New regression coverage (11 cases):

- Separate cold/warm, authenticated/signed-out arrival outcomes show Event A, 3 games per participant, rules and Europe/Amsterdam-labelled deadline, with no join call.
- Native registration explains the fallback and retains A without enrolling.
- Verification uses a separate destination store (sessionStorage as an isolated-context test double, versus the original context's localStorage). It shows fallback guidance without recovering A or joining. Returning/reopening in the original context and signing in reaches A.
- A fresh app context receives the original link without importing another context's pending B destination; sign-in reaches A.
- Browser preview keeps explicit joining available while explaining optional installation/reopening.
- Failed native sign-in retains A and displays the authentication error.
- A newer warm B link survives an older pending sign-in completion.

Existing browser/participant regressions continue to cover missing competition and elapsed joining deadline after account return, participant-only destination safety/consumption, ordinary onboarding, tutorial return and stale preview/page completions. No new backend behavior warrants duplicate tests here.

## Final checks

From `qubigo-frontend`:

- `npm test -- --runInBand test/competitions/browser-onboarding.test.js test/competitions/browser-competition-return.test.js test/pages/registration-success-page.test.js test/pages/confirm-registration-success-page.test.js test/adapters/incoming-link-handler.test.js test/components/app-banner-component.test.js`: **74 passed**, 6 suites, exit 0.
- `npm test -- --runInBand`: **801 passed, 11 skipped**, 70 passing suites / 1 skipped, exit 0.
- `npm run build`: exit 0.

From repository root: `git diff --check`: exit 0.

Backend behavior is unchanged; `./gradlew test` was not rerun. Backend AGENTS and ADRs 0002/0005/0007/0008 were read; this slice changes presentation and frontend collaboration tests only, not validated inputs, commands, aggregates or persistence.

## Evidence boundary

These are application-contract simulations with controlled account/verification/API/plugin inputs, not evidence of physical mail, QR scanning, OS association, WebView/browser storage sharing, app-store transition or actual Android/iOS delivery. No automatic cross-context or deferred-install recovery is promised. Named platform/device/build and observed real cold/warm/verification/install-reopen evidence remains unresolved under task 6, not a gate for this application increment.

Task 2b remains open for independent acceptance and the factory's commit; no commit was made by the doer. Pre-existing assembly-line configuration changes were left untouched.
