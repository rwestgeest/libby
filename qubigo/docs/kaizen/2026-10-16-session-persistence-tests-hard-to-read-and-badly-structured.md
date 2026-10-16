# Problem: branch test code is hard to understand and badly structured while production code follows the standard

Date: 2026-10-16

## Context
Branch `feature/003-sessions-survive-backend-deployments` (iteration 003, session persistence). Observed by the user while reviewing the branch: the production code follows the backend structure and ADRs well, but the test code has anomalies. The evidence below comes from a read of the new tests under `qubigo-backend/qubigo-servlet/src/test/kotlin/com/qubigo/qubigobackend/users/adapters/repositories/`, mainly `PersistentAuthenticationProcessTest.kt`, `PersistentAuthenticationRestartTest.kt` and `PersistentAuthenticationWiringTest.kt` (the Wiring test was only skimmed).

## Expected standard
Tests that read as behavior specifications: one behavior per test, intention-revealing names, shared helpers instead of copies, no incidental mechanics in the test body. The standard is written in `qubigo-backend/AGENTS.md`; the user holds the backend structure and ADRs as the reference for production code. This note does not check the tests against that guide line by line.

## What happened
The production code passed review against the structure and ADRs. The tests did not meet the same bar. Concrete patterns seen:

- **One test, many behaviors.** `session survives two process replacements and logout survives another` (Process test) covers two logins, two refreshes, an expiry wait, logout, three process replacements and post-restart revocation. `multi-session logout and single deletion survive restart without revoking other users` (Restart test) covers four sessions and two users. A failure does not say which behavior broke.
- **Opaque control flow.** `lateinit var` sessions assigned inside lambdas and read after them; `repeat(2) { replacement -> ... if (replacement == 1) ... }`; the parameterized `negative authentication contracts are unchanged after restart` branches on a `kind` string in a `when` and again in three `if (kind == ...)` blocks, so each case needs a mental run of the whole body.
- **Unreadable calls.** `rejected(context, session.refresh, true)` and `rejected(context, first.refresh, false)`: a bare boolean `recognized` flag with no name at the call site. `rejected` also asserts the token-store state before and after, so its name hides most of what it checks.
- **Timing in the tests.** `Thread.sleep` polling of up to 25 s (`afterExpiry`), a `System.currentTimeMillis() / 1000 * 1000 + 1000` wait to cross a JWT second boundary (the code comments that issuance has no nonce), and a 60 s process-readiness wait.
- **Duplicated helpers.** `Session`, `login`, `session`, `request` and a wait helper are redefined in each of the Process, Restart and Wiring tests, with small differences (port vs. context, `data class` vs. `class`, `await` vs. `awaitUntil`).
- **Assertion messages and comments carry the intent that names do not**, for example `"bounded expiry wait"`, `"distinct credential sessions"`, `"HTTP security chain ready"`, and a doc comment reading `No raw HTTP diagnostics.` or `No Spring context cache, rollback transaction, replacement repository or retained datasource.`
- **Evidence printed instead of asserted.** The Process test ends with a `println("Process acceptance passed: pids=...; replacements=3; old-access=401; ...")` summary.
- **Test code reaches into implementation.** Restart tests call `JpaRefreshTokensRepository.count()` and fetch beans from the context to assert on stored rows.
- **Placement.** Process, restart and wiring acceptance tests live in the `users/adapters/repositories` package, although they exercise HTTP login, refresh and logout.

## Impact
- Slows review: the user found the tests hard to understand while the production code was easy to follow.
- Quality risk: long tests with many assertions and wall-clock waits are likely to be slow, flaky or hard to diagnose when they fail. This is a hypothesis; flakiness was not measured.
- Maintenance cost: three copies of the HTTP and session helpers will drift.

## What allowed it to happen
Hypotheses, not yet investigated:

- The plan and machine prompts describe the production structure and ADRs, but nothing comparable for test design, so the doer had no standard to follow for tests.
- The validator checks that tests exist, pass and show red evidence (see `2026-10-16-tests-after-implementation-split-by-task.md`), not whether they are readable or well structured.
- Acceptance criteria asked for specific evidence (restart, expiry, process replacement, no diagnostics, redacted output). The tests appear to be shaped around satisfying that evidence list in one place, which would explain the long scenario tests and the leftover `println`.
- Tasks 4 and 5 were written after the implementation (same note), which may have produced tests that confirm behavior rather than specify it.

## Observations
- The defect is specific to the test code: production and test code on the same branch, written by the same line, differ in quality.
- The tests do pass and demonstrate sensitivity to the original defect (per the related note), so the issue is design and legibility, not correctness.
- No test-quality check appears to run anywhere in the line.

## Why this matters
Every later iteration builds on these tests. If the line reliably produces production code to standard but test code below it, review effort moves to the tests and the gap will recur. Good-looking tests can also hide poor coverage.

## Open questions
- Which of the patterns above does the user consider the anomalies? The list is from a quick read and may miss or over-weight some.
- Do `qubigo-backend/AGENTS.md` or the ADRs say anything about test structure, and was it part of the doer's prompt?
- Were any of the anomalies required by an acceptance criterion in the plan (for example the `println` evidence, the 25 s waits)?
- Are the tests flaky or slow in practice? What is the suite runtime added by these tests?

## Possible prevention ideas
- Give the planner, doer and validator the same test-design standard that exists for production code.
- Add a validator check for test readability: one behavior per test, no boolean flag arguments, shared helpers, no `println`, no unbounded sleeps.

## Investigation

Date: 2026-10-16. Read-only investigation: no test, prompt, line or product file was changed and no test was run. Everything below comes from reading the tests, the machine prompts in `.assembly-lines/.machines/`, the lens, `qubigo-backend/AGENTS.md`, and `docs/iterations/003-*`.

### Current and target condition

- **Current:** production code follows the standard; tests for the same feature are long scenario scripts with duplicated helpers, boolean flags, wall-clock waits and a `println` (see the observation above). All four tests in the Restart class and the one Process test were accepted without any validator finding (tasks 2, 3 and 4 each say "No validator findings were supplied").
- **Target:** a reviewer can read each acceptance test as one behavior, the shared mechanics live in one place, and no test waits in real time without need. The user's own measure: the tests read as well as the production code.

### Evidence and causal chain

**Why are the tests hard to read?** Because they implement whole scenarios, with mechanics inline, rather than one behavior each. This is visible in the test files. It traces to three conditions.

1. **The plan prescribed the test shape, and the doer followed it literally (fact).** `003-session-persistence-investigation.md` lines 168–230 ("Planned independently runnable tests and fixtures") already specify:
   - the scenario list, including `multi-session logout and single deletion`, which became one test with four sessions;
   - the wait protocol: "wait for the next JWT expiry-second boundary with a 3-second bounded poll before second login", "bounded polling every 100ms, at most 15 seconds" (the doer widened this to 25 s);
   - "Compare token identity as a boolean ... assert tokens differ without printing either";
   - "No ... secret-bearing assertion dumps".
   The task docs (`...-task-4.md`, `...-task-5.md`) then read as a checklist of those prescriptions. So the planner decided test mechanics, not only the behavior to cover, and the three plan reviewers approved it unanimously. Their lens (`plan_reviewer_*/AGENTS.md`) asks for "meaningful tests" but is defined through responsibility-driven object design, which says nothing about test structure.

2. **A secrecy rule was applied through test code, not through a shared helper (fact + inference).** The seed forbids exposing token values in logs or evidence (`003-sessions-survive-backend-deployments.md` line 38). Tests that used `assertEquals` on JWTs would print them on failure. The result is `assertTrue(a == b, "...")` and `assertFalse(x.refresh == y.refresh)` (21 such assertions in the test tree), a redacted `Session.toString()` re-declared in each class, and a header comment "No raw HTTP diagnostics". The protection is real, but every test re-implements it by hand and loses the failure message. A single `assertSameToken` / `Session` helper would give both safety and readable tests. That a shared helper was not made is inferred from the duplicated code, not stated anywhere.

3. **Nothing in the line judges test code as code (fact).**
   - `rdd_validator` (used by `careful-plan.dot`) reviews "object design" with the RDD lens, which is about production collaborators.
   - The older `validator` (used by `careful.dot`) has the lens "testability", which is one word, and is not in the careful-plan line.
   - The doer prompt (`.machines/doer/AGENTS.md`) has no pointer to `qubigo-backend/AGENTS.md`'s "Testing Conventions" and none to a test-quality standard.
   - `qubigo-backend/AGENTS.md` "Testing Conventions (TDD)" covers four layers (domain, DTO conversion, controller, persistence) and ADR 0004 on test beans. It has nothing on integration or process-level acceptance tests, helpers, waits or boolean arguments. I could not confirm whether `pi` loads the repo `AGENTS.md` into the doer's context; the doer config is only `{"harness": "pi"}`.
   - The existing test style (`AuthenticationControllerTests`: `@Nested`, `@BeforeEach`, Hamcrest `assertThat`) was not carried over. The new tests use a different style (JUnit `Assertions`, `java.net.http.HttpClient`, local helpers). I read only the top of the older file, so I make no wider claim about its quality.

**Why do the tests wait in real time?** (Fact from the code, plus an unverified alternative.)
- `JwtTokenizer.jwtTokenFor(user, expireAfterMillis)` reads `System.currentTimeMillis()` directly and tokens have no `jti`/nonce, so two logins in the same second produce identical tokens. Hence the "cross the second boundary" waits (3 s each) and the 2 s access expiry waits.
- The plan said "no production clock changes". That fixed the wait-based approach.
- **Challenge to the assumption:** the public overload `jwtTokenFor(user: User, date: Date)` and the exposed `JwtTokenizer.key` already let a test mint an access token that has already expired, and the Restart test already uses `jwtTokenFor(user, -60000L)` for its "expired" case. If the "original session refreshes after access expiry" behavior can be shown with an already-expired access token and a real, recognized refresh token, the 25 s `afterExpiry` waits are unnecessary. I have not tried this; whether it keeps the original purpose (the same refresh token surviving real restarts) needs a check. The refresh-expiry test (`persisted recognition never extends ...`) legitimately needs a short real expiry only if it must prove "refresh before expiry succeeds, after expiry fails" for one token; an expired-at-issue token plus a long-lived one may cover it.

**Why did the defect escape?** Three plan reviewers approved the prescribed shape; the validator lens did not cover test quality; and tasks 2, 3 and 4 passed validation with no findings. Task 5's validator loops were about missing operator evidence (criterion 6), not test code, so they did not draw attention to it either. The first human to look at the tests was the user, after the run.

**Why the single `println`?** Task 5 acceptance required evidence of the process run to be printed or recorded without raw responses (the iteration doc says only aggregate counts and statuses go in evidence). The test prints that summary so the run produces its own evidence. The cause is that evidence generation was put in the test, not in the build output or the iteration doc. This is an inference from the text of the plan, not confirmed.

### Unknowns

- Whether `pi` gives the doer the repo `AGENTS.md`, which decides whether the Testing Conventions were available to it at all.
- Whether the tests are flaky. The only timing evidence is task 4's 44 s for nine restart cases (`003-session-persistence-task-4.md`).
- Whether the user regards the long scenario tests, the duplicated helpers, or the sleeps as the main problem. This decides which option below is worth doing first.

### Options

1. **Do nothing / accept.** The tests pass and show sensitivity to the original defect. Cost: the readability gap repeats in the next iteration, and review time moves to the tests.
2. **Clean up these tests only.** Extract a shared test helper for session, login, request and the redacted token comparison; split the long scenarios; name the boolean argument. Fixes this branch, not the cause; the next run reproduces the pattern.
3. **Add a test-design standard and put it in front of the line.** Write test conventions for integration and acceptance tests into `qubigo-backend/AGENTS.md` (one behavior per test, shared helpers, no boolean flag arguments, secret-safe assertions via helper, no real-time waits when a token can be minted, no `println`), and point the planner, doer and a validator lens at it. Cost: prompt and doc changes; benefit: removes the missing standard.
4. **Stop the planner from prescribing test mechanics.** Plans state behaviors and acceptance evidence; the doer chooses the test design under the standard from option 3. Cost: a change to `planner`/`plan_revision` wording and some loss of determinism. Benefit: removes the path by which a plan's wait protocol and scenario list became the tests.
5. **Remove the real-time waits** by adding a `Clock` to `JwtTokenizer` or minting expired tokens in tests. Cost: a production change (the plan ruled it out) or a test redesign; benefit: faster, less flaky tests. This is a product/design decision, not a machinery fix, so it is out of scope here beyond the unverified mint-expired-token idea.

### Recommendation

Option 3 as the countermeasure, with option 2 applied to this branch as the immediate correction. Option 3 attacks the cause that is best supported by the evidence (no test standard and no check on it), and is small: a short section in `qubigo-backend/AGENTS.md`, a line in the doer prompt, and a "test quality" criterion for the validator. Option 4 is a good second step if, after option 3, plans still dictate test mechanics; I would not start there because it changes the planner for a single observed instance. Option 5 should be a separate product decision.

Prediction: if the line gives the doer and the validator an explicit test-design standard for integration/acceptance tests, then the next iteration's new tests will have one behavior per test, a single shared helper set, and no hand-rolled secrecy workarounds, because the standard names those cases and a validator can check them.

### Validation plan

- **Baseline (this branch):** count of `Thread.sleep`/poll helpers (3 in the Persistent* tests), copies of `Session`/`login`/`request` (3 each), boolean-flag helper calls, `println` in tests (1), and longest test body; plus the Restart class runtime (44 s per task 4).
- **Check:** after the next iteration, re-run the same counts on its new tests, and have the validator report test-quality findings in a run that deliberately includes a long, duplicated test (a seeded check that the new criterion fires). Watch for added retry loops from over-strict findings.
- **Review point:** none set; this note carries no agreed experiment.

### Decisions needed

1. Which patterns in the note are the anomalies you meant (the open question from the note)? The recommendation assumes duplication, scenario-length tests, flag arguments, waits and `println` all count.
2. Is option 2 (cleaning up these tests) wanted on this branch, or only the line change (option 3)?
3. Is it acceptable that the planner keeps writing test mechanics for now (option 4 deferred)?

Nothing here has been applied; `kaizen-fix` has not run.

### Additional input: 2026-10-16 (user)

Scope narrowed by the user to four files in `qubigo-backend/qubigo-servlet/src/test/kotlin/com/qubigo/qubigobackend/users/adapters/repositories/`: `PersistentAuthenticationProcessTest.kt`, `PersistentAuthenticationRestartTest.kt`, `PersistentAuthenticationWiringTest.kt` and the process entry point `SessionAcceptanceProcess.kt`. The user's problem statement: the tests are long, scenario-like, lack focus and are hard to read; the user would not understand them on meeting them later; there are no guidelines for tests and there should be.

This answers decision 1 (which anomalies). The other test files on the branch (`SqliteBasedRefreshTokenRepositoryTest`, `RefreshTokenMigrationTest`, the characterization tests, `RefreshTokenDatabaseFixture`) are not in scope. It also confirms the recommended direction: add test guidelines (option 3). Decisions 2 (clean up these tests on this branch) and 3 (planner prescribing test mechanics) are still open.

Inspiration named by the user: the QWAN "TDD Heuristics" series (qwan.eu). Read for this note:

- [One Assert Per Test](https://www.qwan.eu/2021/08/27/tdd-one-assert-per-test.html): one reason to fail, i.e. one *conceptual* assert; "And" in a test name hints at two tests; split a test into one Given-When-Then per test; builders and extracted helper methods keep each test's setup short; a test that seems to need many asserts may point at code doing too much.
- [Test name describes action and expected result](https://www.qwan.eu/2021/07/27/tdd-naming-tests.html): name the action or condition first and the result second ("fails when ...", "succeeds when ..."); long precise names beat short vague ones; if the name will not come, the intent is not yet understood; rename as understanding improves.
- [Given-When-Then or Arrange-Act-Assert](https://www.qwan.eu/2021/09/02/tdd-given-when-then.html): a focused test with one concern, framed as behaviour rather than internal state, the unit as a black box; no `given/when/then` comments, use setup blocks or test data builders instead. The post does not say what to do with several When steps in one test.
- [Glanceable tests](https://www.qwan.eu/2021/09/27/tdd-glanceable-tests.html): intention-revealing names for tests and helpers; extract a helper when its call site reads better, even at the cost of more parts; one level of abstraction per test; hide setup but keep expected data visible; avoid helpers that hide the relevant detail.

Mapping to this branch (observation, not a design): these four rules each name something the four files do not do. One test covers several When steps and many Thens; the long names carry "and"; helpers such as `rejected(ctx, token, true)` hide which outcome is expected; setup, HTTP and process mechanics sit at the same level as the behavior. The heuristics are silent on real-time waits, token-secrecy assertions and process-level tests, so guidance on those would have to be written by us. The guideline text itself is a `kaizen-fix` deliverable and is not drafted here.

### Additional input: 2026-10-16 (user, second message): are these tests needed at all?

The user questions whether the Process, Restart and Wiring tests are needed. Their reasoning: the risk they address is very low. What iteration 003 wanted was `SqliteBasedRefreshTokenRepository` existing and wired. Tests for the repository itself are necessary and stay. The risk of not wiring it is low, and the project does not test wiring elsewhere; the user is open to one wiring test only if it is simple and readable. All other tests the user would drop.

This challenges the assumption behind the note's options (clean up vs. keep) with a third one, delete, and it fits the investigation's "do we need the artifact at all?" question. It is the user's risk call; the facts below only inform it.

Facts from the code (read, not run):

- The wiring is one production bean: `SecurityConfig.refreshTokenRepository(records, users)`, annotated `@Profile("!dev")`, returning `SqliteBasedRefreshTokenRepository`. It replaced `InMemoryRefreshTokenRepository()`.
- Whatever else is deleted, these files depend on each other: `RefreshTokenDatabaseFixture` is also used by `SqliteBasedRefreshTokenRepositoryTest` and `RefreshTokenMigrationTest`, so it cannot go with the rest. `SessionAcceptanceProcess` is used by `PersistentAuthenticationProcessTest` and by `docs/iterations/session-acceptance/browser.cjs` (the browser acceptance evidence for criterion 7). The `qubigo.acceptance.classpath` system property in `qubigo-servlet/build.gradle.kts` exists only for the Process test.
- If the wiring is wrong, nothing fails: the app would still start and log in, and sessions would be lost on restart. That is the defect the iteration fixed. The likelihood is low; the failure would be silent.
- A minimal wiring test needs one assertion. The first assertion of the existing Wiring test already does it: the `RefreshTokenRepository` bean in a non-`dev` context has target class `SqliteBasedRefreshTokenRepository` (`AopUtils.getTargetClass`).
- `AuthenticationControllerTests` gained four tests for refresh rejection (`validly signed unknown`, `recognized expired`, `signed by another key`, `malformed`) alongside a `JwtTokenizer` change that turns malformed tokens into a 401 instead of a 500. That is a production behavior change, separate from the wiring question. `RefreshTokenMigrationTest` and the two characterization tests were not discussed by the user.

Effect on this note: if the three scenario tests and the process harness are deleted, most of the readability findings above no longer apply to this branch. The missing test guidelines (option 3) remain a problem for later iterations, and the note's problem statement stays as observed.

## Resolution

Date: 2026-10-16. Status: countermeasure applied; effectiveness not yet observed.

### Root cause (as supported by the investigation)

No standard for test design existed for integration and acceptance tests, nothing in the line checked test code, and the approved plan prescribed test mechanics (scenario list, wait protocol, boolean token comparison). Separately, the user judged that the risk those tests covered (production not using the SQLite adapter) was too low to justify scenario tests of this size.

### Decisions (user)

1. Delete the Process, Restart and Wiring scenario tests, `SessionAcceptanceProcess` and the acceptance classpath property. Keep the repository, migration and characterization tests, `RefreshTokenDatabaseFixture` and the `AuthenticationControllerTests` additions.
2. Add one simple, readable wiring test.
3. Add test guidelines (QWAN TDD heuristics as inspiration) and point the machines at them.

The planner-prescribes-test-mechanics option (option 4) stays deferred.

### Changes

- `test(users)` commit: deleted `PersistentAuthenticationProcessTest.kt`, `PersistentAuthenticationRestartTest.kt`, `PersistentAuthenticationWiringTest.kt` and `SessionAcceptanceProcess.kt` (all under `qubigo-backend/qubigo-servlet/src/test/kotlin/com/qubigo/qubigobackend/users/adapters/repositories/`); removed the `qubigo.acceptance.classpath` property from `qubigo-servlet/build.gradle.kts` (the file is back to its `main` content); removed the now-unused `durations` and `forwardedHeaders` parameters from `RefreshTokenDatabaseFixture.open`; added `RefreshTokenWiringTest.kt` with one test, `stores refresh tokens in sqlite when the dev profile is not active`.
- `docs(agents)` commit: new "Test design guidelines" section in `qubigo-backend/AGENTS.md` (value-for-cost, one behavior and one conceptual assert, naming, Given-When-Then without comments, glanceable helpers, no boolean flag parameters, shared helpers, no sleeps or polling, secret-safe comparison helper, public-interface assertions, no `println`); one sentence added to `.assembly-lines/.machines/doer/AGENTS.md` and two to the `validator` and `rdd_validator` prompts, pointing at the guidelines and asking the validators to report violations.

### Validation

- Focused run of `RefreshTokenWiringTest`, `SqliteBasedRefreshTokenRepositoryTest` and `RefreshTokenMigrationTest`: passed.
- Mutation check on the wiring test: temporarily returning `InMemoryRefreshTokenRepository()` from `SecurityConfig.refreshTokenRepository` made the test fail with an `AssertionFailedError` at the assertion (not a compile error); the change was reverted and `SecurityConfig.kt` has no diff against the branch.
- `./gradlew test` and `./gradlew build` from `qubigo-backend` with the disposable JDK 21: both `BUILD SUCCESSFUL`; the reports under `qubigo-backend/build-reports/` show 741 tests, 0 failures, 0 errors, 0 skipped. `git diff --check` was clean.
- Expected versus observed: the wiring risk is still guarded by one failing-on-regression test; the restart and process scenarios are no longer exercised in the suite, as the user chose.

### Remaining follow-up

- **Effectiveness:** not shown. Check the next iteration's new tests against the guidelines (baseline from the investigation: 3 copies of the HTTP helpers, 3 sleep helpers, 1 `println`, boolean-flag helper calls). The validator prompts now ask for test findings; whether they fire in practice is unverified, and I do not know whether `pi` loads the repo `AGENTS.md` into the doer, so the doer sentence in its prompt carries the pointer. No review date set; owner: the user.
- **Not done:** `SqliteBasedRefreshTokenRepositoryTest` (kept by decision) has a test, `all five operations preserve isolation and duplicate save semantics`, that does not meet the new guidelines (many behaviors in one body). It can be split in a later change.
- **Stale references (update, same day: `docs/iterations/session-acceptance/` with `browser.cjs` and `classpath.init.gradle` was removed at the user's request; the bullet below is the state before removal):** `docs/iterations/session-acceptance/browser.cjs` still starts the deleted `SessionAcceptanceProcess`, so the browser acceptance script no longer runs as written. The iteration docs `003-session-persistence-task-4.md`, `-task-5.md` and `-task-6.md` describe the deleted tests as evidence and were left as the historical record. `RefreshTokenDatabaseFixture` still carries a comment ("task 3 owns production wiring") that is now out of date.
- **Deferred:** option 4 (the planner prescribing test mechanics) and option 5 (real-time waits / a clock in `JwtTokenizer`) are untouched; the sleeps went away with the tests.

### Additional observation: 2026-10-16 (repository, migration and wiring tests)

After the first fix the user reviewed what was left in `users/adapters/repositories/` and found the same problem there:

- `SqliteBasedRefreshTokenRepositoryTest` still had tests with many asserts (for example `all five operations preserve isolation and duplicate save semantics`, and a restart test that opened four application contexts). It launched whole applications through `RefreshTokenDatabaseFixture` and did not follow the structure of the sibling `SqliteBased*Test` classes. Those are `@DataJpaTest` classes with injected JPA repositories, `@Nested` groups and small tests (see `SqliteBasedGameRepositoryTest`, `SqliteBasedUserRepositoryTest`).
- `RefreshTokenMigrationTest` was unfocused: its second test saved a user, a game and a list item against a prior schema and restarted twice, although the migration only adds the `refresh_tokens` table. The user said it obscured the goal of the tests in the package.
- `RefreshTokenWiringTest` (added in the first fix) sat in `users/adapters/repositories/`, which is not where wiring lives. The production bean is in `SecurityConfig`. The user would put such a test in `adapters/wiring`, or leave it out and rely on a whole-application component test.

This belongs to the same problem (no test guidelines, no check on test code), so it is recorded here and not in a new note.

Decision (user): delete the migration test and the wiring test and accept the gap that a wrong bean choice would then fail no test; restructure `SqliteBasedRefreshTokenRepositoryTest` like its siblings.

### Resolution addendum: 2026-10-16

- **Changed paths** (all under `qubigo-backend/qubigo-servlet/src/test/kotlin/com/qubigo/qubigobackend/users/adapters/repositories/`): rewrote `SqliteBasedRefreshTokenRepositoryTest.kt` as a `@DataJpaTest` with nested Given-When-Then groups (`WhenATokenWasSaved`, `WhenTheSameTokenIsSavedForAnotherUser`, `WhenAllTokensOfTheUserAreDeleted`), 17 small test methods (one parameterized over 3 inputs) in place of 3 long ones; deleted `RefreshTokenMigrationTest.kt`, `RefreshTokenWiringTest.kt` and `RefreshTokenDatabaseFixture.kt` (no remaining users).
- **Dropped coverage, by decision:** persistence across complete context recreation, the upgrade-with-existing-data check, and the wiring assertion. A fresh Flyway-built schema is still exercised by every repository test, because `@DataJpaTest` runs against the migrated database.
- **Finding while rewriting:** `deleteForUserId` is a bulk `@Modifying` query that does not clear the persistence context, so inside one transaction a following lookup returns the stale record. The old tests never saw this because each operation ran in its own transaction. Production does the same (each request has its own transaction), so no product defect is claimed; the test clears the context with a named helper. If a command ever deletes and reads in one transaction, `clearAutomatically = true` on that query would matter.
- **Validation:** all of `SqliteBasedRefreshTokenRepositoryTest` passes; a mutation making `deleteForUser` a no-op fails `forgets every token of the user` (reverted, no diff on the adapter). `./gradlew test` and `./gradlew build` succeeded; the servlet reports written in that run show 719 tests, 0 failures, 0 errors, 0 skipped (CLI tests up to date). The 741 given in the earlier resolution came from counting every report file under `build-reports/`, which can include stale files from deleted tests, so treat it as unreliable.
- **Follow-up:** effectiveness against the next iteration is still unobserved (see the first resolution). `InMemoryRefreshTokenRepositoryCharacterizationTest` and `JwtTokenizerCharacterizationTest` were not reviewed against the guidelines.

### Additional observation: 2026-10-16 (characterization tests)

Review of the two remaining `*CharacterizationTest` files against the test design guidelines, at the user's request:

- `JwtTokenizerCharacterizationTest` (3 tests): one test mixed second-resolution expiry with an elapsed-boundary rejection (4 asserts, "and" in the name, duplicating `JwtTokenizerTest`'s expired-token test); one looped over four malformed inputs so a failure hid the others; the third was fine in shape. The name described a stage of the work, not the unit, and `JwtTokenizerTest` already holds the unit's tests.
- `InMemoryRefreshTokenRepositoryCharacterizationTest` (5 tests): tests with 4, 2 and 2 behaviors and up to 9 asserts; one test asserted a known defect; the header called itself a baseline "for the future persistent adapter", which now exists. `InMemoryRefreshTokenRepository` is used only by tests (`TestUsers` for the `dev` profile and `JwtBasedAuthenticatorTest`); production uses the SQLite adapter, covered by `SqliteBasedRefreshTokenRepositoryTest`.

Decision (user): delete the in-memory test outright; fold the tokenizer cases into `JwtTokenizerTest` without parameterized tests.

### Resolution addendum 2: 2026-10-16

- **Changed paths** (under `qubigo-backend/qubigo-servlet/src/test/kotlin/com/qubigo/qubigobackend/users/adapters/security/`): deleted `InMemoryRefreshTokenRepositoryCharacterizationTest.kt` and `JwtTokenizerCharacterizationTest.kt`; added four tests to `JwtTokenizerTest.kt`: `username is unavailable when the text is not a token`, `... when the token is empty`, `... when the token is unsigned`, `... when the token was signed with another key`. Dropped by decision: the second-resolution and elapsed-boundary tests, and the in-memory adapter tests.
- **Validation:** `JwtTokenizerTest` has 7 tests, all passing. Mutation: replacing the `JwtException` catch in `JwtTokenizer` with an unrelated exception type fails the not-a-token and unsigned tests; the empty-token test stays green because the `IllegalArgumentException` catch covers it. The change was reverted (no diff on `JwtTokenizer.kt`). `./gradlew test` and `./gradlew build` succeeded; the servlet reports from that run show 715 tests, 0 failures, 0 errors, 0 skipped (CLI tests up to date).
- **Left alone:** `JwtBasedAuthenticatorTest` and `TestUsers` still use the in-memory repository, so the class stays in production source for them. Whether it belongs in test fixtures is a separate question. The rest of the branch's tests (for example `AuthenticationControllerTests`) were not reviewed against the guidelines.

### Resolution addendum 3: 2026-10-16 (test types recorded as an ADR)

The user decided the test approach should be written down. Added `qubigo-backend/docs/adr/0006-test-types-and-what-each-is-for.md` (indexed in `docs/adr/README.md`): the five test types and when each is used, the rule that `ComponentIntegrationTest` means a full application context only (the user's wording: used where the behavior could not be captured in a unit or adapter integration test), the value-for-cost rule, the accepted gaps (profile wiring, restart persistence, upgrade with data, process and browser acceptance), and the status of the in-memory repositories as convenience doubles.

Also changed: `qubigo-backend/AGENTS.md` "Testing Conventions" item 4 (it said persistence had no dedicated test, which was no longer true) now describes adapter integration tests, with a new item 5 and a link to the ADR; the "Test Categories" part of `qubigo-backend/docs/architecture.md` now points to the ADR. Docs only; no build or test run.

Found while writing it: `CurrentUserRepositoryTest` and `SpringBasedPasswordEncoderTest` boot the full application context (`@SpringBootTest`) without the tag. Under the ADR's rule they should be tagged or moved down a level; the ADR records this as a follow-up, and no test was changed.

Update (same day): `CurrentUserRepositoryTest` and `SpringBasedPasswordEncoderTest` now carry the `ComponentIntegrationTest` tag, and the ADR's follow-up bullet was replaced by the rule. `./gradlew test`, `./gradlew build` and `./gradlew :qubigo-servlet:allCmpTests` succeeded.


### Closed: 2026-10-16

Closed as resolved by the user. The countermeasures are applied: test design guidelines and ADR 0006, the doer and validator prompts point at the guidelines, and the scenario, migration, wiring and characterization tests were removed or restructured. Whether the guidelines change the test code of the next iteration was not observed; no review date was set, and the user chose to close without an observation period.

Follow-up idea (not part of this note's resolution): a code walkthrough assembly line that compares a branch against its siblings, ADRs and guidelines and leaves review remarks for an interactive session. It addresses the escape cause (no check on test code before human review). Tracked as the yak "Code walkthrough assembly line that leaves review remarks" (`yx show`), not as a kaizen note.
