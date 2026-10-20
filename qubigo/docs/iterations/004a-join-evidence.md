# 004a — signed-in competition joining (1d)

Implemented; awaiting factory validation and commit. Task 1d remains open for the planner. No commits made by the doer.

## Delivered boundary

- An organizer revisiting an open competition obtains a separate `/#/competitions/:id` participant URL, selectable for copying, and a locally generated QR encoding exactly that URL (`qrcode` 1.5.4). Drafts do not distribute joining links. Failed QR generation leaves the URL usable and reports the failure.
- Signed-in regular users and organizers can preview the name, allowance, device-local closing time with IANA timezone, and concise 3/1/0, points-before-speed, partial-allowance and closure-loss rules. Previewing never enrolls anyone.
- Explicit joining records only the authenticated caller. Confirmation says “You’ve joined — waiting for the competition to start.” Reload uses persisted membership. Another user's membership does not confer membership on the caller.
- Competition owns immutable membership transitions: drafts refuse joining; new participants require time strictly before closing; existing membership is idempotent even after closing. Refusals do not reach save; storage failures remain failures.
- Profile links to My competitions (`#/competitions`), backed by an authenticated joined-only query. No public discovery or participant roster is exposed. Existing shared ADMIN-only management endpoints remain unchanged and protected.
- Actual-Router regressions cover preview/list/join completion after navigation and isolation of subsequent openings. Browser startup retains a signed-in competition deep link. Native cold/warm hash-link routing is simulated, not device-attested.

## Responsibilities and architecture trace

Read backend/frontend AGENTS, ADRs 0002/0005/0007/0008, architecture/domain-module guidance and the responsibility-driven lens. Traced Games: `asValidMoveParameters` constructs Position/Piece via field-keyed conversions → controller calls MakeMove on successful conversion → command obtains state/user → Game.move checks state-dependent rules and withMove returns a copy → SQLite adapter reconstructs/maps/appends journal data.

Joining has no submitted settings or participant identifier to convert. Existing typed settings validation/Domain Primitives and field-specific failures are preserved. `JoinCompetition` resolves CurrentUserRepository, loads Competition, supplies Clock time to Competition.join, and saves only success. `CompetitionParticipation` supplies authenticated read/query context. Competition owns draft visibility and membership/time rules. The SQLite adapter maps EAGER membership sets through a new `competition_participants` table; its joined query filters by caller identity. Commands do not invoke sibling commands or SQL use cases. No Games, Invitations, Notifications or onboarding collaboration was added.

## Test-first record

Commands were run from the corresponding service directory. Logs were kept under `/tmp/004a-*.log` during this run.

1. `./gradlew :qubigo-servlet:test --tests '*JoinCompetitionTest'`: first compilation failed for absent membership API. With the API stubbed, corrected the fixture to use the existing validated ID primitive; the meaningful red run then had **4 assertion failures of 6** (draft refusal, new membership, exact/after deadline). Implemented immutable aggregate behavior.
2. `./gradlew :qubigo-servlet:test --tests '*JoinCompetition*'`: compilation failed for missing JoinCompetition. Added caller-resolution/aggregate/save coordination, including refusal/no-save and storage failure coverage.
3. `./gradlew :qubigo-servlet:test --tests '*JoinCompetition*' --tests '*SqliteBasedCompetitionRepositoryTest'`: compilation failed for missing joinedBy port. Added membership persistence/migration and joined filtering. Re-run passed, covering immutable joins, coordination, membership reconstruction and filtering out unjoined/another user's competitions.
4. `./gradlew :qubigo-servlet:test --tests '*CompetitionParticipationTest'`: missing preview/query types failed compilation. Added preview without enrollment, own-membership projection and caller-specific listing.
5. `./gradlew :qubigo-servlet:test --tests '*CompetitionParticipation*'`: missing participation controller failed kapt compilation. Added thin DTO mapping/controller and wiring. `./gradlew :qubigo-servlet:test --tests '*competitions*'` then passed. Controller tests cover regular/admin joining, anonymous refusal on every new endpoint, no arbitrary submitted identity, draft preview refusal, no rosters and honest enrollment failure. Existing tests still protect every admin management endpoint.
6. `npm test -- --runInBand test/competitions/pages/competition-page.test.js`: missing page failed the suite. Added preview/join/list UI and repository operations. Re-run: **15 passed**; then added the signed-in actual-Router startup regression. Immediate confirmation and reload are separate tests.
7. `npm test -- --runInBand test/competitions/pages/competition-sharing.test.js`: missing sharing adapter failed the suite. Added URL/QR collaborator, open-only distribution, QR failure presentation and actual-Router late QR success/failure guards. Competition suites: **50 passed** at that point.
8. `npm test -- --runInBand test/pages/profile-page.test.js`: new ordinary revisit link test failed with `Cannot read properties of null (reading 'getAttribute')` (**1 failed, 15 passed**). Added the Profile link and wired participant routes/pages in app.js.
9. Added native cold/warm competition-link regressions against existing IncomingLinkHandler; no handler change was needed. Extracted the existing deferred-request helper to `test/utils/hold-request.js` for reuse rather than copying it.

## Final verification

- `npm test -- --runInBand test/competitions test/pages/profile-page.test.js test/adapters/incoming-link-handler.test.js`: **5 suites, 74 passed**.
- `npm test -- --runInBand`: **68 passed suites, 1 skipped; 758 passed tests, 11 skipped**, exit 0.
- `npm run build`: exit 0; production Vite build succeeded.
- `./gradlew test`: exit 0; **801 servlet tests (86 competition), 18 CLI tests**. Counts are from current XML reports under `qubigo-backend/build/reports/*/test-results/test/`, not the unrelated historical `build-reports/` directory.
- `git diff --check`: exit 0.

Dependency installation reported 48 audit vulnerabilities across the existing frontend dependency tree; no unrelated audit upgrades were attempted. This is not a claim that all dependencies are vulnerability-free.

## Explicitly not delivered

Signed-out preview/auth return, registration/verification/store-install retention, physical QR scanning or OS association/native delivery attestation, participant rosters, manual start, running-state late enrollment, matching/play/results, auto-refresh, scheduler, automatic closure and concurrent-request serialization/retry guarantees remain deferred. Native simulations are not real-device evidence; task 6 still needs the owner/device tester's named platform/device, app version, steps and observed outcomes.
