# Iteration 003: characterization and implementation design

Task 1 evidence, 2026-10-15. Production code is unchanged. This is not restart,
client, or production acceptance evidence. The external factory plan remains at
`/home/rob/dev/libby/factory/runs/qubigo-003/plan.md` (not duplicated here).
No validator findings were supplied. `yx list --ready` failed: command not found.

## Sources inspected

Read `qubigo-backend/AGENTS.md`, backend docs `README.md`, `architecture.md`,
`domain-modules.md`, `authentication.md`, `getting-started.md`, all five ADRs and
the ADR index, and the SQLite access issue note. Several documentation links
(`development.md`, `configuration.md`, `testing.md`, `api-reference.md`) have no
corresponding file. Source takes precedence over the example authentication docs:
actual endpoints are `/api/loginjwt`, `/api/refresh`, and GET `/api/logout`;
refresh uses a cookie, does not rotate, and defaults to 14 days, not 24 hours.
The build and Dockerfile target Java 21, despite the Java 17 documentation.

Backend source paths below are relative to
`qubigo-backend/qubigo-servlet/src/main/kotlin/com/qubigo/qubigobackend/`:

- `users/domain/security/RefreshTokenRepository.kt`: all five port operations.
- `users/adapters/security/{InMemoryRefreshTokenRepository,JwtTokenizer,JwtBasedAuthenticator,CookieCreator}.kt`.
- `SecurityConfig.kt`, `JwtAuthorizationFilter.kt`, and
  `users/adapters/controllers/AuthenticationController.kt`.
- `users/domain/{User,UserRepository}.kt`, user JPA records/repository/adapter and
  `users/adapters/wiring/UserRepositoryConfig.kt`.
- `common/domain/QResult.kt` and `common/adapters/controllers/GlobalExceptionHandler.kt`.
- `QubigoBackendKtApplication.kt`, `JpaConfig.kt` (inactive commented configuration),
  game repository/event mappings, existing migrations and migration generation script.
- Existing JWT, authenticator, authentication component, user persistence and
  full-context startup tests; test fixtures and `TestUsers` profile wiring.

### Operation/caller audit

`AuthenticationTokenizer.createRefreshToken` is the only production save caller.
Refresh first validates signature/expiry, then calls `byRefreshToken`, then reloads
by the returned user's email. User reload failures become `InvalidRefreshToken`.
Refresh returns the original refresh token and a newly generated access token;
only cookie max-age resets. JWT expiration does not reset. No production caller
uses single-token `delete`; retain it for the port and tests.
`JwtBasedAuthenticator.logout` calls `deleteForUser` for the current user, doing
nothing when no current user is available. The authorization filter calls
`hasActiveTokensForUser` for protected requests; public paths bypass this membership
requirement. That check is exact-case email membership, **not** refresh JWT expiry.
The user details service still reloads current users for authorization.

## Baseline and commands

All commands below run from `qubigo-backend`. The initial `./gradlew test` and
`./gradlew build` each exited 1 before compilation/tests with `What went wrong:
25.0.4` under the harness's Temurin 25.0.4. This is an environment incompatibility,
not an authentication failure. Downloaded a disposable Temurin 21 JDK to
`/tmp/qubigo-003-jdk` using Adoptium's latest Java 21 Linux x64 JDK endpoint;
actual version was 21.0.12.1+1. No repository build changes or global Java changes.
For reproduction, use an installed Java 21 JDK, not the temporary path's existence.

```sh
export JAVA_HOME=/path/to/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew test
./gradlew build
./gradlew :qubigo-servlet:test \
  --tests '*InMemoryRefreshTokenRepositoryCharacterizationTest' \
  --tests '*JwtTokenizerCharacterizationTest' \
  --tests '*JwtBasedAuthenticatorTest' \
  --tests '*AuthenticationControllerTests*'
```

Before test changes, Java 21 `test` passed (2m26s): servlet 688 tests, CLI 18,
zero failures/errors/skips. `build` passed (4s), reusing those test results.
New characterization initially had five fixture setup failures because an invalid
UUID was used for the second user. Corrected it to `aValidID("456")`. This was
**not** a behavioral red test, nor evidence of a production fix. The targeted
command then passed all 39 tests (9s). Full `test` passed afterward (17s), and
`build` passed (1s, tests up-to-date). Final rerun after adding the explicit
one-second expiry safety margin: `./gradlew test` and `./gradlew build` both
exited 0; servlet 700 + CLI 18 tests, zero failures/errors/skips. `git diff --check`
was clean. Gradle deprecation, existing Kotlin compiler,
and JVM class-sharing warnings are baseline warnings, not test failures.

Reports are generated (gitignored) under
`qubigo-backend/build/reports/{qubigo-servlet,qubigo-cli}/test-results/test/`
and the corresponding `tests/test/` directories. Do not publish raw captured HTTP
or Spring logs: existing tests can include cookies and credentials in diagnostics.
Only aggregate counts/statuses and source/test names are recorded here.

No production implementation increment occurred, so no red/green implementation
claim is made. Future tasks must demonstrate behavioral red tests before code.

## Explicit behavior matrix

New pure tests live under servlet `src/test/kotlin/.../users/adapters/security/`.
HTTP additions extend `component/AuthenticationControllerTests.kt`.

| Case | Current result / evidence | Persistent implementation expectation |
| --- | --- | --- |
| Unknown token lookup | `Failure(InvalidRefreshToken())`; `unknown lookup and repeated deletion are harmless` | Same, never infer recognition from signature |
| Save twice, same token/user | One effective association; `repeated saves overwrite the same token association` | Same; idempotent upsert |
| Same token saved for another user | Replaces association; old user no longer active | Same port behavior (normal issuance never does this) |
| Single deletion, unknown/repeated deletion | Unit return; no failure; remaining tokens keep user active | Same, committed before return |
| Multiple sessions and user-wide deletion | All matching user's tokens gone, another user's retained; `single deletion and user wide revocation preserve other users` | Same, using stable ID |
| No tokens / one token / last removed | Active false / true / false | Same |
| Arbitrary token membership | Save accepts even non-JWT text; lookup and active check succeed; `membership is not JWT validity or expiry` | Same port responsibility; authenticator alone validates JWT |
| Email case | Active check uses exact-case email | Preserve exact-case check against current persisted user's email |
| Profile changed after issue | Memory lookup returns old snapshot; logout with updated user fails to delete it; `existing defect - profile changes prevent whole user equality revocation` | Deliberate narrow correction: lookup current user by ID, revoke by ID regardless of profile/flags/name changes |
| JWT expiration | NumericDate is second-resolution (milliseconds truncated); a strictly elapsed boundary returns `Failure(InvalidToken("token is expired"))`, future token succeeds | Same JJWT rules and absolute expiry; no duration extension |
| Recognized expired refresh HTTP | 401, no successful auth result, record remains; `recognized expired refresh is unauthorized without deleting membership` | Same before/after restart |
| Structurally valid JWT signed by another key | `InvalidToken("token signature is invalid")`; HTTP 401 | Same; later tamper test must alter an existing valid token's signature too |
| Validly signed future JWT with no record | `InvalidRefreshToken`; HTTP 401, no new membership; `validly signed unknown refresh is unauthorized` | Same |
| Deleted user with recognized token | Existing `JwtBasedAuthenticatorTest.refreshing fails when user does not exist anymore` returns `InvalidRefreshToken` | Adapter missing-user resolution must map to same failure; HTTP 401 (controller mapping, to exercise explicitly in task 3) |
| Malformed text | `MalformedJwtException` escapes tokenizer; controller global handler yields **500**; new unit and HTTP defect-characterization tests | Existing defect, not satisfactory rejection evidence. Task 3 must first record/drive a narrow change to 401 for malformed refresh, without logging parser/token contents |
| Missing refresh cookie | HTTP 401; existing `refresh without cookie is UNAUTHORIZED` | Same |
| Refresh success | HTTP 200, body `authToken` only, identical refresh cookie value; newly added boolean identity assertion | Same token, same absolute JWT exp; no rotation |
| Cookie contract | Secure, HttpOnly, SameSite=None by default; path `/api/refresh`, max-age configured duration / 1000; existing controller and CookieCreator tests | Unchanged, including reset cookie max-age rather than resetting JWT validity |
| Logout HTTP | 200 empty body, Location `/`; subsequent protected game request rejected; unauthenticated logout is harmless | Same, plus multiple sessions and changed-profile coverage |

`JwtTokenizerCharacterizationTest` uses generated ephemeral keys and asserts only
failure objects, expiry times and booleans, never raw tokens. Expired tests use a
past boundary with a one-second safety margin, not a racy equality-to-wall-clock
assertion. These tests describe current behavior; do not accidentally treat the
stale-snapshot/500 defect assertions as the desired persistent contract.

## Database and adapter decision (for tasks 2–3)

Use the existing five-operation port, add `SqliteBasedRefreshTokenRepository` in
`users/adapters/repositories`, a JPA `RefreshTokenRecord`, and Spring Data
`JpaRefreshTokensRepository`. Inject `UserRepository` to resolve current users via
`byId`; map any missing-user lookup to `InvalidRefreshToken`, not `UserNotFound`.
Keep JWT parsing out of this adapter. No user snapshot, credentials, JWT text,
subject claim, expiration column, or token logs in the new table.

Add a new timestamp-versioned migration (e.g.
`V20261015120000__create_refresh_tokens.sql`, after latest `20260821112600`):

```sql
CREATE TABLE refresh_tokens (
    token_digest VARCHAR(64) NOT NULL PRIMARY KEY,
    user_id VARCHAR(40) NOT NULL
);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
```

Store lowercase SHA-256 hex of the exact token's UTF-8 bytes (no trimming,
normalization or signature-based fallback). Tokens are high-entropy signed values,
so a one-way digest avoids storing replayable bearer tokens while retaining exact
recognition. Primary key indexes digest lookup; user index serves user-wide
revocation and active checks. `save` upserts association to preserve duplicate
semantics. Do not add a new foreign-key dependency that may block existing user
deletion or rely on SQLite connection-specific foreign-key enforcement. An orphan
row cannot authorize: lookup requires a live user, and active check queries token
membership for a live user's ID with exact-case email. Orphans/expired records can
remain until separately scoped cleanup work; logout still removes all rows by ID.
Recreating an account with a different ID must not inherit old rows.

Use Spring-managed transaction boundaries on adapter operations (write transaction
for each save/delete/deleteForUser; read-only for lookup/active check), with no
long-lived managed user graph or read cache. Spring Data/JPA executes SQL;
controllers/authenticator remain SQL-free. Successful writes must commit before
return, with no enclosing authentication transaction or test rollback hiding this.
Use a single bulk delete by user ID rather than fetch/filter/delete loops. Do not
catch database failures and silently report persistence success.

**No expiry cleanup.** Active membership currently outlives refresh JWT expiry;
a cleanup scheduler would change protected access-token semantics. JWT validation
remains authoritative during refresh. Defaults stay access 60,000ms and refresh
1,209,600,000ms; do not change rotation, signing or API/cookie behavior.

Production datasource URL is `jdbc:sqlite:file:${QUBIGO_DB_PATH:./data/qubigo.db}?date_class=TEXT`.
Boot starts Flyway against its datasource before JPA; production currently has
Hibernate `ddl-auto: update`, tests `none`. Migration tests must explicitly set
`spring.jpa.hibernate.ddl-auto=none` so Hibernate cannot mask missing DDL.
There is no application shutdown token clearing or custom datasource teardown;
close entire Spring context/Hikari pool before reopening the file. Never use the
shared-memory test URL as restart evidence.

## Planned independently runnable tests and fixtures

Use `./gradlew :qubigo-servlet:test --tests '*ClassName*'` for each named class
below, then full `test` and `build`. These classes are planned, not yet implemented:

- `SqliteBasedRefreshTokenRepositoryTest`: all five operations, duplicate overwrite,
  current-user lookup after profile change, absent-user mapping, isolation and
  real committed reopen. First behavioral red: saved token cannot be looked up
  after repository/context recreation with memory storage.
- `RefreshTokenMigrationTest`: separate fresh-file and prior-version upgrade tests.
  Fresh Boot startup with DDL disabled must show Flyway's new table/history entry
  and working storage. Build prior fixture using Flyway API
  `target("20260821112600")` on a new unique temporary file and the existing
  classpath SQL migrations (no hand-built current schema). Use current user/game
  repositories on that prior schema to persist disposable valid builder fixtures:
  a non-seeded user and a created game event plus game-list/participants where
  appropriate. Capture IDs, profile values, event payload/revision/timestamps and
  list values. Close setup context/connections, start updated production context
  on that file, assert data equal and rehydrated user/game accessible, token writes
  work, one new migration entry; repeat startup and assert unchanged history/data.
- `PersistentAuthenticationWiringTest`: full-context non-`dev` profile confirms
  actual production persistent bean, real SQLite user repository, protected
  access, profile mutation then user-wide logout. Existing `dev` tests use
  in-memory `TestUsers`; do not mistake them for persisted-user evidence or
  globally replace production beans to make slices pass.
- Extend `AuthenticationControllerTests`/`JwtBasedAuthenticatorTest` for preserved
  contract and missing-user HTTP outcomes. Narrow malformed handling, if applied,
  first changes the new 500 expectation to 401 and demonstrates the behavioral
  failure; then returns sanitized `InvalidToken` for malformed/unsupported/empty
  JWTs. Do not accept unknown tokens or return/log exception messages.
- `RefreshTokenRestartTest`: programmatically create/close independent servlet
  contexts, explicit file datasource and fixed test-only signing key, no cached
  Spring test context, no outer test transaction. Mock only Firebase/email services
  locally, never users or refresh storage. Unique `@TempDir` per scenario;
  `try/finally` closes every context and connections before deletion. Separate
  repeated refresh, eventual expiry, multi-session logout, single delete, missing
  user and invalid-input scenarios. Confirm the restart recognition test fails
  with memory wiring before enabling production persistence.

Expiry strategy: no production clock changes. Use test-local Boot properties
`qubigo.jwt.authTokenExpirationInMillis=2000` and refresh 120000 for normal restart
scenarios; use refresh 10000 for the focused expiry scenario. Observe signed JWT
absolute exp (second resolution). Bounded polling every 100ms, at most 15 seconds,
after exp + 1000ms, must first assert the original access token receives 401 on
`/api/games`; then real refresh must produce working access. For short refresh
expiry, fail fast if setup/recreation consumes the pre-expiry window (do not claim
it passed). After exp + 1000ms require 401 again after recreation, leaving the row
present. Compare token identity as a boolean and exp as a timestamp. To obtain
distinct sessions, wait for the next JWT expiry-second boundary with a 3-second
bounded poll before second login; assert tokens differ without printing either.
No arbitrary long sleeps, secret-bearing assertion dumps or production-duration
changes. Separate-process/client checks remain task 5 responsibilities.

## Deployment source evidence and operational prerequisites

Inspected `release-backend.py`, `qubigo-backend/{build.sh,Dockerfile,run.sh}`,
`donstro/docker-compose.yml`, root/backend compose references and Terraform
`infra/terraform/qubigo.tf`. No deployment/release scripts were executed.

- Release builds/pushes a versioned image, updates the donstro compose image,
  then invokes external `donstro_single up`. That command is unavailable locally;
  its exact remote replacement/key-loading behavior cannot be established here.
- Donstro backend mounts named volume `prod_data` at `/opt/qubigo-backend/data`.
  Dockerfile sets WORKDIR `/opt/qubigo-backend`, run script changes there, so
  absent an override the database is `/opt/qubigo-backend/data/qubigo.db`.
  Donstro does not declare a `QUBIGO_DB_PATH` override. Root/dev compose bind-mount
  backend data at the same container path; these are not proof of live settings.
- `JWT_SECRET_KEY` is passed through the deployment environment to
  `qubigo.jwt.key`. The image does not generate a key. Source does not prove the
  operator retains the same value across releases. Encrypted secret files were
  not decrypted, and env/secret contents were not copied into evidence.
- A named Docker volume is not an absolute host bind path. Actual host location
  requires the operator to inspect the running mount's volume name and Mountpoint
  safely. Keep the Compose project/volume identity stable; `down -v`, volume prune,
  project-name changes, host replacement or an ephemeral DB override lose sessions.
  No live host path is claimed. Terraform root disk uses `delete_on_termination`,
  and no separately retained database disk is shown there: host replacement needs
  a separate verified backup/restore procedure, beyond container image replacement.
- Confirm the live container's datasource path, persistent volume identity and
  unchanged signing configuration (compare securely without printing values).
  Also confirm volume ownership/write permissions for the non-root runtime user.
  Restart and deployment continuity remain unverified until real task 5 checks.
- Existing Firebase service source logs credential JSON at initialization. This
  pre-existing unrelated risk must not contaminate acceptance evidence: use only
  disposable test configuration, suppress that logger and mock the external
  service in lifecycle integration tests; never collect production startup logs
  as evidence. Credential-logging remediation is separate scope, not performed here.

Initial rollout requires one final sign-in; old memory-only tokens are not migrated.
Operator confirmation, separate-process restart checks and the representative
browser/app check are outstanding. Do not describe this task's passing tests as
satisfying any deployment/client acceptance criterion.
