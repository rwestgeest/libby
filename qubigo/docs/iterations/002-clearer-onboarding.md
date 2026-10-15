# Iteration 002 — Clearer onboarding

Status: Agreed and ready for implementation. Product owner approved the scope, behavior, and copy on 2026-10-14. Implementation has not started.

## Goal and observed problem

Help new players finish registration without getting stuck on password requirements or attempting to sign in before verifying their email. This supports the vision in `docs/vision.md`: help curious players start enjoying Qubigo.

Player feedback identifies two problems: password-rule symbols are unclear, including the length arrow rendering as an icon on Apple devices; some players do not understand that email verification must precede sign-in. No problems after clicking the verification link have been reported in this interview.

## Agreed scope

- Keep the existing password composition requirements; improve their explanation and feedback. Correct the frontend length limit to accept 10–64 characters inclusive, matching the backend.
- Cover registration both directly and through an invitation.
- Improve the shared password-rule presentation on password reset as well.
- Replace the post-registration sign-in prompt with clear instructions to check email and verify before signing in. Remove both the sign-in button and any replacement sign-in link from that page.
- Help players who attempt sign-in before verification understand the next step.
- Do not add verification-email resending or change the journey after clicking the verification link.

## Current implementation and important findings

The frontend is a vanilla JavaScript SPA with custom HTML components, observable modules, and HTTP adapters. Follow `qubigo-frontend/AGENTS.md`. This iteration needs no backend changes, API changes, or database migration. The existing backend password boundary is the reference for the frontend correction.

- `qubigo-frontend/app/components/password-rules-component.js` renders a horizontal list of symbols: a length arrow, `Aa`, `0–9`, and a matching cross/check. Rule descriptions are tooltips. Updates change red/green classes; matching treats two empty strings as equal.
- `qubigo-frontend/app/domain/new-password.js` requires length >= 10 and < 64, ASCII lowercase and uppercase letters, and an ASCII digit. The component tooltip says 10–64, contradicting the frontend's actual 10–63 boundary.
- Backend `qubigo-backend/qubigo-servlet/src/main/kotlin/com/qubigo/qubigobackend/users/domain/primitives/Password.kt` accepts 10–64 inclusive. The agreed fix is to change the frontend upper-bound predicate to `<= 64`; backend validation stays as it is.
- `app/pages/registration-page.js` places the shared rules between password and confirmation fields, updates them on input, and disables registration until the whole registration model is valid. Invitation context is passed into `Registration({invitedBy: invitor})`.
- `app/pages/set-password-page.js` uses the same rules component and `NewPassword` model. Preserve successful reset navigation and token behavior.
- `app/modules/onboarding-module.js` sends successful registration to `#/registration-success`. Its confirmation flow leads to `#/confirm-registration-success`.
- `app/pages/registration-success-page.js` currently says “Awesome! Almost there...” and offers “Sign me in,” contradicting its email instructions.
- `app/pages/confirm-registration-success-page.js` offers sign-in after verification. Preserve this page and route behavior; the old sequence diagrams in `docs/` are not the implementation contract.
- Login flows through `app/adapters/fetch-based-http.js` (`/api/loginjwt`), `app/adapters/api-based.authenticator.js`, `app/modules/authentication.module.js`, and `app/domain/qubigo-error.js`.
- Backend `users/adapters/controllers/AuthenticationController.kt` maps authentication failures to HTTP 400 with `{message: "Invalid username or password"}`. The current API does not distinguish an unverified account. Do not infer account verification status from this generic failure or from remembered signup state.

Paths prefixed with `app/` above are relative to `qubigo-frontend/`. Backend package paths are below `qubigo-backend/qubigo-servlet/src/main/kotlin/com/qubigo/qubigobackend/`.

## Agreed password presentation

Replace symbols and tooltip-only explanations with a vertical text list, visible before typing. Approved labels:

- “10–64 characters.”
- “At least one uppercase letter (A–Z) and one lowercase letter (a–z).”
- “At least one number (0–9).”

Each rule has visible “Not yet met” or “Met” text, updated on input. Color may reinforce status but cannot be its only indication. Avoid Unicode arrows or emoji as status controls. Maintain readable wrapping on small mobile screens.

Put confirmation feedback immediately below the confirmation field on registration and password reset. Approved states:

- Empty confirmation: “Enter the same password again.”
- Nonempty unequal confirmation: “Passwords do not match.”
- Nonempty equal confirmation with a nonempty password: “Passwords match.”

The matching message reports equality; the other rules still determine password validity. Clearing inputs must update feedback correctly. Do not show a success state for two empty fields. Preserve button validity rules and registration-name/email validation.

Use the existing component/module patterns, keeping validation in `NewPassword` rather than duplicating regexes in views. Extend the presentation interface to provide the input-presence information needed for matching feedback, or split matching feedback into a small shared presentation component. Do not expose or store password values beyond their current form/domain lifecycle.

Ensure descriptions are accessible from the actual inputs inside `input-field` rather than assuming ARIA on a custom-element host labels its inner input. Provide readable text status and a concise polite announcement for confirmation state changes without announcing the entire checklist on every keystroke. Verify focus stability while typing, keyboard access, and zoom/reflow.

## Agreed email-verification presentation

Registration page: add a short expectation near the submission action: “After registering, verify your email before signing in.” This applies to both signup entry paths.

Post-registration page:

- Heading: “Check your email”.
- Main instruction: “Open the email from Qubigo and click the verification link. You must verify your email before signing in.”
- Follow-up: “Can't find the email? Check your spam or junk folder.”
- No sign-in button or sign-in link in this page's content. Remove the old handler and its obsolete tests. Do not change global navigation in this iteration.
- No resend action, automatic email-app launching, polling, or automatic sign-in.
- Do not require a stored email address for this page to render; it must work when opened directly or refreshed.

Do not add a promise of automatic login or a new support contact. Keep the existing verification email and confirmation routes working.

## Agreed sign-in explanation

Keep the generic invalid-credentials error and display a separate reminder immediately alongside it:

“If you just registered, verify your email using the link we sent before signing in. Check your spam or junk folder.”

Show this only for a failed login whose returned message is exactly `Invalid username or password`, the current backend contract. Keep classification at the authentication module or adapter boundary; expose a narrowly scoped presentation flag if needed. Do not change `QubigoError` semantics globally or add guidance to other forms' generic error messages.

The wording is conditional because the API does not identify an unverified account. Do not label the account unverified, add a verification-status lookup, or change authentication logic. Unknown usernames and wrong passwords continue receiving the existing generic error plus this reminder. Network and other errors retain their existing messages without this reminder.

Use observable updates for this feedback. Clear stale error/guidance at the start of each sign-in attempt and when opening the sign-in page; notify views through the existing module pattern. Remove guidance on successful login and page teardown. If the page registers a new view, deregister it in `close()`; do not rerender password inputs on error-state changes or lose typed values/focus.

All decisions are settled. Resending verification email and changes after clicking the verification link remain deferred.

## Implementation sequence

1. Read this agreed plan and applicable guidance. Consult `yx list --ready`; it was unavailable during planning (`yx: command not found`). Record availability during implementation without inventing task state. Establish focused test, full test, lint, and build baselines. Preserve unrelated changes and do not bundle iteration 001 work.
2. Extend meaningful component/page tests for readable password rules, initial and changing states, confirmation placement, and valid/invalid button behavior. Implement the shared presentation and both form integrations. Add domain regression coverage for 9, 10, 63, 64, and 65 characters before changing the predicate to accept 64. Use otherwise valid passwords at each length so the tests isolate the boundary.
3. Update registration expectation and success-page instructions. Test presence of verification guidance, absence of page-level sign-in controls, and successful routing only after registration succeeds. Retain failed-registration errors and invitation payload/context.
4. Implement the agreed conditional sign-in explanation only with invalid-credentials feedback. Test wrong credentials, unknown username, network/other errors, retry, success, and leaving/reopening the page. Preserve the login API and successful routing.
5. Run automated checks and rendered/manual acceptance below. Record actual evidence and outstanding device checks; do not claim unavailable checks passed. Update the plan status and handoff with changed files, results, and remaining limitations. Publishing and app releases are separate work.

## Automated verification

Extend existing suites rather than replacing behavior assertions with snapshots:

- `test/components/password-rules-component.test.js`.
- `test/pages/registration-page.test.js` and `test/pages/set-password-page.test.js`.
- `test/pages/registration-success-page.test.js`.
- `test/domain/new-password.test.js` for the inclusive upper boundary.
- `test/pages/sign-in-page.test.js` and `test/modules/authentication-module.test.js` for the conditional reminder and its lifecycle.
- Existing `test/adapters/api-based-authenticator.test.js` and `test/adapters/fetch-based-http.test.js` for unchanged login error propagation and success behavior.
- Existing onboarding and confirmation tests to protect successful and failed registration, confirmation routes, and invitation context.

Use Jest/jsdom and existing `withoutShadowRoot()`, observable stubs, and fake-router helpers. Assert text, state transitions, and user outcomes rather than only CSS class names.

From `qubigo-frontend`, run focused affected tests, then `npm test -- --runInBand`, `npm run lint`, and `npm run build`. Compare any failures with the baseline; document unrelated failures instead of fixing them in this iteration. Backend checks are not required for this frontend-only iteration.

## Acceptance criteria and manual checks

- A new player can read every password requirement without hovering, deciphering symbols, or relying on color; states update while typing, pasting, and clearing.
- Requirements accurately describe the agreed length boundaries and letter/number rules. Boundary, missing-uppercase, missing-lowercase, missing-number, and valid-password cases behave consistently with the text.
- Matching guidance sits below confirmation, handles empty/equal/unequal values correctly, and never indicates that empty fields are successfully matched.
- Registration and password reset remain disabled until all existing requirements are satisfied; editing a valid form into an invalid one disables submission again.
- Both direct and invited registration clearly explain email verification before submission and after success. Failed registration does not display the success page.
- The post-registration page has “Check your email,” explicit verification-before-sign-in instructions, spam/junk guidance, and no sign-in action in its content.
- Premature sign-in gets the agreed explanation; legitimate wrong-credentials feedback remains accurate and successful verified sign-in behaves as before.
- Email verification links, confirmation success, and invitation context still work as before. No resend capability is added.
- Render and exercise both forms on narrow phone portrait and landscape layouts, iPhone Safari, and the iOS app if available. Include a desktop keyboard check and Android/web check. Record platform/version and actual results, especially Apple text rendering.
- Check readable wrapping, no horizontal overflow, focus stability, keyboard operation, screen-reader descriptions/status, and reflow at increased text size. Screenshots supplement interaction evidence.

## Out of scope

Verification-email resend, verification-link redesign, automatic login after verification, email delivery infrastructure, changing password composition rules beyond correcting the frontend upper boundary to match the backend, broader registration redesign, social/passwordless login, invitation-token privacy work, analytics/i18n rollout, competitions, banner acceptance, deployments, and store releases.

## Planning evidence

This plan was derived from `docs/vision.md`, `docs/backlog.md`, `docs/design-decisions.md`, the product-owner interview, and inspection of the source and existing tests on 2026-10-14. No product implementation or test execution has been performed for iteration 002 during planning. Existing modifications in `.factory/runs/qubigo-001/plan.md` and `docs/iterations/001-clickable-app-banner-validation.md` were present before writing this plan and are unrelated.

Completion requires the automated evidence and manual checks above, with any unavailable checks explicitly outstanding. The plan approval authorizes this scope for implementation; it does not claim the work has been implemented or accepted.
