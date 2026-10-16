# Iteration 003 — Final validation and acceptance matrix

## Review handoff

Task 6 execution is complete for planner review; the top-level task is not marked
done and no commit or deployment was performed. The worktree was clean at entry
(HEAD `2444ff7c`). This task changes documentation only. No validator findings were
supplied. The exact external plan is
`/home/rob/dev/libby/factory/runs/qubigo-003/plan.md`.

All seven seed criteria have supporting evidence under the seed's 2026-10-16
criterion-6 relaxation. **Live production database/key continuity across image
replacement remains outstanding for the first production release**, not verified
here and not an iteration acceptance blocker. The accepted owner observation is
a dev same-container restart, not container recreation or production validation.

## Final regression execution (6a–6b)

From `qubigo-backend`, on the final product source:

```sh
export JAVA_HOME=/tmp/qubigo-003-jdk
export PATH="$JAVA_HOME/bin:$PATH"
java -version
./gradlew test --rerun-tasks
./gradlew build
```

Java: OpenJDK Temurin **21.0.12.1+1-LTS**, 64-bit. Test command exit **0**,
**1m54s**, all **22 actionable tasks executed** (not an up-to-date-only pass).
Build exit **0**, **2s**, 3 executed / 24 up-to-date; its tests reused the immediately
preceding successful forced execution. Local logs:
`/tmp/qubigo-003-final-{test,build}.log` (disposable, not committed).

Counts were read from current JUnit XML in
`qubigo-backend/build/reports/{qubigo-servlet,qubigo-cli}/test-results/test/`.
Do not use stale `build-reports/` artifacts or the default module build paths.

| Executed suite | Tests | Failures | Errors | Skips |
| --- | ---: | ---: | ---: | ---: |
| Servlet full suite | 723 | 0 | 0 | 0 |
| CLI full suite | 18 | 0 | 0 | 0 |
| SqliteBasedRefreshTokenRepositoryTest (included above) | 3 | 0 | 0 | 0 |
| RefreshTokenMigrationTest (included above) | 2 | 0 | 0 | 0 |
| PersistentAuthenticationWiringTest (included above) | 8 | 0 | 0 | 0 |
| PersistentAuthenticationRestartTest (included above) | 9 | 0 | 0 | 0 |
| PersistentAuthenticationProcessTest (included above) | 1 | 0 | 0 | 0 |

No extra focused run was necessary: all five classes have nonzero executed tests
in the forced full run. Compared with task 1's Java 21 baseline (688 servlet + 18
CLI, no failures), there are 35 additional servlet tests and no new failures.
Existing Kotlin warnings, JVM class-sharing warnings and Gradle deprecation
warnings remain nonfatal. The original Java 25 setup incompatibility is avoided
using Java 21; no build configuration was changed for this task.

Final process-test sanitized output: backend PIDs **710838 → 710911 → 710983 →
711054**; three replacements, expired original access **401**, original refresh
**200** after each of the first two replacements, unchanged token/absolute expiry,
both logged-out sessions **401** after the third. Original access expiry
**1792147909000 ms**, refresh expiry **1792148203000 ms**. Each child passed
bounded readiness and clean exit checks. Same unique temporary SQLite file and
public test-only signing configuration across children; no second login repairs
the sessions. This is JVM replacement, not Docker or production validation.

## Acceptance matrix (6d–6e)

All test classes below are under
`qubigo-backend/qubigo-servlet/src/test/kotlin/com/qubigo/qubigobackend/users/adapters/repositories/`.
The reproducible command for rows 1–5 and the disposable part of row 6 is the
forced full-suite command above. For an individual class, from `qubigo-backend` use
`./gradlew :qubigo-servlet:test --tests '*ClassName' --rerun-tasks`, replacing
`ClassName` with the named class. Historical evidence is explicitly distinguished
from this task's final automated run.

| Seed criterion | Concrete evidence and observed result | Environment / limitation |
| --- | --- | --- |
| 1. Refresh after replacement and original access expiry | **Final run:** `PersistentAuthenticationRestartTest.original session refreshes after access expiry across two complete restarts`; `PersistentAuthenticationProcessTest.session survives two process replacements and logout survives another`. Old access 401, refresh 200, new protected games access 200. | Real HTTP, production wiring, unique file-backed SQLite, stable fixture key; separate contexts and separate OS JVMs. Short fixture-only durations, no production deployment. |
| 2. Repeated restarts without expiry extension | **Final run:** same repeated-restart/process methods compare original refresh identity and absolute expiry. Restart test `persisted recognition never extends original refresh expiry or resurrects it` succeeds before original expiry, rejects after it and after another recreation while membership remains persisted. | Restart fixture uses 2-second access, 120-second refresh normally, 20-second focused expiry window; bounded polling with 1-second safety margin, not changed production defaults. |
| 3. User-wide logout persists | **Final run:** restart test `multi-session logout and single deletion survive restart without revoking other users`; wiring test `profile changes reload current user and logout revokes all sessions but not another user`; process test rejects both sessions after fourth JVM starts. Adapter tests cover all five operations and committed deletion. | Stable-ID revocation, current user reload, other-user isolation, explicit single-token deletion, authorization-filter rejection after logout. |
| 4. Invalid/unrecognized tokens remain rejected | **Final run:** wiring negative cases and restart parameterized `negative authentication contracts are unchanged after restart`: expired, malformed, tampered, unknown, deleted-user and missing-cookie. All HTTP 401, no usable access response/new cookie, no added membership before/after recreation. | Tampered JWT is structurally valid with altered signature; unknown JWT is validly signed/future-dated. Malformed parser failure was narrowly corrected from characterized 500 to sanitized 401 in task 3, not a session redesign. |
| 5. Fresh and upgraded DB preserve data | **Final run:** `RefreshTokenMigrationTest.fresh full application uses Flyway schema with Hibernate DDL disabled`; `upgrade preserves existing users game journal and list across two startups`. Both pass. | Real SQLite files, prior Flyway version `20260821112600`, new migration once, retained users/game events/list/participants and unchanged migration history on second startup. |
| 6. Database/key deployment continuity | **Source reinspection this task:** `donstro/docker-compose.yml` named `prod_data` mount, main `application.yml` datasource/key, `qubigo-backend/{Dockerfile,run.sh}` working directory, `release-backend.py` tag update and external `donstro_single up`. **Final disposable process run:** same-file/key JVM replacement passed. **Accepted historical live input:** [owner confirmation — dev, 2026-10-15](003-session-persistence-task-5.md#owner-confirmation--dev-2026-10-15). | Owner rob confirms DB/key retained, read/write access and volume not deleted on **dev same-container restart only**. Accepted by relaxed seed. Source plus disposable test is not live production image-replacement evidence; that remains a first-release prerequisite. No further operator checklist required for this iteration. |
| 7. Representative client continues without credentials | **Historical actual run retained, not repeated in task 6:** [Representative-client check](003-session-persistence-task-5.md#representative-client-check--pass-validator-criterion-7-resolved). Chromium **156.0.8078.4** headless desktop, frontend **1.7.7**, backend **1.9.2-local**. Ordered games 401 → automatic POST refresh 200 → games 200, My Games `No games yet` rendered; exactly one credential login. | Real same-origin loopback HTTPS and browser-delivered Secure/HttpOnly/SameSite=None cookie; no cookie/token injection. Production durations 60000/1209600000 ms, first backend fully stopped before access expiry, resumed after expiry. PIDs 630289 → 631259; interruption 65041 ms. Not mobile or live production. |

For row 7 reproduce using the commands and prerequisites in task 5's
[HTTPS browser instructions](003-session-persistence-task-5.md#reproduce-the-https-browser-acceptance):
Java 21, frontend dependencies, Node/OpenSSL and usable Playwright/Chromium;
generate the disposable test classpath with `classpath.init.gradle`, set
`ACCEPTANCE_CLASSPATH_FILE` and `PLAYWRIGHT_MODULE`, then run
`node docs/iterations/session-acceptance/browser.cjs` from the target root.
Browser tooling and product sources are unchanged since the recorded successful
checks; this documentation-only task does not claim a new browser run. Historical
behavioral memory-wiring mutation results remain in task 4/task 5 evidence; no new
production mutation was needed.

## Final diff and diagnostic review (6c)

Inspected cumulative session changes with `git diff cd0515ce..HEAD`, separating
unrelated changes from the session work, and checked these concrete paths:

- `SecurityConfig.kt`: non-dev production factory selects the transactional SQLite
  adapter behind the unchanged five-operation port. Dev-only `TestUsers.kt` keeps
  the matching memory double for in-memory-user MVC slices; production restart
  tests select the actual bean without a candidate replacement.
- `users/adapters/repositories/{SqliteBasedRefreshTokenRepository,JpaRefreshTokensRepository}.kt`:
  SHA-256 digest recognition, stable user ID, current-user lookup, transactional
  writes/deletes, no serialized user snapshot, no expiry cleanup/shutdown clearing.
  Unknown/deleted-user lookup fails. Adapter owns storage and revocation.
- `V20261015120000__create_refresh_tokens.sql`: additive new table and user index,
  digest primary key; no destructive alteration to existing user/game tables.
- `users/adapters/security/{JwtTokenizer,JwtBasedAuthenticator}.kt`: validity stays
  in tokenizer, issuance/refresh/logout orchestration stays in authenticator.
  Only tokenizer's previously recorded sanitized malformed-input handling changed;
  no signature-only recognition fallback. No new SQL/controller coupling or session
  framework. Authentication controllers, cookies, DTOs, frontend sources and
  production duration configuration are unchanged by session implementation.
- `PersistentAuthentication{Wiring,Restart,Process}Test.kt`,
  `RefreshToken{MigrationTest,DatabaseFixture}.kt`, `SessionAcceptanceProcess.kt`,
  servlet `build.gradle.kts` and `docs/iterations/session-acceptance/{browser.cjs,classpath.init.gradle}`:
  fixtures own unique temporary DBs, explicit teardown and bounded lifecycle/expiry
  checks; only external notifications are mocked. Gradle supplies a test-runtime
  classpath; process helper is not packaged as production application code.
  Assertions compare token equality booleans/expiry, suppress raw response/child
  logs, and redact session rendering. Public fixture signing configuration and
  disposable credentials are not operator secrets.

Scanned final test/build logs and current JUnit XML (247 diagnostic files) for
JWT-shaped bearer values, refresh-cookie JWT assignments and private-key blocks:
**zero matches** for each. Reviewed diagnostic handling in the new fixtures and
browser script; no new raw bearer/key logging found. This bounded inspection is
not a claim of a repository-wide secret audit; unrelated pre-existing Firebase
credential logging is prevented in these fixtures, not fixed by this iteration.

`git diff --check` and `node --check docs/iterations/session-acceptance/browser.cjs`
passed (exit 0). No acceptance scripts changed, so no extra browser rerun was
required. No unresolved implementation defect found in this review.

## Operational prerequisites and outstanding scope

- Retain Compose project/named-volume identity and DB contents. Compose mounts
  `prod_data` at `/opt/qubigo-backend/data`; default datasource resolves to
  `/opt/qubigo-backend/data/qubigo.db` via runtime working directory. Any
  `QUBIGO_DB_PATH` override must also target persistent storage with runtime write
  permissions. Do not delete/prune the volume or use `down -v`.
- Preserve the signing configuration supplied through `JWT_SECRET_KEY` on every
  release. Replacing the database loses recognition; replacing the key invalidates
  JWTs even with retained records. Initial rollout needs one final sign-in; no
  memory-token migration or adoption of unknown signed tokens.
- **Outstanding for first production release:** live production volume/database/key
  continuity across image replacement, including external `donstro_single`
  lifecycle. No production deployment or remote host inspection was performed.
  Host replacement/backup restoration and uninterrupted availability are not proven.
- No iteration acceptance check remains unavailable under the relaxed seed.
  `yx list --ready` remains unavailable (exit 127), so no Yaks state was inferred.
  Native mobile checks were not run; the required representative client is the
  actual HTTPS Chromium run. Historical blocked/checklist language in task 5 is
  explicitly superseded by its opening notice and unchanged final owner confirmation.
- Entry worktree had no unrelated uncommitted changes. Cumulative competition and
  backlog documentation (`docs/iterations/004-competitions.md`, `docs/backlog.md`)
  and factory/machine configuration changes are unrelated, not acceptance evidence,
  and were neither modified nor reverted.

The matrix and task-5 current-status notice were reconciled with criteria 1–7 in
the exact external seed and task 6 in the exact external plan. Production release
prerequisites are not labelled verified or iteration blockers. Factory/planner
retains commit and top-level task completion responsibility.
