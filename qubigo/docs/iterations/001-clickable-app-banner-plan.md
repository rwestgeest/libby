# Iteration 001 — Clickable iOS app banner implementation plan

Status: Tasks 1–4 done; baseline, banner behaviour, local styling, and automated verification evidence recorded and committed. Task 5 remains unfinished; the plan is not complete.
Source: `/home/rob/dev/libby/qubigo/docs/iterations/001-clickable-app-banner.md`.
Requested factory destination: `/home/rob/dev/libby/factory/runs/qubigo-001/plan.md`.

This plan is recorded inside the target repository because the requested factory destination is outside the permitted working directory. The factory must transfer it to that destination. No product changes, commits, or deployment are part of this planning step.

## Objective and constraints

Make the existing iOS web banner message a native link to exactly `https://apps.apple.com/us/app/qubigo/id6755611357`, in the current browsing context. Preserve the wording, overall appearance, iPadOS detection, Capacitor and non-iOS exclusions, `#/game/` exclusion, hash-change handling, and `app-banner-dismissed` sessionStorage behaviour.

The dismiss button remains a separate sibling outside the anchor and only dismisses. Retain native link semantics, provide a purpose-revealing accessible name, a clear link affordance, generous tap area, and visible keyboard focus. Preserve safe-area support and avoid overlap between controls on narrow screens.

No backend/API/database work, dependencies, routing services, Android support, analytics, onboarding changes, upgrade-page placeholder fixes, custom URL schemes, popups, install detection, automatic redirects, production deployment, or app-store releases.

## Ordered tasks

### 1. Confirm conventions and establish baseline — DONE

Progress evidence: `docs/iterations/001-clickable-app-banner-validation.md`, with full command outputs in `docs/iterations/001-clickable-app-banner-baseline/`. Conventions inspected and baseline commands run; `yx` remains unavailable. Focused tests and build pass; full tests and lint have unrelated pre-existing failures recorded in the evidence. Previous-attempt validator findings: none (`[]`). Task marked done by the planner following confirmation that the work has been committed.

- Read `qubigo-frontend/AGENTS.md` and consult `yx list --ready` before implementation. Planning attempted this command; `yx` is unavailable in this environment. Record this tooling limitation if it remains unavailable; do not invent task state or run git-based synchronization (factory owns commits).
- Inspect the banner component, CSS, existing tests, and component test helpers. Follow the existing custom-component and `withoutShadowRoot()` test conventions.
- From `qubigo-frontend`, run the focused banner tests, full tests, lint, and build before changes where feasible, retaining results to distinguish pre-existing failures. Check environment/dependency availability without introducing new dependencies.

### 2. Specify and implement banner behaviour with regression tests — DONE

Files: `qubigo-frontend/test/components/app-banner-component.test.js` and `qubigo-frontend/app/components/app-banner-component.js`.

Progress: native anchor and regression tests implemented and committed. Task marked done by the planner following confirmation that the work has been committed. Focused suite passes (18 tests); lint of both changed JavaScript files passes. Evidence: `docs/iterations/001-clickable-app-banner-validation.md`, task 2 section. Previous-attempt validator findings: none (`[]`); no finding subtasks required. Styling and broader/manual verification remain tasks 3–5.

- Add assertions for a native message anchor, exact approved HTTPS href without tracking parameters, original visible wording, and current-context navigation (no new-window target).
- Assert the dismiss button is outside the anchor and remains independently operable.
- Exercise link activation while cancelling its default navigation in jsdom: assert it neither dismisses the banner nor writes the dismissal key. Verify navigation contract through anchor semantics/attributes rather than pretending jsdom can validate App Store handoff.
- Exercise dismiss-button activation: assert no link activation occurs, banner disappears, and the existing storage key is set. Assert continued dismissal after route changes and component recreation in the same session.
- Retain the existing visibility, iPadOS detection, and hash-change regressions.
- Replace only the message span with the native anchor and fixed trusted destination. Use an accessible name that communicates the Qubigo App Store destination while retaining the visible wording. Do not attach navigation to the banner container or change the dismiss handler.

### 3. Add local styling and check accessibility — DONE

Progress: local CSS implemented and committed. Task marked done by the planner following confirmation that the work has been committed. Underlined, inherited-colour link and visible focus outlines added. Link and sibling dismiss control have separate minimum 44px-high targets; flex layout, wrapping, and horizontal safe-area padding replace absolute dismiss positioning. Focused tests (18) and build pass. Static accessibility/layout review is recorded in `docs/iterations/001-clickable-app-banner-validation.md`; browser/device checks remain outstanding. Previous-attempt validator findings: none (`[]`); no finding subtasks required.

File: `qubigo-frontend/app/components/app-banner.module.css`.

- Style the anchor consistently with the existing banner, with an unmistakable link affordance and visible keyboard-focus outline.
- Give the message link a generous tap area without encroaching on the sibling dismiss button. Preserve wrapping/readability and safe-area top padding; check horizontal safe-area insets and control clearance in landscape.
- Avoid removing native keyboard behaviour or introducing positive tabindex, click-only controls, or broad UI redesign.

### 4. Run automated verification and record evidence — DONE

Progress: checks executed and committed. Task marked done by the planner following confirmation that the work has been committed. Evidence: `docs/iterations/001-clickable-app-banner-validation.md`, task 4 section, and full logs in `docs/iterations/001-clickable-app-banner-verification/`. Previous-attempt validator findings: none (`[]`); no finding subtasks required.

- [x] Run focused and full tests, lint, and build; retain command outputs and exit results. Focused tests (18) and build pass; full tests and lint reproduce documented unrelated baseline failures.
- [x] Compare failures against baseline and review the implementation diff for scope, exact destination, unchanged visibility/session logic, and generated output. Changed JavaScript lint and whitespace checks pass; no regressions or tracked generated changes observed.

Run from `qubigo-frontend`:

1. `npm test -- --runInBand test/components/app-banner-component.test.js`
2. `npm test -- --runInBand`
3. `npm run lint`
4. `npm run build`

Record command, result, and any failures or environment limitations. Explicitly separate unrelated baseline failures from regressions caused by this change. Do not fix unrelated failures in this iteration. Review the diff for scope, exact URL, unchanged visibility/session logic, and accidental generated output.

### 5. Perform manual acceptance and hand off

Progress: in progress; environment availability checked, focused regression suite rerun (18 passing), and a manual acceptance matrix plus reviewer handoff recorded in `docs/iterations/001-clickable-app-banner-validation.md`. Real-device acceptance remains outstanding; this task is not done.

Previous-attempt validator findings (subtasks of task 5):

- [x] **No uncommitted changes or new untracked files exist in /home/rob/dev/qubigo; no supplied product work is available for testability review. Previously committed work was not reviewed.** Resolved by supplying uncommitted task-5 acceptance evidence and this plan update for review. Existing product code is already committed; the handoff identifies the exact files to inspect without claiming new product implementation.
- [ ] **The supplied plan is missing (ENOENT), preventing validation against it. The target-local workaround does not restore the requested plan.** Rechecked `/home/rob/dev/libby/factory/runs/qubigo-001/plan.md` in this attempt: read returns ENOENT and filesystem existence check fails. Exact-path repair remains blocked by the instruction to work only in `/home/rob/dev/qubigo`. Factory must restore the requested plan outside the target and supply it to validation. This local plan is only the available task breakdown, not restoration of the requested path; no outside-target plan file or symlink was created.
- [ ] **Required manual acceptance remains outstanding: real iPhone/iPad App Store handoff, rendered layouts, keyboard/accessibility, and runtime visibility/dismissal checks. The new matrix clearly documents these gaps but provides no execution evidence, so acceptance completion remains blocked.** Rechecked available tooling: no browser, Apple simulator tooling, or iOS device CLI; no graphical display; `adb devices -l` lists no attached devices. No real iPhone/iPad browser session is available. Actual acceptance cannot be executed here; a device-equipped reviewer must execute the existing matrix and record actual evidence before this subtask can be resolved. Automated/static checks do not resolve it.

Record evidence in `docs/iterations/001-clickable-app-banner-validation.md` during implementation/review (do not pre-fill passing results).

- On real iPhone and iPad browsers, record device, OS/browser versions, orientation, tested route, and result. Include iPadOS desktop-mode detection.
- Check narrow portrait and landscape/iPad layouts: readable message, usable separate controls, and safe-area clearance.
- Check eligible routes, exclusion on `#/game/...`, and reappearance when leaving a game before dismissal. Verify non-iOS and Capacitor exclusions through available runtime checks as well as automated coverage.
- Tab to the link and dismiss button; check visible focus, native Enter activation, and accessible link purpose. Use assistive-technology inspection where available.
- Activate the message and verify the approved US URL reaches the correct Qubigo listing, app ID `6755611357`, by Rob Westgeest. Browser/DOM simulations are not proof of this handoff.
- Activate dismiss independently: no navigation, hidden after subsequent route changes and recreation/reload within the same browser session.
- If a real device or other acceptance check is unavailable, explicitly mark the check outstanding with the required follow-up. Do not claim acceptance completion.

## Acceptance mapping and completion gate

- Criteria 1–2: anchor/destination automated checks plus real iPhone/iPad activation and verified listing.
- Criterion 3: native semantics and accessible purpose assertions plus manual keyboard/focus and assistive-technology checks.
- Criterion 4: independent dismissal/session regression tests plus manual no-navigation and route checks.
- Criterion 5: retained visibility/detection tests plus runtime checks where available.
- Criterion 6: manual narrow-screen, iPad, orientation, and safe-area inspection.

Implementation is complete only when all agreed acceptance criteria pass and review evidence is recorded. Outstanding real-device verification blocks acceptance completion. Handoff includes changed-file summary, automated results, manual evidence, and explicit outstanding checks or unrelated failures. The factory handles commits; deployment and app releases remain out of scope.
