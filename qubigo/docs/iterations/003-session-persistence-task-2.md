# Iteration 003 — Task 2 implementation evidence

Implemented the additive Flyway migration and SQLite refresh-token adapter. This
is storage/migration evidence, **not production authentication or deployment/client
acceptance**. `SecurityConfig` still selects memory storage; task 3 owns that change.
No commits were made. No validator findings were supplied. `yx list --ready` was
attempted again and is unavailable (`command not found`).

## Implementation

Under `qubigo-backend/qubigo-servlet/src/main/`:

- `resources/db/migration/V20261015120000__create_refresh_tokens.sql` adds only
  `refresh_tokens(token_digest PRIMARY KEY, user_id)` and a user-ID index.
- `kotlin/com/qubigo/qubigobackend/users/adapters/repositories/JpaRefreshTokensRepository.kt`
  defines the JPA record, bulk user-ID deletion, and current-user exact-email
  membership query.
- The sibling `SqliteBasedRefreshTokenRepository.kt` implements all five existing
  port operations, hashing exact UTF-8 token bytes with SHA-256 (lowercase hex),
  upserting duplicate associations, resolving current users through `UserRepository`
  by stable ID, and mapping missing users to `InvalidRefreshToken`.

Spring-managed adapter transactions commit writes before successful return in the
existing nontransactional authentication call path. Database failures propagate;
there is no shutdown clearing, JWT parsing, expiry cleanup, or serialized user
snapshot. Read-only transactions cover lookup and active membership. Orphan rows
cannot authorize and a replacement account with a different ID does not inherit
sessions. No foreign key changes existing user-deletion behavior. The design from
task 1 is unchanged, including the documented narrow stale-profile correction.

## Test-first evidence

New tests under servlet `src/test/kotlin/com/qubigo/qubigobackend/users/adapters/repositories/`:

- `SqliteBasedRefreshTokenRepositoryTest`: all five operations, unknown and
  exact-byte lookup, duplicate/overwritten associations, idempotent single/user
  deletes, multi-session isolation, all active-membership transitions, current
  profile/email resolution, missing/recreated users, and committed saves and both
  deletion forms across four independently opened/closed complete contexts.
- `RefreshTokenMigrationTest`: fresh full application with Hibernate DDL disabled;
  Flyway history, columns/index and digest-only storage assertions. Upgrade fixture
  is first migrated by Flyway API only to `20260821112600`; a full context pinned
  to that version inserts a non-seeded builder user, game-created journal event,
  game-list item and participants through existing repositories. The token table
  is asserted absent before upgrade. After closing it, two updated full-context
  startups compare all old rows (including IDs, profile flags, event payload,
  revision/timestamps and list/participant values), read through existing user/game
  repositories, check exactly one additional migration with stable history, and
  save/reload a token across recreation.
- `RefreshTokenDatabaseFixture`: unique caller-owned `@TempDir` file URLs, DDL
  `none`, explicit stable test-only signing key, random web port, and `use`-scoped
  complete context/connection-pool closure. No rollback transaction or cached
  Spring context. Only external Firebase notification initialization is mocked;
  user and refresh persistence are real. A local non-autowire-candidate adapter
  bean isolates task 2 from the production memory bean. Logging is suppressed;
  user/credential-bearing comparisons produce redacted boolean diagnostics.

Commands run from `qubigo-backend` with Java 21 (`JAVA_HOME=/tmp/qubigo-003-jdk`,
`PATH="$JAVA_HOME/bin:$PATH"`; use your own installed Java 21 for reproduction):

```sh
./gradlew :qubigo-servlet:test \
  --tests '*SqliteBasedRefreshTokenRepositoryTest' \
  --tests '*RefreshTokenMigrationTest'
./gradlew test
./gradlew build
```

Before implementation the local candidate bean intentionally used the existing
memory adapter. First run found a fixture setup problem: existing application
wiring has two `SqliteBasedGameListView` beans. Resolved by selecting the existing
`gameListView` bean by name; no production wiring change. This setup failure is
not behavioral red evidence.

The corrected preimplementation targeted run compiled and ran 5 tests: 4 failed,
1 passed. Behavioral failures were:

- `committed saves and both forms of deletion survive complete context recreation`:
  saved association was unrecognized in the second context.
- `lookup and revocation use current persisted user identity`: lookup returned the
  issuance-time profile after a persisted update.
- `fresh full application uses Flyway schema with Hibernate DDL disabled`: missing
  new migration history entry (zero instead of one).
- `upgrade preserves existing users game journal and list across two startups`:
  missing additional migration (prior history count unchanged).

Added migration/adapter and changed only the local candidate factory to construct
that adapter. Same targeted command passed all 5 tests (15s). Expanded exact-byte
regressions and redacted comparison diagnostics; final full `./gradlew test`
passed (21s): servlet **705**, CLI **18**, zero failures/errors/skips. Final
`./gradlew build` passed (1s; test results up-to-date). Baseline Gradle deprecation
and JVM class-sharing warnings remain. `git diff --check` is clean.

## Remaining boundaries

Task 2 checklist evidence is complete, but its top-level task remains unfinished
for the factory planner to close after commit. Production bean selection,
authentication regression/malformed-input handling, real authenticated restart
and expiry tests, separate-process checks, representative client and operator
confirmation remain tasks 3–6. These tests do not claim those acceptance results.
