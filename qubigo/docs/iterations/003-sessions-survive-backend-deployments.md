# Iteration 003 — Sessions survive backend deployments

Status: Scope agreed in the product-owner interview on 2026-10-15. Ready for implementation planning. Implementation has not started.

## Goal and observed problem

Qubigo releases frequently. After a backend deployment, signed-in players get a login screen and must sign in again. This interrupts the connected playing experience described in `docs/vision.md`.

Make refresh-token validation persistent so backend restarts and deployments no longer force players to sign in again. A brief interruption during deployment is acceptable, including during play, provided players can continue without signing in again. This iteration does not promise uninterrupted backend availability.

## Agreed scope and behavior

- Replace production in-memory refresh-token storage with persistent storage in the existing backend database.
- Preserve existing token expiry and session-duration behavior. Persistence must not extend a token's validity.
- Preserve existing logout behavior, including revocation of all refresh tokens for the user. Revocation must itself survive restarts.
- Preserve the existing authentication API, cookies, token validation, and client behavior unless investigation demonstrates a change is necessary to achieve the agreed outcome. Record any such finding before expanding scope.
- Cover backend deployments and restarts. There is no separate web, iOS, or Android session redesign.

## Initial rollout

The product owner accepts one final sign-in when this change first ships. Tokens held only in the old process's memory do not need to be migrated. Do not accept an otherwise unrecognized token merely because its signature is valid.

Tokens issued after persistent storage is enabled must remain usable across subsequent backend restarts and deployments until they expire or are revoked under the existing rules.

## Current implementation and planning inputs

Inspection during the interview confirmed:

- `qubigo-backend/qubigo-servlet/src/main/kotlin/com/qubigo/qubigobackend/SecurityConfig.kt` wires `InMemoryRefreshTokenRepository` as the production `RefreshTokenRepository`.
- The repository port is in `users/domain/security/RefreshTokenRepository.kt`; its in-memory adapter is in `users/adapters/security/InMemoryRefreshTokenRepository.kt`, below the backend's `com/qubigo/qubigobackend/` package.
- `users/adapters/security/JwtBasedAuthenticator.kt` contains `AuthenticationTokenizer`, which saves newly issued refresh tokens. Refreshing checks the token through `JwtTokenizer`, looks it up in the repository, and reloads the user. It returns the existing refresh token with a new access token.
- Logout calls `deleteForUser` on the refresh-token repository. Preserve this user-wide revocation behavior.
- The backend uses SQLite and Flyway. Use its existing persistence and migration conventions; keep the repository port and adapter separation.
- JWT signing configuration comes from `JWT_SECRET_KEY`. Verify that deployment retains the signing key and the database; persistent token records alone cannot ensure continuity if either is replaced.

Paths beginning with `users/` above are relative to `qubigo-backend/qubigo-servlet/src/main/kotlin/com/qubigo/qubigobackend/`.

Before implementation, read `qubigo-backend/AGENTS.md` and its referenced architecture guidance. Inspect every repository operation and caller, existing authentication tests, database lifecycle, and deployment configuration before selecting the schema and adapter design. Persist a stable user association rather than a serialized in-memory user snapshot. Follow existing handling for missing users and invalid tokens. Choose token storage and cleanup details during implementation planning without changing session semantics or exposing token values in logs or evidence.

The required task discovery command, `yx list --ready`, was attempted during this interview but was unavailable (`yx: command not found`). No task state was inferred.

## Acceptance criteria

1. A player signs in, the backend stops, and a new backend process using the same persistent database and signing configuration accepts that player's still-valid refresh token without another sign-in. Exercise refresh after the original access token expires so access-token validity cannot mask a failure.
2. Repeated backend restarts retain this behavior without extending the existing refresh-token expiry.
3. Logout revokes the user's refresh tokens according to current behavior. Previously revoked tokens remain rejected after restart, including tokens from multiple sessions for that user.
4. Expired, malformed, tampered, and unrecognized refresh tokens remain rejected under the existing authentication rules. Restarting never restores expired or revoked access.
5. The database migration works against an existing database and preserves existing users and game data. A fresh database also starts successfully.
6. The deployed application's database location and signing configuration persist across releases. Verify this from repository deployment configuration (compose volume, datasource path, `JWT_SECRET_KEY` pass-through, release script) and the disposable process-replacement test, and record the operational prerequisites. The product owner's dev-environment confirmation (2026-10-15: backend container restart preserved database and signing key, volume not deleted) is accepted as the live evidence for this iteration; no further operator checklist is required. Live production continuity across an image replacement is not claimed; it is a release-time prerequisite for the first production deployment and is recorded as outstanding.
7. In a representative client, a deployment may briefly interrupt service, but a player with a valid persisted session can continue without entering credentials. Verify this after access-token expiry and record the tested client and actual result.

## Validation and handoff

Follow the backend's test-first conventions and establish the existing test baseline. Extend meaningful authentication and persistence integration coverage, including recreation of the application against the same database. An in-memory repository test or two reads within one running application does not demonstrate restart persistence.

Run the required backend full test suite (`./gradlew test`) and build (`./gradlew build`). Report actual results and unrelated baseline failures. Record a representative deployment/restart acceptance check and any unavailable manual checks as outstanding; do not claim production validation from unit tests alone.

The implementation plan should define the migration, production wiring, persistence lifecycle, regression coverage, and reproducible restart check. Deliver the implementation and validation evidence for review. This document records product scope; implementation planning owns the detailed execution sequence.

## Out of scope

Changing session duration, token rotation policy, logout semantics, authentication UX, password or onboarding flows, migrating existing in-memory tokens, uninterrupted deployment infrastructure, game recovery redesign, and broader authentication changes. Production deployment and app-store releases are separate work.

## Interview decisions

- The observed symptom is a login screen after backend deployment.
- A brief interruption is acceptable; signing in again is not.
- The requested fix is persistence of refresh-token validation, currently held in memory.
- Existing expiry and logout behavior must remain unchanged.
- One final sign-in at the initial rollout is acceptable.
- 2026-10-16: Criterion 6 relaxed. The owner's dev confirmation plus source/disposable evidence suffices; a detailed operator before/after checklist is not required.

No product code was changed or tests run during this interview.
