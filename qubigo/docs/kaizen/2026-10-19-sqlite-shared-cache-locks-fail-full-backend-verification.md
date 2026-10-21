# Problem: SQLite shared-cache locks and heap exhaustion intermittently fail full backend verification

Date: 2026-10-19

## Context

Run `qubigo-004a` (line `careful-plan`), doer tasks 3c, 3e, 4b and 5b. Plan: `/home/rob/dev/libby/factory/runs/qubigo-004a/plan.md`; friction log: `plan.md.friction.md` in the same directory (lines 24, 39, 54, 55, 62). Backend tests use an in-memory SQLite database shared across connections: `jdbc:sqlite:file::memory:?cache=shared&date_class=TEXT` in `qubigo-backend/qubigo-servlet/src/test/resources/application.yml` and `qubigo-backend/qubigo-cli/src/test/resources/application.yml`.

## Expected standard

A full `./gradlew test` from `qubigo-backend` is the doer's acceptance evidence and gives the same result on a rerun without product changes.

## What happened

- Task 3e: `QubigoBackendCliApplicationTests.contextLoads` failed during migration `V20261015120000__create_refresh_tokens.sql` with `[SQLITE_LOCKED_SHAREDCACHE] Contention with a different database connection that shares the cache (database table is locked: sqlite_master)`.
- Task 4b: the full suite reached 1016 servlet tests, then three `SqliteCompetitionPlayTest` setup/cleanup cases failed with `[SQLITE_LOCKED_SHAREDCACHE] ... (database table is locked)`, while the focused standings tests passed.
- Task 5b: the same `sqlite_master` lock in CLI `contextLoads` stopped verification before the servlet tests ran.
- Tasks 3c and 4b: `java.lang.OutOfMemoryError: Java heap space` in the Gradle test executor (360 tests in at 4b) after repeated application-context failures.
- Each time the doer reran or narrowed the run. The final reports say the suites passed.

## Impact

Repeated reruns of a multi-minute suite, and an early-stopped suite that left verification incomplete. The doer must judge whether a failure is a product defect or infrastructure noise; the validator relies on the doer's reported counts.

## What allowed it to happen

- Several connections to one shared-cache in-memory database make table locks possible whenever two threads touch it at once (scheduler threads, migrations, test setup/cleanup).
- Competition work adds background schedulers (closure, reminders) to the application context. Commit `b982ddf0` (5b, "Record factory task") adds `closure-scheduling-enabled: false` and `reminder-scheduling-enabled: false` to the CLI test `application.yml`. This may be a workaround for the lock, but nothing in the evidence says so.
- No test-level rule serialises access, retries on `SQLITE_LOCKED`, or reports "infrastructure failure" apart from product failure.
- Failing application contexts are not released, so one failure cascades into heap exhaustion.

## Observations

- The failures are intermittent; focused runs pass and the full suite passes after a retry.
- The lock text and the migration name are identical across 3e and 5b, which points at a repeatable race rather than separate causes.
- The heap errors followed "repeated application-context failures", so they may be a consequence rather than an independent fault. Not verified.

## Why this matters

Full-suite evidence is the line's main quality gate. Intermittent failures teach agents to retry until green, and they hide real concurrency defects in the new transactional competition code behind the same error text.

## Open questions

- Which threads hold the competing connections during CLI `contextLoads`: schedulers, Flyway, or a leaked connection from an earlier context?
- Did disabling the schedulers in the CLI test config stop the lock, or only change timing?
- How many distinct Spring contexts does the suite create, and are failed contexts cached or closed?
- Are these locks possible in production code paths, or only against the shared in-memory test database?

## Possible prevention ideas

- Give each context a uniquely named in-memory database, or a temporary file database.
- Fail fast and report a lock as test infrastructure, with the failing thread, instead of cascading to OOM.
- Record the full-suite run count and any retry in the evidence file so reruns are visible to the validator.

## Resolution

Date: 2026-10-20. Status: countermeasure applied; effectiveness not yet demonstrated.

- **Not reproduced.** Six isolated CLI `contextLoads` runs and four full `./gradlew cleanTest test` runs (one before, three after the change) all passed, so the original failure could not be observed on demand. The cause below is supported by the lock text, not by a reproduction.
- **Probable cause.** Every test context pointed at the same process-wide database, `file::memory:?cache=shared`. All Spring contexts cached in one JVM, and every connection of each context's pool, therefore shared one SQLite shared-cache. Shared-cache locks are table-level and surface as `SQLITE_LOCKED_SHAREDCACHE`, which SQLite's busy handler does not retry. A context starting its Flyway migration (write lock on `sqlite_master`) while another context's connections were open or being created, or while test setup/cleanup ran, hits that error. A failed context is not released, which feeds the heap exhaustion.
- **Fix (prevention of cross-context contention).** Each context now gets its own uniquely named in-memory database: `jdbc:sqlite:file:qubigo-${random.uuid}?mode=memory&cache=shared&date_class=TEXT` in the servlet and CLI test `application.yml`. Still in memory, no file on disk (checked). Connections inside one context still share one cache, so the deliberate overlap tests in `SqliteCompetitionPlayTest` keep working.
- **Not addressed.** Contention between connections of the *same* context (pool filler against Flyway, overlapping test threads) is untouched. If the lock recurs with this change, that is the next suspect; consider `hikari.minimum-idle: 1` for tests. Failing contexts are still not released, and the evidence file still does not record retries.
- **Validation.** `./gradlew cleanTest test` ×3 and `./gradlew build` green: 1078 tests, 0 failures. Expected result (no regression, no lock) matched, but the baseline never failed in this session, so this proves no regression rather than prevention.
- **Review point.** Watch the next two or three full-suite runs on a competition-sized branch; reopen this note on any `SQLITE_LOCKED_SHAREDCACHE`.
- **Changed paths:** `qubigo-backend/qubigo-servlet/src/test/resources/application.yml`, `qubigo-backend/qubigo-cli/src/test/resources/application.yml`.
