# Iteration 001 — Validation evidence

## Task 1: conventions and baseline (2026-10-10)

Baseline collected before any application or test changes. Task 1 remains in progress for planner review; later tasks have not been implemented. Previous-attempt validator findings supplied: none (`[]`), so no finding subtasks were needed.

The requested `/home/rob/dev/libby/factory/runs/qubigo-001/plan.md` was absent. Used the target-local plan `docs/iterations/001-clickable-app-banner-plan.md`; no files outside the target directory were changed.

### Conventions and inspection

- Read the seed, `docs/vision.md`, and `qubigo-frontend/AGENTS.md`.
- Attempted `yx list --ready`: `/bin/bash: yx: command not found`. Task discovery through yaks is unavailable; no task state was invented and no synchronization or commits were performed.
- Inspected the banner component, its CSS and tests, `component.js`, `without-shadow.root.js`, `jest.config.js`, and `package.json`.
- Components use `defineComponent()` and `Component` shadow roots; tests register `withoutShadowRoot(AppBanner())` and query the document. Jest maps CSS classes through `identity-obj-proxy` and inline CSS through a stub.
- Current message is a span with verbatim wording; dismiss is a sibling button. Dismiss sets `app-banner-dismissed` and re-renders. Visibility covers iOS/iPadOS touch detection, Capacitor exclusion, game-route exclusion, session dismissal, and hash changes. Existing tests cover these behaviours, but not a store link.
- CSS uses a fixed flex banner, absolute dismiss control, and top safe-area padding. Horizontal safe areas and link affordance/focus remain for task 3.
- Node `v24.18.0`, npm `11.16.0`; existing `node_modules` available. No dependencies installed or changed.

### Baseline commands

All commands run from `qubigo-frontend`. Full outputs are retained in `docs/iterations/001-clickable-app-banner-baseline/`.

| Command | Exit | Result | Evidence |
| --- | --- | --- | --- |
| `npm test -- --runInBand test/components/app-banner-component.test.js` | 0 | 1 suite, 16 tests passed | `focused-tests.txt` |
| `npm test -- --runInBand` | 1 | Summary: 65 suites passed, 1 skipped; 669 tests passed, 11 skipped. After summary, Jest reported open asynchronous operations and an uncaught assertion in `test/domain/jumper.test.js:78`: expected `2`, received `2.0000000000000004`. | `full-tests.txt` |
| `npm run lint` | 1 | 36 pre-existing errors: 32 in `deploy/dist/assets` bundles; 4 unused imports/declarations in source/tests listed below. | `lint.txt` |
| `npm run build` | 0 | Vite `6.4.3`, 128 modules transformed, production build succeeded. | `build.txt` |

Unused declarations reported by baseline lint:

- `app/games/components/game-list-component.js:5`: `GameType`.
- `test/pages/confirm-registration-page.test.js:15`: `visibilityOf`.
- `test/pages/confirm-registration-success-page.test.js:8`: `RegistrationSuccessPage`.
- `test/router.test.js:150`: `FakeUserProfileModule`.

These failures were present before banner changes and are outside this iteration's scope; they were not fixed. Build output was confined to ignored `qubigo-frontend/dist`; git status showed no tracked application changes after the checks. No deployment or release was performed.

## Task 2: banner behaviour and regression tests

Used the target-local plan again because the requested factory plan path is absent. `yx list --ready` remains unavailable (exit 127). Previous-attempt validator findings: none (`[]`); no finding subtasks were required. Task 2 remains in progress for planner review; no commits were created.

- Replaced only the message span with a native anchor to the exact approved HTTPS URL, without target, tracking parameters, or navigation handlers. Visible wording is unchanged; its accessible name includes that wording and identifies the Qubigo App Store destination.
- Dismiss remains a separate sibling button. Visibility, iPadOS detection, hash-change handling, and session storage logic are unchanged.
- Added destination/native-semantics and sibling assertions, cancelled-default link activation checks, and dismissal checks covering no link activation, route changes, and component recreation.
- Before implementation, the expanded focused suite failed as expected: 3 failures from the missing anchor, 15 tests passed.
- After implementation, `npm test -- --runInBand test/components/app-banner-component.test.js` passed: 1 suite, 18 tests (exit 0).
- `npx eslint app/components/app-banner-component.js test/components/app-banner-component.test.js` passed (exit 0).

## Task 3: local styling and static accessibility review

Used the target-local plan because the requested factory plan is still absent. `yx list --ready` remains unavailable (exit 127). Previous-attempt validator findings: none (`[]`); no finding subtasks required. Task 3 remains in progress for planner review; no commits were created.

- Changed only `qubigo-frontend/app/components/app-banner.module.css` in application code. The message inherits the banner colour and has a persistent underline with offset, preserving wording and native anchor semantics.
- Added contrasting 2px `:focus-visible` outlines with 2px offset to both link and dismiss button. No keyboard handlers, tabindex changes, or outline suppression were introduced.
- Link has a minimum 44px height and padded full-width flex target. Dismiss has a separate minimum 44×44px target and cannot shrink. Removed absolute positioning so the controls occupy non-overlapping sibling flex boxes with a 0.75rem gap.
- Retained top safe-area padding and added left/right safe-area padding. Link can shrink and wrap (`min-width: 0`, `overflow-wrap: anywhere`) rather than pushing the dismiss target beyond a narrow viewport. Static review confirms padding/gap leave clearance for focus outlines; this is not rendered-layout proof.
- `npm test -- --runInBand test/components/app-banner-component.test.js` from `qubigo-frontend`: exit 0, 1 suite / 18 tests passed.
- `npm run build` from `qubigo-frontend`: exit 0, Vite 6.4.3 / 128 modules transformed. Build validates CSS processing, not visual appearance.
- No Chromium/Google Chrome executable or real iOS device was available for this check. Rendered narrow/iPad layouts, landscape safe-area clearance, keyboard focus, and assistive-technology behaviour remain outstanding for task 5.

## Task 4: automated verification and scope review

Used the target-local plan because the requested factory plan path is absent. `yx list --ready` remains unavailable (exit 127). Previous-attempt validator findings: none (`[]`); no finding subtasks required. Task 4 remains in progress for planner review; no commits were created.

All required commands ran from `qubigo-frontend`; full outputs are in `docs/iterations/001-clickable-app-banner-verification/`.

| Command | Exit | Result | Evidence |
| --- | --- | --- | --- |
| `npm test -- --runInBand test/components/app-banner-component.test.js` | 0 | 1 suite, 18 tests passed | `focused-tests.txt` |
| `npm test -- --runInBand` | 1 | 65 suites passed, 1 skipped; 671 tests passed, 11 skipped. Same post-summary open-operations warning and uncaught floating-point assertion at `test/domain/jumper.test.js:78` as baseline (expected `2`, received `2.0000000000000004`). | `full-tests.txt` |
| `npm run lint` | 1 | Same 36 baseline errors: 32 in existing `deploy/dist/assets` bundles and the same 4 unused declarations listed under task 1. No banner-file errors. | `lint.txt` |
| `npm run build` | 0 | Vite 6.4.3, 128 modules transformed; production build succeeded. | `build.txt` |

Additional checks: `npx eslint app/components/app-banner-component.js test/components/app-banner-component.test.js` and `git diff --check` both passed (exit 0).

Compared implementation changes from baseline commit `1054883c` to HEAD. Application changes are confined to the banner component and its CSS, with regression coverage confined to the banner test. The anchor uses exactly `https://apps.apple.com/us/app/qubigo/id6755611357`, no tracking parameters, target, or custom navigation handler; the dismiss button remains a sibling. Visibility detection, session key, dismiss handler, and hash-change logic are unchanged. CSS changes remain local, including safe-area padding, independent targets, underline, and focus styling. No upgrade-page, backend, dependency, or routing changes were introduced.

No new automated failures were observed. The full-suite and lint failures match the recorded pre-change baseline and were deliberately not fixed. Build output remains in ignored `qubigo-frontend/dist`; post-check git status showed only the new verification evidence, with no tracked generated or application changes. No deployment or release was performed.

## Task 5: current acceptance recheck and handoff

### Current doer execution — blocked

**Supplied finding (this attempt):** “The supplied update explicitly records no executed acceptance tests. Real iPhone/iPad listing handoff, layout/safe-area, keyboard/accessibility, visibility and independent session-dismissal evidence remains outstanding. Record device/browser details and actual matrix results; tooling observations, reviewer requests and plan checkbox corrections do not resolve this testability gap.” Recorded verbatim as an unchecked subtask of task 5 in the target-local `.factory/runs/qubigo-001/plan.md`. No physical-device session or tester results are available to this execution. No acceptance tests were executed and no matrix results changed. This finding cannot be fixed with the available tools and remains unresolved; task 5 is not done. To unblock, supply access to physical iPhone/iPad testing or actual tester-completed matrix evidence, plus owner visual approval. Existing local edits were preserved; no external files were changed.

The supplied validator finding at line 85 (documentation is not executed acceptance evidence) is recorded verbatim as an unchecked subtask of task 5 in `.factory/runs/qubigo-001/plan.md`. It remains unresolved. No real iPhone/iPad testing session or actual device results were supplied. Current executable/environment inspection found only Android `adb`, no `xcrun`, `idevice_id`, `ios`, or `appium`, and no configured display or remote device/browser session. This is availability inspection only: no acceptance matrix row was executed, no device/browser result is claimed, and the existing Chromium simulations are not real-iOS evidence. To proceed, provide an accessible physical iPhone/iPad testing session or tester-completed matrix results with device/OS/browser versions, routes, orientations, listing identity and supporting evidence. Owner visual approval is also still required.

Corrected target-local plan checkboxes that marked owner approval and unresolved manual-acceptance findings complete despite their explicit unresolved descriptions. Resolved historical findings remain checked. Task 5 stays open; a device-equipped tester must execute the matrix below and supply actual device/browser results, and the owner must approve the filled treatment. No product changes or frontend checks were needed for this documentation-only correction; existing automated evidence and unrelated failures remain as recorded. No commits or deployment performed.

**Latest validator finding:** “The new follow-up supplies tooling availability observations and reviewer requests, not executed acceptance evidence. Real iPhone/iPad listing handoff, layouts/safe areas, keyboard/accessibility, visibility and independent session-dismissal checks remain outstanding. Record device/browser details and actual matrix results to resolve the testability finding.” Recorded verbatim as an unresolved subtask of task 5 in `.factory/runs/qubigo-001/plan.md`. This execution has no physical iOS browser session or supplied tester results. No acceptance matrix row was executed or changed to passing; no device/browser details or results were invented. The finding cannot be resolved here. Task 5 remains open until a device-equipped tester supplies actual matrix results and the owner supplies visual approval.

This execution used the supplied plan at `/home/rob/dev/libby/q/.factory/runs/qubigo-001/plan.md` and updated only the target-local `.factory/runs/qubigo-001/plan.md`. The supplied plan is readable; historical missing-path statements below no longer describe the current contract. Previous-attempt validator finding: the handoff at line 87 contains availability checks rather than executed acceptance tests. Recorded verbatim as an unchecked subtask under task 5 in the target-local plan. It remains unresolved: no real-device acceptance evidence is available.

Read frontend conventions and attempted `yx list --ready` (command not found). Rechecked `command -v` for `xcrun`, `idevice_id`, `ideviceinfo`, `ios-deploy`, and `adb`: only Android `adb` is available. `DISPLAY` and `WAYLAND_DISPLAY` are unset. `adb devices -l` started a daemon and returned an empty attached-device list; the daemon was stopped afterward. These checks provide no usable real iPhone/iPad session. No physical-device, Safari, hardware-keyboard, VoiceOver or App Store listing check was performed.

**Current finding follow-up:** checked `command -v` for `xcrun`, `idevice_id`, `ideviceinfo`, `ios-deploy`, `tidevice`, and `pymobiledevice3`; none was found. `DISPLAY` and `WAYLAND_DISPLAY` are empty; `yx list --ready` is unavailable. USB bus directories exist but do not establish an accessible iOS browser session. These are tooling observations only, not executed acceptance tests. No device model, OS/browser version, tested orientation or actual iOS acceptance result can be supplied from this execution. No new device results were provided by the user. The supplied finding cannot be fixed by relabeling simulations or requesting review; it stays unchecked until a physical-device tester records actual matrix results. No seed acceptance pass is claimed.

Task 5 remains blocked on a device-equipped reviewer and product-owner visual approval. Existing Chromium simulations remain valid limited evidence, not real-device acceptance. The historical manual acceptance matrix below remains outstanding. No new approval or listing-identity evidence was supplied, and the unresolved task-5 manual-acceptance finding remains unchecked.

**Reviewer request:** on physical iPhone and iPad, execute the matrix below and return model, OS/browser versions, orientation, route, actual results and screenshots. Verify the approved US URL reaches Qubigo, app ID `6755611357`, by Rob Westgeest; independently verify dismissal/session persistence, route exclusions, safe-area layouts and keyboard/VoiceOver behaviour. **Owner request:** explicitly approve or reject the rounded filled banner treatment on your device. The earlier click-through report is not visual approval.

Documentation-only update: no product changes, dependencies, deployment or commits. Automated frontend checks were not rerun; task-4a results and documented unrelated baseline failures remain the latest full verification. `git diff --check` was run for this handoff. Task 5 is not marked done.

## Task 5: historical manual acceptance availability and reviewer handoff

Task 5 remains in progress. Tasks 1–4 have since been marked done by the planner in the local plan; earlier task sections above are historical execution records.

### Environment and available evidence

- Rechecked the requested factory plan with the read tool: `ENOENT`. The available plan is `docs/iterations/001-clickable-app-banner-plan.md`. The exact-path finding remains blocked pending factory action outside the permitted target directory.
- `yx list --ready` still fails with command not found. No commits or task synchronization performed.
- Checked executable availability with `command -v chromium chromium-browser google-chrome firefox playwright xcrun idevice_id adb`: only Android `adb` is available. No local browser, Apple simulator tooling, or iOS device CLI was found. `DISPLAY` and `WAYLAND_DISPLAY` are unset. USB bus entries alone do not establish access to an iPhone/iPad; no real iOS device/browser session is available to this agent.
- No installed `@playwright` or `puppeteer` package was found in the frontend. No dependencies installed. Browser emulation, even if available, would not prove an App Store handoff.
- Reran `npm test -- --runInBand test/components/app-banner-component.test.js` from `qubigo-frontend`: exit 0, 1 suite / 18 tests passed. This covers destination, current-context anchor semantics, independent dismissal, session persistence, visibility exclusions, iPadOS detection, and hash changes. It is automated evidence only.
- Re-read the current component and CSS: approved fixed HTTPS destination, purpose-revealing link name, sibling dismiss button, underline, focus-visible outline, separate 44px minimum targets, wrapping, and safe-area padding are present. Static inspection is not manual acceptance.

### Manual acceptance matrix — all outstanding

No device, OS/browser version, orientation, or tested runtime route can honestly be recorded as tested in this environment. A reviewer with physical iPhone and iPad access must fill in those fields and actual results below; do not substitute DOM test results for them.

| Check | Required execution and evidence | Result |
| --- | --- | --- |
| iPhone layout | Record model, iOS/Safari version, `#/signin`, narrow portrait and landscape. Confirm readable message, separate usable tap targets, no overlap/clipping, and safe-area clearance. | Outstanding — no device |
| iPad layout/detection | Record model, iPadOS/browser version, portrait/landscape and desktop-site mode on `#/signin`. Confirm banner appears, readable wrapping and control clearance. | Outstanding — no device |
| Route visibility | On each device, start undismissed at `#/signin`, enter `#/game/acceptance-check`, then return. Banner must hide on the game route and reappear before dismissal. | Outstanding — no runtime browser |
| Keyboard/accessibility | With hardware keyboard, Tab to link and dismiss; confirm visible focus and native Enter activation. Inspect VoiceOver link purpose (Qubigo/App Store) and independently named dismiss control. | Outstanding — no keyboard/browser/AT session |
| Real destination | Tap the link on each device. Record approved URL `https://apps.apple.com/us/app/qubigo/id6755611357`, actual handoff, listing title Qubigo, app ID `6755611357`, developer Rob Westgeest. Verify the approved US listing; do not replace it with another storefront URL. | Outstanding — no real iOS handoff |
| Independent dismissal | In a fresh undismissed session, tap ×. Confirm no navigation, banner hidden, `app-banner-dismissed` set; navigate between routes and reload/recreate in the same tab/session, confirming it stays hidden. | Outstanding — no runtime browser |
| Non-iOS/native exclusions | Record desktop/Android browser and Capacitor device/build versions; confirm no banner. Existing automated coverage passes, but runtime checks are still required where available. | Outstanding — no runtime session |

Use a fresh browser session or clear only `app-banner-dismissed` before undismissed checks. After visiting the store, return to the web page and confirm link activation did not set the dismissal key. For each completed row, record device/browser versions, route, orientation, actual result, and supporting notes/screenshots; record failures rather than assuming success.

### Latest validator recheck — blocked, not resolved

Both supplied findings are recorded as unchecked subtasks under task 5 in `001-clickable-app-banner-plan.md`; neither can honestly be marked resolved.

- **Missing supplied plan:** reading `/home/rob/dev/libby/factory/runs/qubigo-001/plan.md` again returns ENOENT; `test -e` exits 1. A target-local document does not repair that path. Restoring it requires factory action outside the permitted target directory. No replacement external plan or symlink was created.
- **Missing manual execution evidence:** executable checks again find only `adb`, not Chromium, Chrome, Firefox, Playwright, `xcrun`, or `idevice_id`. `DISPLAY` and `WAYLAND_DISPLAY` are empty. `adb devices -l` returns an empty device list; its automatically started daemon was stopped afterward. No real iOS browser, rendered-layout, hardware-keyboard/VoiceOver, or runtime acceptance session was executed. The outstanding matrix remains unchanged; no results were fabricated.

No product changes or repeated automated checks were necessary for these environment blockers. Required next actions remain: factory restores the external plan; a reviewer with iPhone/iPad access executes the matrix and records results. Task 5 remains in progress, and acceptance remains blocked.

### Handoff and validator findings

- The no-uncommitted-work finding is resolved by the new, uncommitted acceptance evidence and task-5 plan updates. The earlier findings and both latest validator findings are recorded as nested subtasks of task 5 in the local plan; the external-path and manual-execution findings remain explicitly unresolved. No new top-level tasks were added.
- Review these existing committed product files: `qubigo-frontend/app/components/app-banner-component.js`, `qubigo-frontend/app/components/app-banner.module.css`, and `qubigo-frontend/test/components/app-banner-component.test.js`. This task adds evidence only, not another banner redesign.
- Automated evidence: focused 18-test suite passes again. Full test, lint, build, and changed-JavaScript lint evidence remains in the task-4 section and `001-clickable-app-banner-verification/`; those broader checks were not rerun for this documentation-only task. Full tests/lint have the documented unrelated baseline failures; build passed.
- Factory follow-up: restore the requested external plan path using the local plan, then resubmit the uncommitted evidence for review. Reviewer follow-up: execute every outstanding manual row, particularly the real US App Store destination, and record actual results.
- Acceptance is **not complete**. Real-device handoff, rendered layouts, keyboard/assistive-technology behaviour, and runtime checks remain outstanding. No production deployment, app release, outside-target writes, or commits were performed.

## Task 4a: minimal app-style refinement (2026-10-11)

Implemented the first unfinished task in the supplied plan; progress recorded in target-local `.factory/runs/qubigo-001/plan.md`. Task remains open for the planner. Previous-attempt findings supplied: none (`[]`), so no finding subtasks were required.

- Read `qubigo-frontend/AGENTS.md`; `yx list --ready` still fails with command not found. No synchronization or commits performed.
- Inspected `main.css`, `forms.module.css`, `invitation-button.module.css`, and colour definitions in `main-layout.css`. Reused the existing pill-button radius, white fill, dark Qubigo text and bold typography. Chose white/darkgrey rather than the accent-blue/white pairing to retain strong contrast at the banner's small text size.
- Application change confined to `qubigo-frontend/app/components/app-banner.module.css`: rounded filled message link, bold text, no underline. No component/test/dependency changes or CSS-mirroring tests. Exact destination, current-context native anchor semantics, accessible purpose, wording and separate dismiss cross remain untouched.
- Static review: white (`#FFFFFF`) against darkgrey (`#20252C`) gives approximately **15.41:1** text contrast. Existing offwhite (`#DADDE2`) focus outline against darkgrey gives **11.32:1**. Focus outline remains outside the filled target with its existing 2px offset. Separate minimum 44px targets, wrapping, flex gap and safe-area padding are unchanged. This is static evidence, not rendered proof.

Required commands ran from `qubigo-frontend`; full logs retained in `docs/iterations/001-clickable-app-banner-refinement/`.

| Command | Exit | Result | Log |
| --- | --- | --- | --- |
| `npm test -- --runInBand test/components/app-banner-component.test.js` | 0 | 18 tests passed | `focused.txt` |
| `npm test -- --runInBand` | 1 | 65 suites passed, 1 skipped; 671 tests passed, 11 skipped. Same baseline post-summary asynchronous warning and `jumper.test.js:78` floating-point assertion (2 vs 2.0000000000000004). | `full.txt` |
| `npm run lint` | 1 | Same 36 baseline errors: 32 in existing deploy bundles and 4 unused declarations listed under task 1. | `lint.txt` |
| `npm run build` | 0 | Vite 6.4.3; 128 modules transformed; build succeeded. | `build.txt` |

`git diff --check` passed. Scope review found only the banner CSS change plus plan/evidence updates; generated build output remains ignored. No new automated failures observed; unrelated baseline failures were not fixed.

### Outstanding visual review and acceptance

Rechecked executable availability and browser caches: no Chromium/Chrome/Firefox/Playwright executable, cached browser, Apple simulator tooling or iOS device CLI found; no display session available. No rendered narrow portrait, landscape or iPad check was executed. The task-5 device matrix remains outstanding, including keyboard/VoiceOver, safe-area/control clearance and complete destination verification.

The product owner reports that clicking the previous banner reaches the App Store. This is successful owner-reported handoff only: device, OS/browser, orientation and listing identity were not supplied. It does not establish complete iPhone/iPad acceptance or visual approval of this refinement.

**Owner review requested:** please try the new simple rounded, filled treatment and confirm whether it feels app-like and invites clicking. A device-equipped reviewer must also record narrow portrait, landscape/iPad layouts, visible focus and separation from the dismiss cross. Acceptance is not complete. No deployment, release, commits or outside-target writes performed.

### Task 4a validator follow-up: rendered browser evidence (2026-10-11)

Recorded the supplied medium-severity finding at `app-banner.module.css:30` as a nested subtask of task 4a. Resolved the missing **rendered browser evidence** by running the actual Vite-served application and unmocked banner CSS in headless Chromium **156.0.8078.4**, via Playwright **1.64.0** on Linux. This supersedes the earlier statement that no browser check could be executed. It does **not** resolve real-iOS acceptance or owner visual approval; task 4a remains in progress.

Evidence directory: `docs/iterations/001-clickable-app-banner-refinement/`. Retained executable `rendered-check.cjs`, complete `rendered-results.json`/`rendered-check.txt`, and three PNGs per layout: normal, `-link-focus`, `-dismiss-focus`. Screenshots were visually inspected for readable text, rounded filled affordance, distinct cross and visible focus. Actual route: `http://127.0.0.1:5173/#/signin`. iPhone UA and touch emulated for phone cases; Macintosh UA and touch for iPad desktop-mode cases. These are **viewport simulations, not physical devices or Safari**.

| Case / viewport (CSS px) | Simulated safe-area top/left/right/bottom | Rendered result |
| --- | --- | --- |
| Narrow portrait 320×568 | 0/0/0/0 | PASS: readable full wording, 242.25×44 link; separate cross; focus clear |
| Notched portrait 390×844 | 47/0/0/34 | PASS: controls begin at y=54.5, below simulated top obstruction; readable link and focus |
| Phone landscape 844×390 | 0/47/47/21 | PASS: link starts x=62; dismiss ends x=789.5, before right safe-area boundary x=797; focus clear |
| iPad portrait 768×1024 | 24/0/0/20 | PASS: controls begin y=31.5; readable text, separate targets and visible focus |
| iPad landscape 1024×768 | 24/0/0/20 | PASS: readable message, separate targets and focus; top clearance retained |

Safe-area values were set through Chromium CDP `Emulation.setSafeAreaInsetsOverride`, exercising the actual `env(safe-area-inset-*)` CSS rather than replacing styles. Recorded computed padding confirms the overrides took effect. All cases have an 11.25px gap between targets, independent 44×44 dismiss control, 44px link height, and text bounds entirely inside the link. The 2px outline with 2px offset fits inside the banner and does not touch the other target or simulated unsafe region. The rendered text is dark on white, with a clearly contrasting offwhite focus ring on dark background. Tablet screenshots also show the fixed banner overlaying part of the existing page logo; this pre-existing fixed-banner behaviour is outside this CSS refinement, not a link/cross overlap.

Actual keyboard events: first Tab focuses the native link with `:focus-visible`; second Tab focuses the dismiss cross with its visible outline. Enter on dismiss hides the banner without URL change, writes the session key, and remains hidden after reload in all five contexts. Assertions and screenshot capture exited **0**. Focused Jest suite rerun: **18 tests pass**, exit 0 (`focused-rendered-followup.txt`). `git diff --check` passes. Broader test/lint/build results above remain applicable: no additional application change was needed for this finding.

Tooling was downloaded temporarily under `.factory/browser-tools` without changing frontend dependencies/lockfiles: `npm install --prefix .factory/browser-tools --no-package-lock --no-save playwright@1.64.0`, then `PLAYWRIGHT_BROWSERS_PATH="$PWD/.factory/browser-tools/browsers" .factory/browser-tools/node_modules/.bin/playwright install chromium`. Missing shared libraries were downloaded as Fedora RPMs and extracted locally (not installed system-wide). Run with Vite on port 5173:

```sh
LD_LIBRARY_PATH="$PWD/.factory/browser-tools/libs/usr/lib64" \
PLAYWRIGHT_BROWSERS_PATH="$PWD/.factory/browser-tools/browsers" \
node docs/iterations/001-clickable-app-banner-refinement/rendered-check.cjs
```

Temporary browser/tool downloads and Vite process were removed/stopped after capture. Retained only review evidence and the reproduction script. Real iPhone/iPad Safari safe-area behaviour, hardware keyboard/VoiceOver, store handoff with confirmed listing identity and owner approval remain outstanding under the existing device matrix; emulated Chromium results are not substituted for those checks. **Owner: please review the retained screenshots and the banner on your iOS device and confirm whether the treatment invites clicking.** No commits, deployment or release performed.
