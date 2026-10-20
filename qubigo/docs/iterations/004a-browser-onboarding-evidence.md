# 004a — browser account-return evidence (2a)

Status: implemented, awaiting factory acceptance/commit. Task 2a remains open.

## Delivered scope and responsibilities

A signed-out browser visitor following a participant competition link retains that
internal destination while using existing sign-in, registration and email
verification. The destination lives in browser-profile localStorage, not in the
account or verification token. A reload or new tab sharing that storage retains
it. A later competition link replaces it. Successful authenticated return consumes
it; failed account actions do not. Only `#/competitions/<ASCII identifier>` routes
are accepted; external URLs, admin routes and encoded path/query injection are
not redirect targets.

`BrowserCompetitionReturn` owns that small storage/validation protocol.
`CompetitionPage` coordinates the existing profile lookup before requesting the
protected preview, retains the destination, and redirects anonymous visitors to
sign-in. `UserProfileModule.goToHome` refreshes the authenticated profile and
returns from a neutral route to the retained participant screen instead of the
ordinary home. Ordinary account routes still use their existing destinations
when nothing is pending. Preview and join remain the existing capability;
registration, verification, sign-in and viewing never enroll the visitor.

Authentication/registration/verification commands ignore stale completions after
page close or a newer attempt. Home routing also checks the initiating route and
pending destination before redirecting. Participant preview/join HTTP calls have
a request-local authentication-required callback, guarded by the page opening;
a late unauthorized response cannot invoke the app-wide sign-in redirect over
Profile. The HTTP default remains unchanged for other callers. No new backend
API or anonymous competition access was introduced; admin security is unchanged.

The existing tutorial is linked from sign-in and participant preview. Returning
to sign-in from the existing tutorial retains the pending competition without
enrolling. App banner and tutorial implementation are unchanged; no installation
step is required.

## Test-first record

Commands below ran in `qubigo-frontend`, with controlled account, verification and
competition collaborators and the real Router/pages. No sleeps or polling.

1. `npm test -- --runInBand test/competitions/browser-competition-return.test.js`
   initially failed because the destination collaborator did not exist. Added
   the bounded storage protocol; 11 destination safety/replacement/consumption
   cases passed.
2. `npm test -- --runInBand test/competitions/browser*` initially reported
   3 failures: signed-out startup did not redirect/retain A, successful login
   did not return to A, and a later B destination was not retained. Added profile
   gating, persistence and authenticated return. The composed account-flow
   harness then passed (16 tests at that point).
3. Extended the real-page registration/verification journey and deferred
   responses. `npm test -- --runInBand test/competitions/browser-onboarding.test.js`
   reported 3 failures: late registration and verification navigated over a newer
   destination, and an old verification error contaminated a new registration
   form. Added page-lifetime invalidation and command completion guards.
4. Added delayed profile-refresh regression: expected `#/signin` with B pending,
   received `#/welcome`. Home routing now refuses to act if the pending
   destination changed while awaiting profile/notifications.
5. Added real FetchBasedHTTP + API repository + Router regression for late 403
   preview completion after navigation. It first failed with expected `#/profile`,
   received `#/signin`. Request-local authentication callbacks now keep that
   response inside the originating page lifetime. A separate current-session
   expiry check confirms active requests still retain A and ask for sign-in.
6. Tutorial-link journey first failed on the absent links; added links to the
   existing tutorial on sign-in/preview. The test opens the real GameDemoPage
   with a controlled scenario module, then returns to sign-in without losing A.

Final focused command:

```
npm test -- --runInBand test/competitions/browser-competition-return.test.js test/competitions/browser-onboarding.test.js
```

Exit 0: 32 passed. Coverage includes actual sign-in form submission, registration
form values and verification guidance, a new browser-tab instance with shared
storage following the verification route, failed registration/verification,
refreshing sign-in, signed-in deep links, destination replacement, one-time
consumption and ordinary later sign-in, deferred account/profile/HTTP responses,
missing competition and explicit join deadline refusal after account return.

Final checks:

- `npm test -- --runInBand`: exit 0; 70 suites passed, 1 skipped;
  **790 tests passed, 11 skipped**.
- `npm run build`: exit 0.
- `git diff --check`: exit 0.
- Backend unchanged; `./gradlew test` not rerun for this frontend-only increment.
  No backend behavior or authorization changes are claimed.

## Evidence limits and deferrals

These are automated browser-profile continuity checks, not physical email,
camera, OS association, native app, cross-browser/device or store-install proof.
Existing verification contracts are controlled responses, not a new mail service.
Native continuity and device evidence remain in 2b/6. No participant roster,
start, play, standings, scheduler, general redirect platform, account-bound
recovery or onboarding queue was added. Factory owns commits and acceptance;
this evidence does not mark 2a or iteration 004 complete.
