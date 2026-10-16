# Iteration 003 — Task 3 implementation evidence

Production authentication now uses SQLite refresh-token persistence. This is
wiring/authentication regression evidence, **not restart, deployment or client
acceptance**. Task 3 remains open for the factory planner after commit; no commits
were made by the doer. No validator findings were supplied. `yx list --ready`
was attempted and remains unavailable (`command not found`).

## Changes

Paths below are under `qubigo-backend/qubigo-servlet/src/`:

- `main/kotlin/com/qubigo/qubigobackend/SecurityConfig.kt`: constructs the existing
  transactional `SqliteBasedRefreshTokenRepository` with real JPA records and the
  user repository. The bean follows the existing persisted-user `!dev` profile.
  There is no production memory factory. CLI remains outside security wiring.
- `main/kotlin/com/qubigo/qubigobackend/users/adapters/security/JwtTokenizer.kt`:
  after the existing expiry/signature catches, maps other JWT parsing failures
  and empty/blank input to `InvalidToken("token is invalid")`. Parser exception
  contents are neither returned nor logged. This is the narrow correction
  explicitly planned in task 1 for malformed refresh producing HTTP 500.
- `test/kotlin/com/qubigo/qubigobackend/common/adapters/controllers/TestUsers.kt`:
  the existing dev-only shared in-memory user fixture supplies a matching memory
  refresh-token double for MVC slices and dev-profile component tests. No broad
  overrides or extra production fallback; real persistence tests use non-dev.
- `test/kotlin/com/qubigo/qubigobackend/users/adapters/repositories/RefreshTokenDatabaseFixture.kt`:
  optional production-wiring mode omits the task-2 candidate adapter entirely.
  Existing storage/migration tests retain their isolated candidate configuration.

No session durations, rotation, cookie construction, DTOs, controller/filter
orchestration, signing configuration, schema or cleanup behavior changed.

## New authentication coverage

`PersistentAuthenticationWiringTest` lives alongside the database fixture. Each
of its eight independently runnable scenarios owns a unique temporary SQLite
file, complete non-dev application context and random-port HTTP server. `use`
closes the full context/pool. Only external notification initialization is mocked;
users, refresh storage, authentication manager, tokenizer and HTTP security chain
are real. No enclosing rollback, Spring context cache, or bean replacement.
Assertions report statuses, booleans, counts, attribute names or expiry dates,
not cookies, credentials, tokens or keys.

- `production selects persistent storage and preserves login refresh and cookie contract`:
  exactly one refresh port bean, actual SQLite adapter/current-user repository,
  login persists a record, protected `/api/games` succeeds, refresh returns only
  `authToken`, the original refresh token and unchanged absolute JWT expiry.
  Login/refresh cookies retain Secure, HttpOnly, SameSite=None, `/api/refresh`
  path and 1209600-second max-age; refreshed access authorizes a protected request.
- `profile changes reload current user and logout revokes all sessions but not another user`:
  two distinct recognized sessions for one user, another user's credential login,
  persisted name/flag mutation, current profile lookup, refresh/protected success,
  logout 200/empty body/Location `/`, all first-user sessions rejected, old access
  rejected by the real authorization filter, other user's access/refresh retained.
  The second session is issued with the real tokenizer and explicitly recognized
  through the port using a distinct expiry; multi-login timing/restart belongs to
  task 4, not this scenario.
- Separate `recognized expired refresh`, `malformed refresh`, `tampered signature`,
  `valid signed future token`, `deleted user`, and `missing cookie` scenarios:
  each requires HTTP 401, no access-token response field or new cookie, unchanged
  record count and unchanged expected lookup recognition. Expired and tampered
  inputs have persisted recognition so validation cannot be masked by membership.
  Tampering changes the first signature character of a structurally valid JWT;
  the unknown input is correctly signed/unexpired for a real persisted user.
  Deleted-user rows remain in storage but cannot authenticate or activate access.

Existing `AuthenticationControllerTests` now requires malformed input HTTP 401;
its missing-cookie, DTO/cookie, login, refresh and logout tests remain in place.
`JwtTokenizerCharacterizationTest` now requires sanitized failures for malformed,
empty, blank and unsigned/unsupported JWT input; expiry/signature expectations
are unchanged. Existing authenticator tests still cover user reload/missing users.

## Test-first results

Commands run from `qubigo-backend` under Java 21:

```sh
export JAVA_HOME=/path/to/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :qubigo-servlet:test \
  --tests '*PersistentAuthenticationWiringTest' \
  --tests '*JwtTokenizerCharacterizationTest' \
  --tests '*AuthenticationControllerTests*'
# Final targeted regression also includes:
# --tests '*JwtBasedAuthenticatorTest'
./gradlew test
./gradlew build
```

Actual JDK was the existing disposable `/tmp/qubigo-003-jdk` (21.0.12.1).
An initial invocation from the monorepo root had no Gradle wrapper. The first
compilation also exposed fixture API assumptions (Jackson Kotlin helper not
installed, no QResult `getOrNull`, user save returns Unit). Corrected those test
APIs without production changes; neither setup failure counts as behavioral red.

The corrected preimplementation run executed **34 tests, 6 failures**:

- Production bean selection: expected SQLite adapter, observed memory adapter.
- Profile mutation: lookup returned the stale issuance profile.
- Deleted user: memory active membership remained true.
- Malformed HTTP input: both full-context and existing component tests observed
  HTTP 500 instead of required 401.
- Tokenizer malformed test: parser exception escaped rather than a failure result.

After wiring and parser correction, all behavior regressions passed except one
fixture assertion assuming title-case cookie attributes. Existing CookieCreator
emits lowercase attribute names; changed the test to case-insensitive attribute
sets, without changing production cookie behavior. Final targeted run including
`JwtBasedAuthenticatorTest`: **42 passed**, exit 0 (15s).

Final `./gradlew test`: exit 0 (31s), **713 servlet + 18 CLI tests**, zero failures,
errors or skips. Final `./gradlew build`: exit 0 (2s, tests up-to-date).
`git diff --check` passed. Existing Gradle deprecation/JVM warnings remain, with
no unrelated baseline test failures. Reports are generated under
`qubigo-backend/build/reports/{qubigo-servlet,qubigo-cli}/`; raw captured HTTP or
Spring logs are not acceptance evidence and must not be published with tokens.

## Remaining acceptance work

This task deliberately does not prove authenticated refresh after access expiry
and full recreation, repeated restarts, expiry/non-resurrection after restart,
separate process replacement, actual deployed database/key retention or a real
client continuing without credentials. Those remain tasks 4–6 in the exact
external plan `/home/rob/dev/libby/factory/runs/qubigo-003/plan.md`.
