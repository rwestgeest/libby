# 004a / 5c — stand display with auto-refresh

Implementation delivered; task 5c remains open for planner acceptance and factory commit.

## Delivered capability

Any signed-in ADMIN can open a full-screen stand display for a specific competition at `#/admin/competitions/{id}/stand`. It shows rank, player name, points, completed games out of allowance, and live/final status. The display auto-refreshes every 30 seconds to reflect new outcomes. Admin-only route registration and presentation tests are included. Reuses the existing admin-only `GET /{id}/standings` endpoint and its `CompetitionStandings` projection; no new backend API, competition rule or schema was needed.

### Acceptance coverage

- **A — Live standings:** navigating to the stand shows rank, player name, points, completed/allowance and response time for each row, with a `Live` status label and the games-per-participant count.
- **A — Final standings:** when the snapshot returns `FINAL` status, the display says `Final` and the correct finalized points/completions are shown. A player with null average response time displays `–` instead of a fabricated timing.
- **B — Auto-refresh:** calling `page.refresh()` after changing the mock data updates the displayed rows with new points and status. The refresh reuses the existing `page.refresh()` call (invoked by the 30-second timer).
- **B — Failure preserves existing display:** a failed refresh retains the previously loaded standings table and shows `Unable to refresh. Will retry.` in a dedicated status element.
- **C — Admin authorization:** a regular user (non-admin) sees `Admin access required` without fetching the standings endpoint. The route is registered under `#/admin/...` and the page checks `userProfile.role`.
- **C — Late success after navigation:** if the standings load completes after the admin has navigated to Profile, the active page content is not overwritten. The opening lifecycle token guards against stale responses.
- **D — Failed initial load:** when the first standings call fails, an honest error message is shown with a retry link, and no partial or empty table is displayed.
- **D — Timer lifecycle:** navigating away from the stand closes it, stopping the auto-refresh timer. A subsequent `page.refresh()` call is ignored because `close()` invalidated the opening token.

### Test commands and results

Run from `qubigo-frontend/`:

1. `npx jest test/competitions/pages/competition-stand-page.test.js --no-cache --verbose`
   - 8/8 passed (standalone stand-page tests covering A–D above)

2. `npx jest test/competitions/ --no-cache --verbose`
   - 147 passed (all competition tests, including the 8 new stand-page tests)

3. `npx jest --no-cache --verbose`
   - 894 passed, 11 skipped (full frontend suite; skips are pre-existing)

4. `npx vite build`
   - Production build succeeds (187 modules transformed, ~240 kB index)

### Files changed

- `qubigo-frontend/app/competitions/pages/competition-stand-page.js` — new stand page with auto-refresh
- `qubigo-frontend/test/competitions/pages/competition-stand-page.test.js` — focused stand-page tests
- `qubigo-frontend/app/competitions/adapters/api-based-competitions-repository.js` — added `standings(id)` method
- `qubigo-frontend/app/competitions/pages/competition-draft-page.js` — added `#view-stand` link in the admin saved view, shown when competition is OPEN or STARTED (hidden for DRAFT)
- `qubigo-frontend/app/app.js` — import and wire `CompetitionStandPage` at route `#/admin/competitions/:id/stand`

### Deferrals preserved

Personal rank/speed display, participant-facing standings, public leaderboard, websocket/push refresh, configurable cadence, historic data snapshots, PDF/export, full-screen kiosk mode enhancements, physical display hardware attestation and prize-presentation workflow remain open or in later capabilities. Backend unchanged; no new endpoint, game lifecycle or competition rule was introduced.