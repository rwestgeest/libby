# Iteration 004f — Competitions tab and overview

Status: seed drafted on 2026-10-21. Tab visibility and the admin's Joined
section agreed with the owner; the rest is proposed. Implementation has not
started. Depends on 004e (grouped card list pattern).

## Why

Admins have no way into competition management: they type
`#/admin/competitions/new` or reuse a bookmarked competition URL, and there is
no list of existing competitions. Players reach a joined competition only
through its joining link or QR, or a "My competitions" link on the Profile
page. Before Spellenspektakel (7–8 November 2026) both need a visible way in.

Source inspection (2026-10-21, `main`):

- The bottom tab bar (`bottom-tab-bar-component.js`) renders
  `UserProfile.menu()` (`app/domain/user-profile.js`), which returns the same
  `Menu.forPlayer()` (Home, New game, Profile, Info; `app/domain/menu.js`) for
  admins and players. The tab bar re-renders when the profile module notifies
  its views.
- `GET /api/competitions` (`CompetitionParticipationController.joined`)
  returns the signed-in player's joined competitions as `CompetitionPreviewDto`
  with `status`, `running` and `closed`. That is enough to decide whether a
  player has an open or running competition.
- There is no admin endpoint that lists competitions; `CompetitionRepository`
  has `byId`, `joinedBy`, `started` and `awaitingFinalization`, but nothing
  that returns all of them.
- "My competitions" (`competition-page.js` `renderList`) is a bare list of
  links; 004e moves it onto the grouped card list.

## Owner decisions

1. **Competitions tab.** The bottom tab bar gets a Competitions tab (trophy
   icon, label "Competitions").
   - **Admin:** always visible, so an admin with no competitions can still
     create one. It opens the competitions overview.
   - **Player:** visible only while the player has joined at least one
     competition that is **open or running** (not closed or finalized). It
     opens "My competitions".
2. **Admins see a Joined section.** An admin who joined competitions as a
   player sees them in a Joined section of the overview, so they do not need
   "My competitions" separately.
3. *(proposed)* **Overview groups.** Using the 004e grouped card list, in this
   order: Joined (admins only, omitted when empty), Running, Open, Draft,
   Closed, Closed without starting (opened but never started before its
   closing time). Each row shows name, closing time with year, and a status
   badge; running and closed rows also show participant count. A row opens
   the existing management page `#/admin/competitions/:id`. A "New
   competition" `accent-pill` action sits above the groups. Empty groups show
   their empty message, so the admin can see the state of everything at once.
4. *(proposed)* **Admin list endpoint.** `GET /api/admin/competitions`
   returns all competitions with the fields the overview needs (id, name,
   closing time, status, running, closed, participant count), admin only,
   backend enforced. The grouping rule (which group a competition belongs to)
   lives in one tested place, reusing the presentation-state rules from 004b
   rather than a second interpretation.
5. *(proposed)* **When the player's tab updates.** The player's tab state is
   loaded with the profile after sign-in and app start, and refreshed after
   joining a competition. It does not poll: a competition that closes while
   the app is open drops the tab on the next load. If the lookup fails, the
   tab stays hidden and nothing else breaks. The Profile page's "My
   competitions" link is removed, now that the tab replaces it.

## Acceptance examples

- An admin with no competitions sees the Competitions tab; it shows empty
  groups and "New competition".
- An admin sees each competition in exactly one group, matching its state
  on the management page; tapping a row opens that competition's management
  page.
- An admin who joined a running competition also sees it under Joined.
- A player who joined an open competition sees the tab; tapping it shows
  "My competitions" with that competition.
- A player who has joined nothing, or whose joined competitions are all
  closed or finalized, has no Competitions tab.
- Right after joining a competition, the player's tab appears without
  signing in again.
- `GET /api/admin/competitions` is refused for non-admins.
- The tab bar still fits at phone width with five tabs, with touch targets
  of at least 44px.

## Planning and implementation

- Follow the `careful-plan` line. The design walkthrough should cover how the
  menu becomes data-dependent for players (where the open/running lookup
  lives, how the tab bar is notified) and the admin list query.
- Follow backend guidance and the ADRs for the new endpoint; test-drive the
  grouping rule and menu visibility as domain rules.
- Use the 004e card list and form patterns; no new one-off styling.
- Keep existing competition routes and ids.

## Out of scope

- Search, filtering, sorting options or paging in the overview.
- Archiving or deleting competitions; close now (004c).
- Player-side visibility of competitions they have not joined (joining stays
  link and QR based).
- Live refresh of the overview or tab while it is open.
- Admin CLI.
