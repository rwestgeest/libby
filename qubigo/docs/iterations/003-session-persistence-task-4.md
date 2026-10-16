# Iteration 003 — Task 4 restart integration evidence

## Scope and changes

Added `PersistentAuthenticationRestartTest` under
`qubigo-backend/qubigo-servlet/src/test/kotlin/com/qubigo/qubigobackend/users/adapters/repositories/`.
Extended its existing `RefreshTokenDatabaseFixture` with optional test-local
access/refresh duration arguments. No production code or defaults changed.
No validator findings were supplied. `yx list --ready` remains unavailable
(`command not found`). Task 4 itself is left open for the factory planner.

Each scenario owns a JUnit temporary directory and SQLite file. Every application
is a new random-port, non-dev Spring context using the production bean, real user
persistence, HTTP authentication/security chain, explicit stable test-only signing
key, Flyway and no Hibernate schema creation. Only external notifications are
mocked. Kotlin `use` closes the complete context and connection pool before the
next starts, including on assertion failure. No cached contexts, retained
repositories, enclosing transactions or copying of recognition records.
Only session strings/expiry observations are retained between instances; session
`toString` is redacted and assertions compare booleans/statuses/counts, not JWTs.
No raw HTTP responses, cookies or keys are included in this evidence.

## Nine independently runnable cases

- `original session refreshes after access expiry across two complete restarts`:
  credential login once, protected access succeeds, context closes; after each of
  two recreations the original access expiry passes, protected access returns 401,
  original refresh returns 200, unchanged refresh identity/absolute expiry, new
  access authorizes `/api/games`. No second credential login repairs the session.
- `persisted recognition never extends original refresh expiry or resurrects it`:
  login, recreate, refresh before original expiry, retain the record, reject after
  original expiry and again after another recreation. Membership remains true,
  proving expiry rejection is not cleanup or missing recognition.
- `multi-session logout and single deletion survive restart without revoking other users`:
  two distinct credential logins for each of two users, explicitly delete one
  second-user token; recreate and verify deletion, refresh first-user session and
  logout; recreate again, both first-user tokens and explicitly deleted token
  remain rejected, first-user protected access fails, surviving other-user
  refresh/protected access succeeds. JWT second-boundary polling ensures distinct
  login tokens, rather than assuming repeated logins produce different strings.
- Parameterized `negative authentication contracts are unchanged after restart`:
  six isolated cases: recognized expired, malformed, recognized tampered signature,
  validly signed future but unknown, deleted user with retained record, missing
  cookie. Identical inputs before/after full recreation each return 401, no access
  response field or cookie, unchanged record count and expected membership.
  Deleted-user records remain present but user lookup/active membership fail.

## Timing protocol

Test-only access duration is 2000 ms, refresh 120000 ms. The focused expiry case
uses 20000 ms (rather than the initially suggested 10000) to leave headroom for
full application recreation on slower hosts. Production durations are unchanged.
Tests observe absolute signed JWT expiration and poll every 50 ms until expiry
plus a one-second NumericDate safety margin, bounded at 25 seconds. Distinct
session issuance polls the next observed wall-clock second with a 3-second bound.
Pre-expiry refresh explicitly requires at least one second of remaining lifetime;
slow setup fails rather than silently skipping the pre-expiry assertion. There
are no unbounded or arbitrary long sleeps and refresh never renews its expiry.

## Actual execution and defect sensitivity

All commands from `qubigo-backend`, with:

```sh
export JAVA_HOME=/tmp/qubigo-003-jdk
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :qubigo-servlet:test --tests '*PersistentAuthenticationRestartTest'
./gradlew :qubigo-servlet:test --tests '*PersistentAuthenticationRestartTest.original session*'
./gradlew test
./gradlew build
```

Java 21.0.12.1, existing disposable JDK. New tests were written before any
production mutation; this task needed no production implementation change.
Initial targeted run: **9 passed**, 44 seconds. To verify actual regression
sensitivity, temporarily replaced only the production refresh repository factory
with a new memory adapter, then ran the focused original-session test: **1 test,
1 behavioral failure**, at `refreshWorks`' HTTP 200 assertion (line 148), after
complete recreation lost recognition. This was not a compile/setup failure.
Restored the original SQLite factory immediately; it has no final diff. This is
a mutation check of the already-implemented task-3 behavior, not a claim of a
new preimplementation red/green production change.

Final full `./gradlew test`: **722 servlet + 18 CLI tests**, zero failures,
errors or skips, exit 0, 1m 2s. This includes all nine restart cases with restored
production wiring. `./gradlew build`: exit 0, 1s (tests up-to-date).
`git diff --check`: passed. Existing Gradle deprecation/JVM warnings remain;
no unrelated baseline failures. Reports under `qubigo-backend/build/reports/`.

## Boundaries

This demonstrates repeated full-context recreation on the same file/key, not
separate OS process replacement, live deployment continuity or a representative
browser/mobile client. Those task-5 checks and operational prerequisites remain
outstanding; full-suite execution here does not mark tasks 5 or 6 complete.
