# Iteration 004 implementation evidence

## Task 1 — durable admin competition drafts (2026-10-16)

Implemented in the uncommitted working tree based on `df120f51`. Task completion
is reserved for the planner after the factory commits accepted work. No publish,
enrollment, matching, results, native-device or product-approval claim is made.

### Delivered behavior

- Admin profile links to `#/admin/competitions/new`. A successful save opens
  `#/admin/competitions/<id>`; that durable URL supports reload and editing.
- POST `/api/admin/competitions`, GET/PUT `/api/admin/competitions/{id}` require
  the existing ADMIN role. Anonymous requests receive 401; ordinary players 403.
  The page resolves the existing profile before requesting draft details and
  does not show forms/data to non-admins.
- Draft fields: name, closing instant, positive integral game allowance,
  nonnegative advisory minimum. Zero minimum is supported. Names are plain text,
  1–120 characters, rejecting blank/control/markup characters (`<`, `>`, `&`).
  UI values and messages use DOM value/textContent, not interpolated markup.
- Closing input explicitly requires ISO date/time with UTC offset. Detail shows
  browser-local time, timezone abbreviation and IANA zone. Explicit offsets
  distinguish the repeated DST hour; the backend normalizes to UTC. The browser
  sends the original text, allowing strict server calendar validation and keeping
  nanosecond precision when an unrelated field is edited. SQLite TEXT round trips
  `2026-11-07T15:00:00.123456789Z` exactly. This establishes draft storage precision,
  not yet game-journal/ranking precision.
- Flyway adds a separate competitions table without modifying ordinary data.
  Prepared JDBC statements implement the repository port. A single conditional
  UPDATE checks persisted DRAFT state and expected revision atomically, so stale
  edits cannot overwrite another edit or a concurrent publication. Publication
  itself is task 2; this task exercises its persisted state in a fixture.
- Invalid edits do not write; database failures preserve prior values. UI failures
  retain entered values and show retryable errors, never a saved message. Detail
  failures disable editing and offer reload. Duplicate submits and late responses
  after navigation do not create false success or replace another page.

### Verification

Environment: Linux x86_64, Gradle wrapper 8.14.4, Temurin JDK 21.0.12.1+1,
Node v24.18.0, npm 11.16.0, existing installed node_modules.
The default JDK 25.0.4 fails Gradle configuration (`What went wrong: 25.0.4`).
JDK 21 was downloaded from Adoptium to `/tmp/qubigo-jdk21`; no project toolchain
or dependency configuration was changed. Backend commands below used:

```sh
cd qubigo-backend
export JAVA_HOME=/tmp/qubigo-jdk21 PATH=/tmp/qubigo-jdk21/bin:$PATH
```

| Command (in relevant service directory) | Observed result |
| --- | --- |
| `./gradlew :qubigo-servlet:test --tests '*CompetitionDraftTest'` before implementation | Expected compile failure: Competition/DraftFields did not exist. |
| `npm test -- --runInBand test/competitions/pages/competition-draft-page.test.js` before implementation | Expected missing page/module failure. |
| Precision/invalid-calendar UI regressions before fix | 5 failures; browser normalization lost original offset/precision and changed invalid dates. |
| `./gradlew :qubigo-servlet:test --tests '*Competition*Test'` | PASS: 50 cases across domain (13), conversion (14), MVC (17), SQLite (6). |
| `./gradlew test` | PASS: servlet 765, CLI 18; no failures/errors/skips. |
| `npm test -- --runInBand test/competitions` | PASS: 2 suites, 18 tests. |
| `npm test -- --runInBand` | Exit 1 after summary: 67 passed suites, 1 skipped; 719 passed tests, 11 skipped. Unhandled asynchronous jumper assertion described below. NOT a passing full command. |
| `npm run lint` | Exit 1: 36 existing errors, none in changed files; baseline comparison below. |
| `npx eslint app/competitions test/competitions app/app.js app/pages/profile-page.js` | PASS. |
| `npm run build` | PASS: Vite 6.4.3, 132 modules transformed. |
| `git diff --check` | PASS. |

SQLite tests migrate an empty file database through all migrations, reopen it
with a new datasource/repository, and round-trip edited values. A separate
upgrade fixture applies the previous migration target, inserts a user, ordinary
invitation and game event, applies the new migration, and asserts their values
survive. State/revision conflicts and a SQLite ABORT trigger prove failed edits
leave original values unchanged. No sleeps or clock polling were added.

Backend reports: `qubigo-backend/build/reports/{qubigo-servlet,qubigo-cli}/tests/test/`
and corresponding `test-results/test/` XML. Existing `build-reports/` is not the
current Gradle output and was not used for final counts.

### Independently reproduced frontend baseline failures

Archived unmodified HEAD `df120f51` under `.factory/qubigo-004-baseline`, reused
installed node_modules and the unchanged deploy/dist assets, and ran the same
commands with the same Node/npm. Removed the disposable baseline afterward.

- `npm test -- --runInBand`: also exit 1, after 65 passed suites / 701 passed tests
  and the same 1 skipped suite / 11 skipped tests. Both runs end in
  `test/domain/jumper.test.js:78`: expected `2`, received `2.0000000000000004`,
  emitted asynchronously through `test/utils/eventually.js`. The new work adds
  exactly 2 passed suites and 18 passed tests. No skipped tests count as evidence.
- `npm run lint`: also exit 1 with 36 errors. Outputs compare exactly after
  normalizing the baseline directory prefix. Errors are in the unchanged
  game-list-component unused import, generated deploy/dist bundles, and unused
  imports in confirm-registration-page, confirm-registration-success-page and
  router tests. No baseline errors were waived as passes or changed in task 1.

The full frontend commands remain non-green for these reproduced unrelated
baseline defects; the eventual integration acceptance must retain/review this
limitation rather than infer a green suite. No physical-device or rendered
browser acceptance was required or performed for this draft increment.

### Validator follow-up — pending Router navigation

The validator found that Router did not own a page until its asynchronous open
finished. The prior manual-close test bypassed that collaboration. Replaced it
with two deferred-request cases through the actual Router and ProfilePage:
leave the draft during profile resolution or detail loading, then resolve the
request. Both tests failed before the fix (profile overwritten; detail rejected
with `Cannot set properties of null (setting 'value')`).

Router now assigns ownership before calling open and does not assign it again
after awaiting initialization. Navigation therefore closes pending draft visits,
activating their existing visit/generation guards. The regressions assert that
late opens resolve without error, Profile remains unchanged, and the next
navigation closes Profile rather than a superseded draft page.

Verification on Node v24.18.0 / npm 11.16.0:

| Command in `qubigo-frontend` | Observed result |
| --- | --- |
| `npm test -- --runInBand test/competitions/pages/competition-draft-page.test.js` before Router fix | 2 failed, 15 passed; both validator reproductions. |
| `npm test -- --runInBand test/competitions test/router.test.js` | PASS: 3 suites, 27 tests. |
| `npx eslint app/router.js app/competitions test/competitions` | PASS. |
| `npm test -- --runInBand` | Exit 1: 67 passed suites, 1 skipped; 720 passed tests, 11 skipped, followed by the same asynchronous jumper assertion recorded above. |
| `npm run lint` | Exit 1: same 36 baseline errors; none in changed files. |
| `npm run build` | PASS: 132 modules transformed. |
| `git diff --check` | PASS. |

No backend changes were made in this follow-up; prior backend evidence remains
above. Task 1 remains open pending factory commit and planner acceptance.

## Task 2 — publish and share a read-only invitation (2026-10-16)

Implemented in the working tree; task acceptance remains with the planner after
factory commit. No enrollment, automatic closure, matching, native association,
physical scanning or device acceptance is claimed by this increment.

### Delivered behavior

- Admin POST `/api/admin/competitions/{id}/publish` transitions a future-deadline
  DRAFT to WAITING and increments its revision. Existing ADMIN security protects
  this action; anonymous and ordinary-player requests are rejected.
- Publication obtains SQLite `BEGIN IMMEDIATE` on a dedicated JDBC connection,
  reads the persisted draft, then samples the injected authoritative clock and
  invokes domain validation under the write boundary. Equality/past deadline,
  non-draft state and an existing WAITING/RUNNING competition reject publication.
  SQLite's partial unique index also enforces the single-active constraint across
  connections/processes. Drafts are not restricted by that index. Failures roll
  back state/revision; lock/storage errors return retryable server failures.
- Conditional draft edits continue checking persisted DRAFT/revision; all fields
  become read-only after publication. The admin UI explicitly publishes the saved
  draft, not unsaved input, explains the permanent deadline/allowance lock, and
  shows a share link and QR only after authoritative successful publication or
  detail reload. Failure leaves the draft retryable and never shows success.
- Anonymous GET `/api/competitions/{id}/invitation` exposes an explicit DTO containing
  only ID, name, closing instant, allowance, state and rules. Drafts and unknown
  IDs both return 404. No revision, advisory management minimum, roster or standings
  is exposed. This read cannot create membership or an invitation/game.
- Public `#/competitions/{id}` renders that metadata and the rules as text, with
  local closing time including zone. Rules summarize full-board play, 3/1/0,
  points-before-speed/shared ties, allowance/prize eligibility, own-turn closure
  loss, pending-invitation/no-game/no-result, explicit join and manual start.
  This task's public page is deliberately read-only; account continuity and join
  controls belong to tasks 3–4. Unknown/private/network failures offer retry;
  late requests cannot overwrite a page after navigation.
- Share links use the configured public `VITE_API_URL`, as existing invitation
  sharing does, rather than a native WebView localhost origin. Production is
  `https://app.qubigo.com`, development `https://qubigo.labs.qwan.it`. Deployment
  must keep that base address serving the frontend. QR generation is local using
  `qrcode-generator` (no external QR service); `jsqr` is test-only. The decoder
  test reads the rendered canvas pixels and asserts the exact escaped URL.
  A real-Router regression also verifies decoded special-character identity
  reaches the repository without double encoding. This initially failed before
  adding one decode at the route/page boundary.

### Verification

Environment unchanged: Linux x86_64, Gradle 8.14.4 with Temurin JDK 21.0.12.1+1
at `/tmp/qubigo-jdk21`, Node v24.18.0, npm 11.16.0. Backend commands used
`export JAVA_HOME=/tmp/qubigo-jdk21 PATH=/tmp/qubigo-jdk21/bin:$PATH` as above.

| Command (in relevant service directory) | Observed result |
| --- | --- |
| `./gradlew :qubigo-servlet:test --tests '*CompetitionDraftTest'` before domain implementation | Expected compile failure: unresolved `publish`. |
| `npm test -- --runInBand test/competitions/pages/competition-invitation-page.test.js` before implementation | Expected missing invitation-page module failure. |
| Same public-page command before route decoding fix | 1 failed, 5 passed: repository received encoded identity instead of original identity. |
| `./gradlew :qubigo-servlet:test --tests '*Competition*Test'` | PASS: 65 cases (17 domain, 14 conversion, 24 MVC, 10 SQLite). |
| `./gradlew test` | PASS: servlet 780, CLI 18, zero failures/errors/skips. |
| `npm test -- --runInBand test/competitions` | PASS: 3 suites, 30 tests. |
| `npx eslint app/competitions test/competitions app/app.js` | PASS. |
| `npm test -- --runInBand` | Exit 1: 68 passed suites, 1 skipped; 731 passed tests, 11 skipped, followed by the previously reproduced asynchronous jumper assertion (expected 2, received 2.0000000000000004). Not a full-suite pass. |
| `npm run lint` | Exit 1: same 36 previously documented baseline errors in unchanged files/deploy bundles; no changed-file errors. |
| `npm run build` | PASS: Vite 6.4.3, 135 modules transformed. |
| `git diff --check` | PASS after removing one extra EOF blank line. |

SQLite tests use actual Flyway file databases and independent connections with
barrier-synchronized threads, no sleeps: exactly one concurrent publication
succeeds and recreated repositories read one WAITING/one unchanged DRAFT.
Separate assertions cover deterministic second-publication conflict, expired
publication rollback, injected write failure preserving the draft and stale edit
protection. Existing migration tests still prove ordinary data survives upgrades.
The frontend additions account for 11 tests/one suite over accepted task-1
follow-up evidence. The backend additions account for 15 tests. Skipped frontend
tests are not acceptance evidence. `npm install` reported 48 audit findings in the
installed dependency tree; no broad dependency remediation was attempted here.

Final test reports remain under
`qubigo-backend/build/reports/{qubigo-servlet,qubigo-cli}/test-results/test/`.
The plan's task 2 remains unchecked for factory commit/planner review.

## Task 3 — explicit enrollment and admin roster (2026-10-16)

Implemented in the working tree; task 3 remains unchecked pending factory commit
and planner acceptance. No matchmaking, start action, account-context retention,
deployment restart, device evidence or product approval is claimed here.

### Delivered behavior

- Authenticated POST `/api/competitions/{id}/join` resolves the current User and
  returns the same durable membership on repeated requests while enrollment is
  open. ID/name are obtained from the server, never from request fields. The
  response includes competition ID, own user ID, original joining instant and
  `WAITING` status; no roster, rankings or automatic matching is returned.
- Existing onboarding has no separate verified flag: email confirmation promotes
  a pending UserRegistration into a User. Pending registrations cannot authenticate
  as users. The command additionally rejects an authenticated identity with no
  established User (403). Existing established users remain eligible; no new
  verification schema or migration of existing accounts was invented.
- `Competition.allowEnrollment` owns eligibility: only WAITING/RUNNING, strictly
  before cutoff at full authoritative Instant precision. Draft/closed and
  equality/past cutoff fail with useful conflicts; unknown IDs return 404.
  Advisory minimum never starts a competition. Persisted RUNNING fixtures prove
  late entry without claiming that the later start UI has shipped.
- A Flyway table stores unique `(competition_id, user_id)` membership with name
  at enrollment and original timestamp. Participant names follow the existing
  snapshot approach and work with either persisted or dev-profile in-memory Users;
  the membership adapter does not reach into Users storage to reconstruct names.
- Publication and membership writes share an extracted SQLite `BEGIN IMMEDIATE`
  boundary. Time is sampled only under that lock, domain validation precedes the
  insert, and failure rolls back. Duplicate joins use targeted ON CONFLICT DO
  NOTHING, then read the original membership. Publication concurrency tests still
  pass after extraction. No in-memory-only lock or auto-start behavior was added.
- Admin GET `/api/admin/competitions/{id}/participants` returns enrolled names,
  IDs, joining times and count, not emails/standings. Existing ADMIN security
  protects all methods/direct paths. Unknown competitions do not fabricate empty
  rosters; anonymous/ordinary-player roster requests fail. No public roster route
  exists.
- The published admin detail links to `#/admin/competitions/{id}/participants`.
  The roster page resolves admin access before making a request, renders names
  with textContent, shows count/empty state, and offers manual refresh/retry.
  It does not claim a failed read is an empty roster. Pending profile/detail
  responses cannot overwrite another page, verified through the real Router.
- The existing public invitation now offers an explicit Join competition button
  for WAITING/RUNNING, with sign-in/verification guidance, duplicate-click guard,
  waiting acknowledgment only after success, and retryable failure messaging.
  Reading alone never joins. Closed invitations offer no join action; the server
  independently enforces deadlines even if a page was loaded earlier. Late join
  responses after navigation are ignored. Retaining the intended competition
  through account flows remains task 4; this increment makes no such promise.

### Verification

Environment unchanged: Linux x86_64, Gradle wrapper 8.14.4 with Temurin JDK
21.0.12.1+1 at `/tmp/qubigo-jdk21`, Node v24.18.0, npm 11.16.0. Backend commands
used `export JAVA_HOME=/tmp/qubigo-jdk21 PATH=/tmp/qubigo-jdk21/bin:$PATH`.

| Command (in relevant service directory) | Observed result |
| --- | --- |
| `./gradlew :qubigo-servlet:test --tests '*CompetitionEnrollmentTest'` before domain implementation | Expected compile failure: unresolved `allowEnrollment`. |
| `./gradlew :qubigo-servlet:test --tests '*SqliteBasedCompetitionRepositoryTest'` before membership adapter | Expected compile failure: unresolved membership adapter. |
| `./gradlew :qubigo-servlet:test --tests '*Competition*Test'` before command/controller implementation | Expected missing JoinCompetition/controller compilation failures. |
| `npm test -- --runInBand test/competitions` before enrollment/roster pages | Expected failure: missing roster page, four missing join-method failures. |
| `npm test -- --runInBand test/competitions/adapters/api-based-competition-enrollment.test.js` before adapter | Expected module-not-found failure. |
| `./gradlew :qubigo-servlet:test --tests '*Competition*Test'` | PASS: 91 competition cases, including 26 new cases. |
| `./gradlew test` | PASS: servlet 806, CLI 18; no failures/errors/skips. |
| `npm test -- --runInBand test/competitions` | PASS: 5 suites / 44 cases (14 new). |
| `npx eslint app/competitions test/competitions app/app.js` | PASS. |
| `npm test -- --runInBand` | Exit 1: 70 passed suites, 1 skipped; 745 passed tests, 11 skipped, then the previously reproduced asynchronous jumper assertion (expected 2, received 2.0000000000000004). Not a full-suite pass. |
| `npm run lint` | Exit 1: same 36 documented baseline errors in unchanged sources/generated deploy bundles. Changed-file lint passes. |
| `npm run build` | PASS: Vite 6.4.3, 137 modules transformed. |
| `git diff --check` | PASS. |

Real SQLite tests cover simultaneous duplicate joins using independent connections
and a barrier, later duplicate timestamp retention, unknown/deadline-equal rejection,
no enrollment from invitation reads, advisory-minimum non-start, persisted RUNNING
late entry, and an AFTER INSERT abort trigger leaving no membership. Closing and
recreating adapter Spring bean contexts over the same migrated file retains the
membership/name; this is adapter context evidence, not deployment-style full-app
restart acceptance (task 15). Existing migration tests still preserve ordinary
users, invitations and game events. Domain examples additionally cover draft,
closed, past and just-before (one nanosecond) cutoff states. No sleeps were added.
Skipped frontend cases do not count as evidence. Backend XML reports remain under
`qubigo-backend/build/reports/{qubigo-servlet,qubigo-cli}/test-results/test/`.

## Task 4 — invitation context through account flows (2026-10-16)

Implemented in the working tree; acceptance remains with the planner after factory
commit. No native association or store-install continuity is claimed.

### Delivered behavior

- The existing `#/competitions/:id` invitation offers registration/sign-in, the
  existing `#/demo` tutorial, public API rules, allowance/deadline and explicit
  join. The existing global app banner is unchanged; no new installation funnel.
  Installation is optional. After installing or changing device/browser, the page
  advises scanning the original QR/reopening the original competition link.
- `IntendedCompetition` persists only the most recent competition identity in
  same-origin localStorage, never credentials, verification tokens or enrollment.
  It encodes a fixed competition route and rejects invalid/control/oversized IDs
  and malformed Unicode. Disabled/full storage falls back to current-session
  memory; reload/new-tab recovery then requires reopening the QR/link.
- Registration/confirmation preserve intent without changing account APIs or
  verification links. Successful sign-in/profile resolution at neutral root/sign-in
  returns to the invitation rather than home/welcome, without joining. Anonymous
  startup and failed sign-in remain at sign-in. Existing deep links always win,
  including a newer link opened during delayed profile resolution. Authentication
  now awaits the profile/navigation decision. Ordinary invited-by is unchanged.
- Only the explicit button enrolls. Existing duplicate-click suppression remains.
  Join failure retains intent; success clears it. Closed/expired invitations clear
  obsolete intent and offer no join; unknown/read errors are retryable and never
  claim membership. Client deadline presentation is advisory; existing serialized
  backend join validation remains authoritative. Cold/warm link protocol tests
  retain competition hashes/encoding, without claiming real OS association.

### Verification

Environment: Linux x86_64, Node v24.18.0, npm 11.16.0, Vite 6.4.3. Local acceptance
backend compilation used Gradle 8.14.4 and Temurin 21.0.12.1+1 (`/tmp/qubigo-jdk21`).
No backend production/test source or schema changed. Commands below run in
`qubigo-frontend` unless stated otherwise.

| Command | Observed result |
| --- | --- |
| `npm test -- --runInBand test/competitions/competition-account-flow.test.js test/competitions/adapters/intended-competition.test.js` before implementation | Expected missing-module failures for `IntendedCompetition`. |
| `npm test -- --runInBand test/competitions/adapters/intended-competition.test.js` before Unicode/quota fix | 2 failed / 8 passed: unpaired surrogate URIError; quota-exhausted storage lost session intent. Both fixed. |
| `npm test -- --runInBand test/competitions test/modules/user-profile-module.test.js test/modules/authentication.module.test.js test/adapters/incoming-link-handler.test.js test/pages test/Invitations/pages/invited-by-page.test.js test/router.test.js test/components/app-banner-component.test.js` | PASS: 24 suites / 302 tests, including ordinary onboarding, invited-by, startup, banner and router regressions. |
| `npm test -- --runInBand` | Exit 1: 72 passed suites / 769 passed tests, 1 skipped suite / 11 skipped tests, then the documented baseline async jumper assertion at `test/domain/jumper.test.js:78` (expected 2, received 2.0000000000000004). NOT a full-suite pass. |
| `npm run lint` | Exit 1: same 36 baseline errors in unchanged source/deploy bundles. Initial run found one new no-control-regex error; fixed and rerun. |
| `npx eslint app/competitions test/competitions app/app.js app/modules/authentication.module.js app/modules/user-profile-module.js test/adapters/incoming-link-handler.test.js ../docs/iterations/competition-acceptance/invitation-browser.cjs --config .eslintrc.js` | PASS. |
| `npm run build` | PASS: 138 modules transformed. |
| `git diff --check` (repository root) | PASS. |

Outputs: `/tmp/qubigo-004-task4-{red,storage-red,focused,full,lint,build,browser,compile}.log`.
Independent baseline reproduction is documented above. Skipped tests do not count
as evidence. No new sleeps/polling were added to the test suite.

### Disposable rendered browser journey

Local aids under `docs/iterations/competition-acceptance/` are not application/test
source or packaged code. `InvitationBackend.java` starts the real non-dev Spring
application with Flyway SQLite, seeds one synthetic organizer, and substitutes only
external push and email delivery. The production welcome/verification message body
is captured to a disposable file; account registration, confirmation, security,
invitation reads and joining remain real. There are no mocked HTTP app responses
or manually promoted entrants. Failed-run artifacts may contain synthetic tokens
and must not be published; successful-run artifacts are removed.

The Node runner starts that JVM with devtools restart disabled, and Vite on
localhost:5196 proxying `/api` to localhost:8096. Both ports must be free. The
same-origin proxy removes Origin on the backend hop: this is not cross-origin/CORS
deployment evidence. Local HTTP uses non-secure synthetic cookies, not production
cookie-security evidence. No product security configuration was changed.

Final run: PASS, exit 0, headless Playwright Chromium build 1248. Assertions:
public invitation and existing tutorial link → rendered registration → failed
pre-verification sign-in with guidance → actual captured email in a separate
same-origin tab → confirmation → reload → successful sign-in back at the original
invitation → another reload → exactly one explicit join and cleared intent.
There were zero join requests before that button. Expected anonymous 401s and
pre-verification login 400 were observed.

Reproduce from the repository root, substituting available tool paths:

```sh
export JAVA_HOME=/tmp/qubigo-jdk21
export PATH="$JAVA_HOME/bin:$PATH"
(cd qubigo-backend && ./gradlew -I ../docs/iterations/competition-acceptance/classpath.init.gradle :qubigo-servlet:competitionAcceptanceClasspath -PacceptanceClasspathFile=/tmp/qubigo-004-task4-classpath)
export ACCEPTANCE_CLASSPATH_FILE=/tmp/qubigo-004-task4-classpath
export PLAYWRIGHT_MODULE=/home/rob/.npm/_npx/e41f203b7505f1fb/node_modules/playwright
export LD_LIBRARY_PATH=/tmp/qubigo-002-browser-libs/usr/lib64
node docs/iterations/competition-acceptance/invitation-browser.cjs
```

Actual Android/iOS cold/warm association, physical-device fallback, and voluntary
store-install recovery remain task 16. Same-origin browser storage is not assumed
to transfer to another browser or newly installed native app. Protocol doubles and
this local browser run do not satisfy that human gate.

## Task 5 — Manual start and reserved first-round invitations

Implemented; task remains open pending factory commit and acceptance.

- Admin-only `POST /api/admin/competitions/{id}/start` delegates through
  `StartCompetition` and the assignment port. WAITING only, strictly before the
  UTC cutoff; suggested minimum never blocks start. The existing admin detail
  page offers manual start, disables duplicate submission and retains retryable
  waiting state on failure. No automatic start.
- `CompetitionMatching` owns eligibility, unseen-opponent preference, repeat
  fallback, randomized equal choices and combined absolute first/second imbalance
  minimization. The first player is the invitee. All reserved assignments count
  toward allowance/orientation; pending assignments exclude both players from any
  additional pairing. Zero/one entrants wait; odd rosters leave one waiting.
- Flyway adds durable invitation linkage and numbered participant slots, unique
  per member/slot and per active member. Existing `BEGIN IMMEDIATE` serialization
  includes the lifecycle change, existing invitation/participant rows and slot
  reservation. Failure anywhere rolls everything back; lock errors remain
  retriable, not partial success. Late running joins drive matching inside this
  same boundary; a failed pairing also rolls back that join.
- Invitations are persisted through the invitations adapter's narrow
  `TransactionalInvitationWriter`, on the caller's connection, and read by the
  existing invitation repository/list API. A JPA round-trip regression verifies
  UTC millisecond TEXT timestamps and both participants, including the test JVM's
  non-UTC zone. Invitation time is not game response time. The ordinary ten-day
  list filter is retained except for still-open RUNNING competition invitations;
  an older pending competition invitation is explicitly tested. No expiry,
  cancellation, release, re-pairing, or game creation is introduced here.
- Tests cover seeded choices and orientations, repeat fallback, unseen preference,
  capacity/active exclusions, zero/one/odd rosters, below-minimum manual start,
  invalid lifecycle/cutoff, admin authorization, rollback/retry, pending retention,
  late joins, concurrent starts and concurrent start/joins with adapter recreation.
  Invitation acceptance/link conversion is task 6; completion-triggered matching
  and startup/tick recovery are task 8, not claimed here.

### Verification

Environment: Linux x86_64, Temurin 21.0.12.1+1 at `/tmp/qubigo-jdk21`, Gradle
8.14.4, Node v24.18.0, npm 11.16.0. Backend commands use
`export JAVA_HOME=/tmp/qubigo-jdk21 PATH=/tmp/qubigo-jdk21/bin:$PATH`.
Backend commands below run in `qubigo-backend`; frontend commands in
`qubigo-frontend`.

| Command | Result |
| --- | --- |
| `./gradlew :qubigo-servlet:test --tests '*CompetitionMatchingTest'` before domain implementation | Expected compile failure: missing matching/start types and behavior. |
| `npm test -- --runInBand test/competitions/pages/competition-draft-page.test.js` before start UI | Expected red: 3 new cases fail, 20 existing cases pass. |
| `./gradlew :qubigo-servlet:test --tests '*Competition*Test' --tests '*SqliteBasedInvitationRepositoryTest'` | PASS, including real SQLite reservation/concurrency/rollback and existing JPA invitation reader. |
| `./gradlew test` | PASS: 836 servlet + 18 CLI cases; zero failures/errors/skips. |
| `npm test -- --runInBand test/competitions` | PASS: 7 suites / 70 cases. |
| `npm test -- --runInBand` | Nonzero: runner reports 72 suites / 773 cases passing, 1 suite / 11 cases skipped, then the existing unhandled jumper assertion (`Expected: 2`, `Received: 2.0000000000000004`, `test/domain/jumper.test.js:78`). Not a full-suite pass. |
| `npm run lint` | Nonzero: the same 36 pre-existing errors; no changed-file errors. |
| `npx eslint app/competitions/adapters/api-based-competition-drafts.js app/competitions/domain/competition-draft.module.js app/competitions/pages/competition-draft-page.js test/competitions/pages/competition-draft-page.test.js test/competitions/adapters/api-based-competition-drafts.test.js` | PASS. |
| `npm run build` | PASS (Vite 6.4.3). |
| `git diff --check` | PASS. |

### Validator follow-up — matching history fixtures

Replaced positional `ReservedPair` boolean flags in `CompetitionMatchingTest.kt`
with intention-revealing `completedPair` and `pendingPair` helpers. Helpers name
`active` explicitly; existing test behaviors and assertions are unchanged.
Using the backend toolchain above, reran
`./gradlew :qubigo-servlet:test --tests '*CompetitionMatchingTest'` and
`./gradlew test`: both PASS (CLI test task up-to-date). No frontend changes in
this follow-up.

## Task 6 — Accepting a competition invitation creates exactly one correctly labeled game (2026-10-17)

Acceptance creates one competition game with invitee-first turn at acceptance
clock, consumes the reserved slot (does not allocate a new one), and labels both
the invitation and game DTOs with the competition name. The underlying
`SqliteBasedCompetitionAcceptance` writes game_event_records, game_list_items,
game_participants, invitation archive, invitation_participant status and the
assignment `game_id` link inside the same serialized `BEGIN IMMEDIATE` transaction.
Any failure rolls all changes back atomically.

### New files

- `competitions/domain/CompetitionLabels.kt` — read-only interface with `None` sentinel.
- `competitions/adapters/repositories/SqliteBasedCompetitionLabels.kt` — label lookup via
  JOIN on `competition_assignments`.
- `competitions/adapters/repositories/SqliteBasedCompetitionAcceptance.kt` —
  `CompetitionInvitationAcceptance` implementation using transaction-bound JDBC,
  player validation, deadline gating, replay safety and rollback.
- `games/adapters/repositories/TransactionalGameWriter.kt` — persists the
  normal creation payload on the caller's serialized connection.
- `invitations/domain/CompetitionInvitationAcceptance.kt` — functional interface;
  null return means "not a competition invitation", delegating to ordinary creation.
- `competitions/adapters/competition-label.js` — escaped-text label utility.
- `test/competitions/competition-game-labels.test.js` — frontend rendering tests.

### Modified backend files

- `Competition.kt` — added `allowGameCreation(now)` guard.
- `GameDto.kt` — `OnlineGameDto`, `GameListItemDto`, `asGameDto`, `asGameListDto`,
  `asGameListItemDto`, `asMoveWasMadeDto` accept optional `competitionName`.
- `GamesController.kt` — injected `CompetitionLabels` to pass names into DTOs.
- `InvitationDtos.kt` — `InvitationDto`, `InvitationListEntryDto`, `asInvitationDto`,
  `asInvitationListDto` accept optional `competitionName`.
- `InvitationsController.kt` — injected `CompetitionLabels` for both endpoints.
- `InvitationsControllerBeans.kt` — wired `CompetitionInvitationAcceptance` and `Clock`.
- `AcceptInvitation.kt` — tries `competitionAcceptance.accept()` first; if null returned,
  proceeds with ordinary game creation. Competition decision never falls back.

### Modified frontend files

- `api-based-invitations-repository.js`, `invitation-list-component.js`,
  `invitation-list.js`, `invitation.js`, `invitations-module.js`,
  `accept-invitation-page.js` — added `competitionName` field/rendering.
- `game-dto-converters.js`, `game-list-component.js`, `game.js`, `gameList.js`,
  `game-page.js` — added `competitionName` field/rendering.

### Verification

Environment: Linux x86_64, Temurin 21.0.12.1+1 at `/tmp/qubigo-jdk21`, Gradle
8.14.4, Node v24.18.0, npm 11.16.0. Backend commands use
`export JAVA_HOME=/tmp/qubigo-jdk21 PATH=/tmp/qubigo-jdk21/bin:$PATH`.
Backend commands below run in `qubigo-backend`; frontend commands in
`qubigo-frontend`.

| Test scope | Command | Result |
| --- | --- | --- |
| Competition acceptance (real SQLite, fake clock) | `./gradlew :qubigo-servlet:test --tests '*SqliteBasedCompetitionRepositoryTest*'` | 43/43 pass, 0 failures |
| AcceptInvitation domain | `./gradlew :qubigo-servlet:test --tests '*AcceptInvitationTest*'` | 6/6 pass, 0 failures |
| Invitation controller | `./gradlew :qubigo-servlet:test --tests '*InvitationControllerGetInvitation*' --tests '*InvitationControllerAcceptInvitationTest*'` | 14/14 pass, 0 failures |
| Game controller detail/list | `./gradlew :qubigo-servlet:test --tests '*GameControllerGetGame*' --tests '*GameControllerGetGameList*'` | 13/13 pass, 0 failures |
| Game repository (transactional write) | `./gradlew :qubigo-servlet:test --tests '*SqliteBasedGameRepositoryTest*'` | Pass |
| Full backend suite | `./gradlew test --rerun-tasks` | PASS (servlet + CLI) |
| Game list component | `npm test -- --runInBand --testPathPattern='game-list-component'` | Pass |
| Invitation list component | `npm test -- --runInBand --testPathPattern='invitation-list-component'` | Pass |
| Accept invitation page | `npm test -- --runInBand --testPathPattern='accept-invitation-page'` | Pass |
| Competition game labels | `npm test -- --runInBand --testPathPattern='competition-game-labels'` | 4/4 pass |
| Full frontend tests | `npm test -- --runInBand` | 73/74 suites pass, 1 skipped (pre-existing jumper assertion); 782/793 tests pass, 11 skipped |
| Lint (changed files only) | `npx eslint app/Invitations/... app/adapters/... app/games/... app/competitions/...` | 1 pre-existing unused `GameType` import in game-list-component.js |
| Build | `npm run build` | PASS (Vite 6.4.3) |

### Acceptance criteria coverage

- **Game creation**: acceptance creates exactly one game_event_records row with
  invitee-first turn, acceptance timestamp, and slot numbers unchanged.
- **Transactional atomicity**: game list trigger ABORT rolls back game_event_records
  and leaves assignment.game_id null; link-update trigger ABORT rolls back archive,
  game_event_records and participant status unchanged.
- **Label persistence**: `SqliteBasedCompetitionLabels` recreated from fresh
  `JdbcTemplate` returns same name; ordinary IDs return null.
- **Authorization**: opposite player (not assigned to the invitation) cannot accept;
  non-competition IDs return null from the protocol, delegating to ordinary creation.
- **Repeat/concurrent safety**: CyclicBarrier concurrency returns identical
  `QResult` and a single game_event_records row. Serial replay after cutoff
  retrieves the existing game ID without creating another.
- **Deadline gating**: acceptance with `now >= closesAt` or after state change to
  CLOSED fails without side effects. Sub-millisecond pre-cutoff admission succeeds.
- **Invalid-state rejection**: CLOSED competition rejects even if clock regresses.
- **CompetitionInvitationAcceptance delegation**: `accept("ordinary",…)=null`;
  outer `AcceptInvitation` proceeds normally with `CreateGame` and archival.
- **UI error rendering**: failed acceptance displays `role="alert"` textContent and
  leaves Accept button enabled for retry.
- **Unsafe name escaping**: competition names with HTML tags render as escaped
  `textContent` in `.competition-label`; no child SVG/IMG elements exist.

No native device, push-delivery or final product approval evidence is claimed.

## Task 7 — normally finished competition games record one durable points result (2026-10-17)

Task 7 adds durable competition result recording for games that finish normally
(board full). It establishes the admission marker protocol for cutoff reconstruction
and fixes deterministic journal ordering.

### Changes

- **Fixed `JpaGameRepository.journalFor` ORDER BY** — Added `order by record.revision`
  to the JPQL query for deterministic event replay.
- **Flyway migrations** —
  - `V20261016160000__competition_admission_markers.sql`: adds `last_cutoff_revision`
    (INTEGER) and `cutoff_observed` (INTEGER NOT NULL DEFAULT 0) columns to
    `competition_assignments`.
  - `V20261016170000__competition_results.sql`: creates `competition_results` table
    with game_id PK, competition_id FK, player/opponent IDs, points (0/1/3), and
    finalized_at timestamp. Removed invalid FK reference to `game_event_records(aggregate_id)`
    (`aggregate_id` is not unique).
- **Domain types** — `CompetitionGameResult`, `CompetitionResultRepository`,
  `AssignmentAdmissionMarker`, `CompetitionAdmissionMarkerRepository` defined in
  `CompetitionAssignments.kt`. `CompetitionScoreTable` with 3/1/0 constants.
- **Result derivation** — `CompetitionGameResultDerivation.kt`: replays a game
  journal up to the cutoff revision, detects board-full via `GameField.isFull()`,
  and assigns 3/1/0 points based on score comparison.
- **Marker initialization at creation** — `SqliteBasedCompetitionAcceptance.kt`
  now writes `last_cutoff_revision = created.revision.value` and `cutoff_observed = 0`
  in the same serialized competition write boundary as game creation.
- **Result persistence with adapter-level `writeResult` helper** —
  `SqliteBasedCompetitionResultRepository.kt` now exposes a `writeResult(transaction, result, now)`
  method that accepts an already-open `JdbcTemplate`. The standalone `recordResult`
  opens its own `competitionWrite` and delegates to `writeResult`. This enables
  composition within an existing transaction (used by closure worker in task 9).
- **Marker queries** — `SqliteBasedCompetitionGameMarker.kt`: reads admission
  markers by invitation_id or game_id. Also implements `findCompetitionIdByGameId`
  for the finalizer to look up the parent competition.
- **CompetitionMoveWriter** — New `CompetitionMoveWriter.kt` provides the single
  authoritative implementation for persisting a competition game event, the game-list
  projection, and the admission marker update atomically in one `competitionWrite`
  transaction. Both `SqliteBasedGameRepository.saveCompetitionEvent()` and tests
  use this class, ensuring tests exercise the same production code path.
- **Single-connection competition write boundary** — For competition games, the
  move command (`SqliteBasedGameRepository.saveEvent()`) acquires the serialized
  `competitionWrite` once and persists the event, game-list projection
  (`game_list_items`, `game_participants`), and admission marker on the same
  JdbcTemplate connection. The authoritative `Instant.now()` is obtained inside
  the write boundary for cutoff comparison. Non-competition games continue
  unchanged via the JPA path.
- **Wiring fix** — `GameRepositoryConfig.gameRepository()` now injects
  `JdbcTemplate` into `SqliteBasedGameRepository` and passes an `isCompetitionGame`
  predicate to `GameRepositoryWithView` so that the duplicate `gameListView.save()`
  call is skipped for competition games (the game-list was already written in the
  competitionWrite).
- **Separate-transaction contract** — Game events are committed through the
  competition write boundary. Result persistence uses a separate competition
  write boundary (`BEGIN IMMEDIATE`). A result-write failure does not roll back
  game events; retry re-derives the same result idempotently.
- **MakeMove save-result propagation** — `MakeMove.kt` changed from
  `.onSuccess(gameRepository::save)` (which discards the save result) to
  `.map { result -> gameRepository.save(result).map { Success(result) } }`
  so that a `Failure` from `save()` propagates through the chain, preventing
  post-processors and notifications from firing on an uncommitted move.
- **CompetitionGameFinalizer as MakeMovePostProcessor** —
  `CompetitionGameFinalizer.kt` now implements `MakeMovePostProcessor` and is
  wired into `GameCommandBeans` as a `MakeMovePostProcessor` bean so that
  board-full competition games trigger result recording automatically after a
  move is committed. Failures from `recordResult` are logged (not discarded).
  Also adds `reconcileUnfinishedResults()` for startup/tick recovery of games
  whose result write failed or never ran.
- **Active slot release on result** —
  `SqliteBasedCompetitionResultRepository.recordResult` now updates
  `competition_assignment_slots SET active = 0` in the same transaction as the
  result insert, releasing both participants for matching (task 8).

### Tests

**Domain tests** (`CompetitionGameResultDerivationTest.kt`, 6 tests):
- `derive returns null for an incomplete game` — Board not full.
- `derive returns null for empty events` — Empty journal.
- `derive returns null when cutoff revision excludes all events` — Cutoff -1.
- `derive returns win-loss for a full-board game where first player has more points` — 3/0.
- `derive returns loss-win when second player has more points` — 0/3.
- `derive returns draw for equal scores on a full board` — 1/1.

**Integration tests** (`SqliteBasedCompetitionRepositoryTest.kt`, 14 new tests,
plus 6 marker-advancement tests using `CompetitionMoveWriter` shared helper):
- `admission marker is initialized on acceptance` — Marker set to 0 after game creation.
- `admission marker survives repository recreation` — Marker readable after recreated JdbcTemplate.
- `non-competition game has no admission marker` — Null for nonexistent game/invitation.
- `competition result records and retrieves a game outcome` — 3/0 stored and read back.
- `competition result is idempotent on duplicate record` — Only one result row.
- `competition results for a competition lists all entries` — List query returns results.
- `result persistence failure rolls back without losing game events` — ABORT trigger
  on competition_results fails result write but game_event_records survive.
- `pre-cutoff advances admission marker to move revision`
- `multiple pre-cutoff moves advance the marker to the latest revision`
- `at-cutoff move does not advance marker and sets cutoff_observed`
- `post-cutoff move does not advance marker after cutoff is observed`
- `clock regression after cutoff observation cannot advance marker`
- `admission marker survives repository recreation after moves`
- `atomic write failure on marker update leaves no game event`

**Game repository tests** (`SqliteBasedGameRepositoryTest.kt`, 2 new tests split
from combined ordered-journal test):
- `journal returns events ordered by revision with ORDER BY sensitivity` — 5 moves,
  strictly increasing revisions.
- `journal preserves timestamp through SQLite TEXT round trip` — Creation and move
  timestamps round-trip within 1 ms.

### Verification

Environment: Linux x86_64, Gradle 8.14.4 with Temurin JDK 21.0.12.1+1
at `/tmp/qubigo-jdk21`. Backend commands use
`export JAVA_HOME=/tmp/qubigo-jdk21 PATH=/tmp/qubigo-jdk21/bin:$PATH`.

| Test scope | Command | Result |
| --- | --- | --- |
| Domain derivation | `./gradlew :qubigo-servlet:test --tests '*CompetitionGameResultDerivationTest'` | 6/6 pass, 0 failures |
| MakeMove save propagation | `./gradlew :qubigo-servlet:test --tests '*MakeMoveTests*'` | All pass (includes save-propagation tests) |
| Competition integration | `./gradlew :qubigo-servlet:test --tests '*SqliteBasedCompetitionRepositoryTest*'` | 50/50 pass, 0 failures |
| Full competition suite | `./gradlew :qubigo-servlet:test --tests '*Competition*'` | All pass |
| Competition integration (admission markers) | `./gradlew :qubigo-servlet:test --tests '*SqliteBasedCompetitionRepositoryTest*'` | 65/65 pass, 0 failures |
| Ordered journal | `./gradlew :qubigo-servlet:test --tests '*SqliteBasedGameRepositoryTest*'` | 13/13 pass, 0 failures |
| Full backend | `./gradlew test` | PASS (servlet + CLI), 0 failures |

### Acceptance criteria coverage

- **Win (3), Draw (1), Loss (0)** — Domain derivation tests cover all three outcomes.
- **Board-full detection** — Derivation returns null for incomplete games.
- **Idempotent recording** — Duplicate result call produces exactly one row.
- **Separate-transaction contract** — ABORT trigger on result table leaves game events intact.
- **Ordered journal** — `JpaGameRepository.journalFor` now queries with `order by record.revision`.
- **Marker initialization at creation** — Acceptance sets last_cutoff_revision = 0 and cutoff_observed = 0.
- **Marker survives restart** — Recreated JdbcTemplate reads back same marker values.
- **Non-competition games unaffected** — Marker queries return null for non-competition IDs.
- **Save-result propagation** — MakeMove propagates save failures; post-processors
  and notifications are not called when save returns a Failure.
- **MakeMovePostProcessor chain** — Post-processors registered in
  `movePostProcessors` are called after a successful save.
- **CompetitionGameFinalizer as MakeMovePostProcessor** — Finalizer implements
  the `MakeMovePostProcessor` interface and is wired into the Spring bean
  container through `GameCommandBeans.competitionGameFinalizerPostProcessor`.
- **Active slot release on result** — `recordResult` releases both participants'
  slots by setting `competition_assignment_slots.active = 0` in the same
  transaction as the result insert.

### Validator findings resolved (2026-10-17, second attempt)

| Finding | File | Resolution |
| --- | --- | --- |
| 1 | `CompetitionMoveWriter.kt:30` | Changed `saveMove()` to use `gameWithEvent.event.timestamp.toInstant()` instead of a fresh `Instant.now(clock)`, ensuring the marker comparison uses the same timestamp as the persisted event. |
| 2 | `GameRepositoryWithView.kt:21` | Added `saveGameMove()` method that bypasses `@Transactional` by delegating directly to the inner repository. `MakeMove` now calls `saveGameMove()` for competition games (detected via `isCompetitionGame()`) and `save()` for non-competition games. |
| 3 | `CompetitionGameFinalizer.kt:76` | `reconcileUnfinishedResults()` now reuses `finalizeGame()` instead of duplicating derivation, and propagates actual save failures. Wired into a `CompetitionStartupReconciler` bean that runs on `ApplicationReadyEvent`. |
| 4 | `SqliteBasedCompetitionRepositoryTest.kt:707` | Removed test-only `saveMoveAndMarker()` method from `CompetitionMoveWriter`. Replaced `simulateCompetitionMove()` with `playCompetitionMove()` which reads the game from `game_event_records` via JDBC, makes a legal `game.move()` with an injected clock, and saves through `CompetitionMoveWriter.saveMove()` — the exact production path. |
| 5 | `SqliteBasedGameRepositoryTest.kt:57` | Ordering test now saves 5 moves in deliberately scrambled revision order (5, 1, 3, 2, 4) via JDBC, then asserts `journalFor()` returns them in strictly increasing revision order. Also asserts each expected revision at each index. |
| 6 | `SqliteBasedCompetitionResultRepository.kt:17` | Added 3 new integration tests: `writeResult inside existing competitionWrite commits result and releases slot`, `writeResult slot-release ABORT rolls back result and companion state`, and `writeResult is idempotent when composed inside competitionWrite`. |

### Updated test verification

| Test scope | Command | Result |
| --- | --- | --- |
| Domain derivation | `./gradlew :qubigo-servlet:test --tests '*CompetitionGameResultDerivationTest'` | 6/6 pass, 0 failures |
| MakeMove save propagation | `./gradlew :qubigo-servlet:test --tests '*MakeMoveTests*'` | 9/9 pass, 0 failures |
| Competition integration | `./gradlew :qubigo-servlet:test --tests '*SqliteBasedCompetitionRepositoryTest*'` | 60/60 pass, 0 failures |
| Game repository | `./gradlew :qubigo-servlet:test --tests '*SqliteBasedGameRepositoryTest*'` | 13/13 pass, 0 failures |
| Full backend | `./gradlew test` | PASS (servlet + CLI), 0 failures |

No native device, push-delivery, final product-owner approval or deployment restart
evidence is claimed by this increment.
