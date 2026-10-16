# Iteration 003 — Task 5 process acceptance and operational handoff

## Current status — criterion 6 reconciled (2026-10-16)

Under the seed's relaxed criterion 6, the [owner confirmation — dev, 2026-10-15](#owner-confirmation--dev-2026-10-15)
plus repository deployment configuration and disposable process replacement satisfies
this iteration's continuity evidence requirement. No further operator checklist is
required. Earlier blocked/checklist requests and retry assessments below are
historical and superseded, not active iteration acceptance blockers.

Evidence remains distinct: repository inspection establishes deployment configuration
and operational prerequisites; disposable JVM replacement and HTTPS browser checks
passed; the owner attests to a **dev same-container restart only**. That confirmation
is neither recreated-container nor production validation. **Live production database
and signing-key continuity across image replacement remains outstanding for the first
production release**, not verified here and not an iteration acceptance blocker.

## Result and scope

Separate backend JVM replacement acceptance **passed**, using real HTTP, production
SQLite refresh/user adapters, real Flyway migrations, and a disposable file database.
Repository deployment continuity prerequisites are verified below. **Representative
HTTPS browser acceptance passed.** Criterion 6 uses the accepted owner dev confirmation
linked above; criterion 7 is resolved by the browser evidence below. Historical
validator findings remain recorded under task 5 in the exact external plan.
No production deployment, release script, credentials inspection, or commit performed.
`yx list --ready` was retried and remains unavailable (`command not found`).

### Reproduce safely

From `qubigo-backend`, with an installed Java 21 JDK:

```sh
export JAVA_HOME=/path/to/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :qubigo-servlet:test --tests '*PersistentAuthenticationProcessTest' --rerun-tasks
```

The test itself is the reproducible process acceptance aid; no account, operator
DB path, signing secret, Docker daemon, frontend or external service is required.
Do not point the helper at any existing database or use it as a deployed server.
The helper is test-runtime-only and is not packaged in the application JAR.

`PersistentAuthenticationProcessTest` owns a JUnit `@TempDir`, whose
`acceptance.db` is the **same absolute SQLite file for all four JVMs**; other
scenarios/runs get different directories. The public, test-only signing configuration
is fixed by `RefreshTokenDatabaseFixture`, never taken from an operator secret.
Gradle provides the complete test runtime classpath to `ProcessBuilder`. The child
`SessionAcceptanceProcess` runs a new real application with production wiring,
DDL disabled, and fixture-only durations: **access 6000 ms, refresh 300000 ms**.
Production defaults remain 60000/1209600000 ms. Only external notifications are
mocked, preventing Firebase initialization/credential logging and external delivery.
The fixture now binds its random HTTP port to loopback only. Application output
and error streams are discarded, not copied into evidence. No cookie jars, JWTs,
keys, passwords, browser storage, raw HTTP bodies or credentials enter reports.

The first JVM seeds a disposable builder user, then credentials are submitted only
to the real `/api/loginjwt` endpoint. Two logins cross an observed one-second JWT
boundary (3-second polling bound) to guarantee distinct sessions. The parent keeps
session values in memory with redacted `toString`; subsequent JVMs never seed users,
log in, copy token records, or receive session values on command lines. The
`ready` file contains only the ephemeral port. Readiness requires the live child,
port publication, and an actual protected HTTP request returning 401 within a
60-second startup bound; every request has a 10-second timeout.

Closing child stdin requests complete application/context/Hikari shutdown. Each
child must exit 0 within 20 seconds **before** the next starts. Failure cleanup
forcibly terminates a stuck child and reports failure, not successful termination.
A failed assertion also closes the process. Only test-owned temporary data is
cleaned up; no operator/pre-existing database is deleted.

## Actual execution evidence

Executed with Java 21.0.12.1 at `/tmp/qubigo-003-jdk`, application version
**1.9.2-local**, current source plus this task's test-only changes.

```sh
# From qubigo-backend, after setting JAVA_HOME/PATH as above:
./gradlew :qubigo-servlet:test --tests '*PersistentAuthenticationProcessTest'
./gradlew test
./gradlew build
# From the target root:
git diff --check
```

Final focused run: **1 passed**, 31 seconds. Final full run: **723 servlet + 18
CLI tests**, zero failures/errors/skips, 1m26s. Build passed in 1s (tests
up-to-date). Diff whitespace check passed. Existing Gradle/JVM warnings remain;
no unrelated baseline failures. Full-suite process evidence recorded these distinct
backend PIDs: **625464 → 625537 → 625610 → 625680**. These are backend JVMs,
not Spring context IDs or the Gradle test worker.

- First JVM: real credential login, protected `GET /api/games` returns 200,
  distinct second session issued. Clean shutdown **before original access expiry**
  is explicitly asserted.
- Backend stays stopped until original access JWT `exp` + 1 second, with bounded
  50-ms polling (10-second maximum). Observed original absolute access expiry:
  **1792095717000 ms**; refresh expiry: **1792096011000 ms**. These are expiry
  observations, not bearer values.
- Second and third JVMs: old access token returns **401** on `/api/games`;
  original refresh token returns **200** on `/api/refresh`; returned refresh
  identity and absolute expiry equal issuance; new access token returns **200**
  on `/api/games`. Both sessions refresh successfully. No credential login repairs
  either session. Third JVM then logs out using renewed access: **200**.
- Fourth JVM: **both** original user sessions return **401** on refresh, with
  neither an access-token response field nor a new cookie. Revocation persisted.
- All four JVMs passed bounded readiness, exit-zero/clean termination checks.

This verifies process replacement against the same file/key, not an image rollout,
remote host, browser cookie delivery, Android/iOS client, or production operation.
The Java HTTP client deliberately sends cookie headers; that is not browser evidence.

### Defect sensitivity and resolved setup findings

No production implementation increment was necessary in this task. Initial fixture
setup assumed a `URLClassLoader`, which the Java 21 Gradle worker does not expose;
that setup failure is **not** behavioral red evidence. Passing the full test runtime
through a Gradle system property fixed it. An intermediate eager runtime-classpath
resolution ran before repository configuration and failed Gradle setup; resolution
now occurs in `Test.doFirst`. An initial root-level `./gradlew` invocation failed
because the wrapper is in `qubigo-backend`; all reported runs use that directory.

The initial runnable test passed. Then temporarily changed **only** production
refresh repository wiring to memory and reran the focused command: **1 behavioral
failure**, expected refresh 200 but received 401 at the first replacement
(`refresh`, line 77). Restored SQLite wiring immediately; `SecurityConfig.kt` has
no final diff. Final focused/full runs above passed. This mutation proves sensitivity
to the original process-memory defect, not a new preimplementation red/green claim.

## Deployment configuration: source verified, live state not verified

Reinspected `donstro/docker-compose.yml`, `release-backend.py`,
`qubigo-backend/{Dockerfile,run.sh}` and main `application.yml`:

- Release changes the image tag, then calls external `donstro_single up`. No
  release command was run; `donstro_single` is unavailable locally. Docker itself
  is available, but that does not grant evidence about the live deployment host.
- Donstro mounts named volume `prod_data` at `/opt/qubigo-backend/data`. Runtime
  WORKDIR and `run.sh` use `/opt/qubigo-backend`; absent an override the datasource
  resolves to `/opt/qubigo-backend/data/qubigo.db`. Compose declares no
  `QUBIGO_DB_PATH` override. The image does not bake in or recreate that database.
- `JWT_SECRET_KEY` is passed through Compose and consumed by `qubigo.jwt.key`.
  There is no per-image key generation. This establishes the source mechanism,
  **not** that the operator supplies the same value on every release.
- Keep the Compose project/volume identity and signing configuration unchanged.
  Do not run `down -v`, prune the volume, change to an ephemeral datasource, or
  regenerate the key. Preserve runtime-user write permissions. Initial rollout
  requires **one final sign-in**; old memory-only tokens are not migrated and
  unknown signed tokens are never adopted.
- Replacing/deleting the database loses recognition; changing the signing key
  invalidates existing JWTs despite retained rows. Source infrastructure does not
  prove a separately retained host disk; host replacement requires verified
  backup/restore beyond ordinary image replacement.

**Operator check pending:** on the actual deployment host, compare the backend's
mount destination and named-volume identity before/after approved image replacement
(using selected `docker inspect --format` mount fields only, not full environment
output). Confirm datasource override absence or its persistent destination, runtime
permissions, and securely compare signing configuration via the secret manager
without displaying values. Confirm `donstro_single` preserves these. Record sanitized
container/image IDs, volume identity and observed refresh results. Do not paste
full `docker inspect`, Compose interpolation, environment, secret hashes, startup
logs or HAR files into evidence. Source inspection cannot satisfy this live check.

## Representative-client check — PASS (validator criterion 7 resolved)

Executed the real Sign in UI and My Games against a disposable backend through a
single loopback HTTPS origin. **Chromium 156.0.8078.4, headless desktop**, Playwright
cached revision 1248, real frontend version **1.7.7**, backend **1.9.2-local**.
Production durations: **60000 ms access / 1209600000 ms refresh**; no duration override.
The test-only backend seeds a disposable user with privacy/demo already completed,
uses production authentication/persistence, and mocks only external notifications.
Both JVMs use the same unique temporary `acceptance.db` file and unchanged public
test-only signing configuration. No production account, host, or deployment used.

Final execution: backend PIDs **630289 → 631259**, each readiness checked with a
bounded real HTTP probe and each gracefully stopped with exit 0. The first backend
was fully stopped before original access expiry **1792096580000 ms** and remained
stopped until expiry + one second. Observed interruption: **65041 ms** including
replacement startup. The browser and its original session storage remained open.

Actual observed sequence:

| Stage | Browser network / UI result |
| --- | --- |
| Initial unauthenticated startup | profile 401, refresh 401 (expected; no session yet) |
| One UI credential login | POST `/api/loginjwt` 200; profile/games/invitations 200; My Games renders `No games yet` |
| After access expiry and backend replacement, reload My Games | GET `/api/profile` 401 and GET `/api/games` 401 |
| Automatic existing-client recovery | Two concurrent POST `/api/refresh` requests 200; profile/games/invitations retries 200 |
| Completed named action | `#/games`, rendered `No games yet`, no Sign in button/prompt, total credential logins exactly one |

Chromium received the backend cookie normally: **Secure=true, HttpOnly=true,
SameSite=None, Path=/api/refresh**. Refresh identity remained unchanged. The script
never supplies Cookie/Authorization headers, adds cookies, writes localStorage,
mocks authentication, or calls refresh itself. It only reads expiry and compares
cookie identity in memory; neither token nor cookie values enter evidence. This
is actual browser cookie delivery and automatic recovery, not the Java cookie-header
check. The empty state is backed by a successful fresh protected games response,
not cached markup. No claim of physical-device/mobile or live deployment testing.

### Reproduce the HTTPS browser acceptance

Target-local aids: `docs/iterations/session-acceptance/browser.cjs` and
`classpath.init.gradle`. Prerequisites: Java 21, installed frontend dependencies,
Node, OpenSSL, usable Playwright/Chromium and browser shared libraries. The script
owns a unique temporary directory and local certificate, loopback-only ports,
backend children and Vite; it cleans only these resources. It prevents remote browser
requests, discards child/console logs, and reports only sanitized metadata/statuses.

Commands actually used, from the target root:

```sh
export JAVA_HOME=/tmp/qubigo-003-jdk
export PATH="$JAVA_HOME/bin:$PATH"
(cd qubigo-backend && ./gradlew \
  -I ../docs/iterations/session-acceptance/classpath.init.gradle \
  :qubigo-servlet:acceptanceClasspath \
  -PacceptanceClasspathFile=/tmp/qubigo-003-browser-classpath)
export ACCEPTANCE_CLASSPATH_FILE=/tmp/qubigo-003-browser-classpath
export PLAYWRIGHT_MODULE=/home/rob/.npm/_npx/e41f203b7505f1fb/node_modules/playwright
export LD_LIBRARY_PATH=/tmp/qubigo-002-browser-libs/usr/lib64
node docs/iterations/session-acceptance/browser.cjs
```

Replace local tool/cache paths on another machine. The previously missing libatk
was available in reusable disposable browser libraries; no system installation was
needed. The runner sets `VITE_API_URL` only in the child environment, not frontend
source/env files. It generates a disposable localhost certificate; Playwright trusts
that test certificate via `ignoreHTTPSErrors`, without changing cookie options.
TLS terminates in a real HTTPS proxy. Standard forwarded host/proto headers and
fixture-only Spring forwarded-header support preserve the original same-origin
request; production security/CORS configuration is untouched.

Two setup failures preceded success: an initial invocation from the root lacked
`gradlew`; initial proxy forwarding lost HTTPS origin and POST returned 403. Then
successful login reached the welcome/demo route rather than My Games because the
seed user had not seen the demo. Corrected wrapper directory, proxy forwarding,
and the disposable builder's existing privacy/demo flags, respectively. These
are setup failures, not behavioral red evidence or product authentication defects.
The completed check passed twice; the final run asserts ordered games 401 → automatic
refresh 200 → games 200 and records the final PIDs/timings above.

### Post-change regression validation

Java 21: `./gradlew test` from `qubigo-backend` **passed** (1m28s),
`./gradlew build` **passed** (1s, tests up-to-date). Servlet **723** + CLI **18**
tests, zero failures/errors/skips. `node --check` on the acceptance runner and
`git diff --check` passed. No production or frontend code changed in this follow-up.

## Operator evidence request — blocked (validator criterion 6 unresolved)

No authorized deployment operator confirmation or approved live-host inspection
channel was supplied in this job. Local Docker access, SSH environment variables,
and repository configuration do not establish authorization or facts about the live
service. No remote connections, secret reads, release commands or image replacement
were attempted. The passing disposable browser test cannot resolve this finding.

An authorized operator must return this sanitized record, covering an existing or
separately approved replacement (do not deploy solely to fill this template):

- Confirmation date, operator role and approved environment identifier.
- Before/after container/image identifiers and replacement mechanism; confirmation
  that external `donstro_single` does not remove/recreate the persistent volume.
- Selected mount destination and retained volume/datasource identity; actual
  `QUBIGO_DB_PATH` destination or confirmation that the documented default applies.
- Runtime identity has read/write permissions on that persistent database location.
- Signing secret-manager reference/version retained across replacement, or operator
  attestation of equality from secure comparison; **no secret values or hashes**.
- Confirmation that no `down -v`/volume prune/ephemeral database/key regeneration
  occurs; any conditions on host replacement/backup restoration.
- If available, sanitized post-expiry refresh/action statuses across the approved
  replacement, without tokens, cookies, passwords, raw logs or environment dumps.

Until that record is supplied, live volume/key continuity is **unverified**. Task 5
and its operator subtasks remain open; only the browser finding is marked resolved.
Task 6's final review/acceptance mapping remains separate.

### Current validator retry — still blocked

Re-read the seed, exact external plan and this evidence in response to the finding
at lines 235–260. The current job supplies no operator response to the checklist
above and no approved live-host inspection channel. Therefore no new deployed
continuity evidence can be recorded. Existing source, process and browser results
are unchanged and are not substitutes for operator confirmation.

Recorded this retry's finding as an unchecked subtask of task 5 in
`/home/rob/dev/libby/factory/runs/qubigo-003/plan.md`. Task 5 remains open; the
finding is not resolved. No product code changed, tests rerun, secret inspection,
remote connection or deployment performed for this documentation-only retry.

**Required next input:** an authorized operator's completed sanitized checklist
above, including database/mount identity, runtime permissions, signing-key retention
attestation and external replacement-lifecycle confirmation. Record that actual
response here before marking the operator finding resolved; do not infer answers
from the repository or fabricate an attestation.

### Current doer attempt — task 5a blocked

The first unfinished plan task is task 5; its next unchecked step is 5a.
This attempt received no validator findings (`[]`) and no authorized operator
response. Re-read the seed, exact external plan and existing task-5 evidence;
no completed operator checklist is present in the supplied input or target-local
iteration evidence. `yx list --ready` was retried and is still unavailable.

Per step 5a, no disposable tests were repeated as a substitute, and no remote
inspection, secret access or deployment was attempted. Steps 5a–5c and criterion 6
remain blocked. Existing worktree changes were left intact. No production code
changed and no tests ran for this documentation-only attempt.

**Next action for the authorized operator:** return the completed sanitized
checklist in “Operator evidence request” above, with date, environment, role,
replacement method and concrete before/after continuity observations. Do not send
secret values, hashes, tokens, cookies or raw environment/inspection dumps. Only
then can 5b validate the response and 5c record verified deployed evidence.

### Short operator confirmation — pending

Draft only; not yet confirmed by the operator:

> As deployment operator, I confirm that backend replacement preserves the database and signing key, the backend can read and write the database, and the replacement process does not delete the persistent volume. Verified on [date] in [environment] using [replacement method].

This draft is not evidence. Even with its placeholders filled, this short statement
alone does not satisfy the detailed checklist: it lacks concrete before/after mount
and datasource identity, attributable permission observations and signing/lifecycle
verification. Supply the full sanitized record above; task 5 remains open.

### Latest validator finding — operator evidence absent

The supplied finding at lines 282–308 states that criterion 6 and steps 5a–5c
remain unverified because operator confirmation is still a placeholder draft.
Recorded it as an unchecked subtask of task 5 in the exact external plan at
`/home/rob/dev/libby/factory/runs/qubigo-003/plan.md`.

No completed operator response accompanies this attempt. Consequently there is no
attributable deployed before/after observation to validate or record, and the
finding cannot safely be resolved here. Source configuration and disposable
process/browser checks remain separate evidence, not deployed continuity proof.
Earlier retry sections above are historical; their references to no supplied
findings do not describe this attempt.

**Input required from an authorized deployment operator:** return the existing
sanitized checklist with verification date, operator identity/role, environment,
replacement method and before/after container/image identifiers. Include retained
mount/volume and effective datasource identity (including overrides), runtime
read/write permissions, signing-reference/version retention or secure equality
attestation, and external replacement/volume lifecycle with host/backup limitations.
Do not send secret values/hashes, tokens, cookies or raw environment dumps. Use an
existing or separately approved replacement; no deployment is authorized by this
request.

After receipt, step 5b must check every field for completeness and consistency;
step 5c must record the actual sanitized response and field-by-field validation.
Until then 5a–5c, criterion 6, this finding and task 5 remain open. No product code,
remote inspection, secret access, deployment or substitute acceptance test run was
performed for this documentation-only attempt.

### Validator retry — lines 312–340

The current supplied finding requires actual authorized-operator evidence, not a
further draft confirmation. It is recorded as an unchecked subtask of task 5 in
`/home/rob/dev/libby/factory/runs/qubigo-003/plan.md`. No operator response was
supplied with this job; the earlier draft remains explicitly unconfirmed.

Field-by-field input assessment for steps 5a–5c:

| Required operator evidence | Current result |
| --- | --- |
| Verification date, attributable operator identity/role, approved environment | Missing; no attestation received |
| Before/after container/image IDs and replacement method | Missing; disposable JVM IDs are not deployed IDs |
| Before/after retained mount/volume identity and destination | Missing; Compose source is not a live observation |
| Effective datasource identity, including runtime overrides | Missing; repository default does not establish deployed configuration |
| Runtime identity and database read/write permissions | Missing; no authorized runtime observation supplied |
| Retained signing reference/version or secure equality attestation | Missing; key pass-through configuration does not prove retention |
| External replacement/volume lifecycle, host/backup limitations | Missing; no operator lifecycle confirmation supplied |

These are missing inputs, not failed observations or validated fields. There is
no actual response to sanitize or check for consistency. Criterion 6, this finding,
steps 5a–5c and task 5 remain unresolved. No test rerun can obtain this external
attestation; no remote access, secret inspection or deployment was attempted.

**Requested next action:** an authorized deployment operator must return the full
sanitized checklist in “Operator evidence request” for an existing or separately
approved replacement. Do not include keys, hashes, tokens, cookies or raw dumps.
Then validate each field against that response and record the sanitized evidence;
request clarification for any omissions rather than filling them from source.

### Owner confirmation — dev, 2026-10-15

Supplied by the product owner (rob). Accepted as the live evidence for criterion 6
under the seed's 2026-10-16 relaxation: dev environment, same-container restart,
not a production validation and not a recreated-container observation.

> As deployment operator, I confirm that backend replacement preserves the database and signing key, the backend can read and write the database, and the replacement process does not delete the persistent volume. Verified on 2026-10-15 in dev using restarting qubigo-backend container.

**Task 5a assessment (updated 2026-10-16):** Earlier retry assessments above are
historical and describe the criterion before its relaxation. The seed's criterion 6
now accepts the owner confirmation above as the live evidence for this iteration.

- Confirmed (owner-attributed): date **2026-10-15**, environment **dev**, method
  **restarting qubigo-backend container**; database and signing key preserved,
  database readable and writable, persistent volume not deleted.
- Scope limits, recorded rather than missing: this was a same-container restart in
  dev. It is not a recreated container/image observation and not production
  validation. Before/after container/image IDs, mount/volume identity, datasource
  override, runtime permissions, signing reference/version and `donstro_single`
  lifecycle were not collected; the seed no longer requires them.
- Outstanding for release: confirming volume and key continuity across an image
  replacement on the first production deployment, as stated in criterion 6.

Steps 5a–5c are satisfied by this confirmation together with the source-configuration
check and the disposable process/browser acceptance above. No tests, remote
inspection, secret access or deployment were performed for this update.
`yx list --ready` was retried and remains unavailable (exit 127).
