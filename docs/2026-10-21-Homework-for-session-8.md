# Homework for session 8

## 2026-10-21 — Scoping competition follow-ups and turning the design system into a specification

Context: while the factory ran qubigo iteration 004c (closing-time safeguards)
in the live Qubigo working tree, I used an agent session to scope the next
chunks found while testing 004b: form fields on the competition management
page are not aligned, and admins have no overview of competitions. All writes
went to a separate Git worktree on `main`
(`git worktree add ../qubigo-main main`) so the active run's branch and working
tree stayed untouched. That worked smoothly and is a pattern worth keeping
while a line is running.

### 004e — design system: forms and lists

The agent read the design system, its clickable prototype and the two
competition pages before proposing anything. Source inspection corrected my
assumption: the management form does not use inline styles. It bypasses the
design system entirely by rendering bare `<label>`/`<input>` pairs, and the
shared `<input-field>` component could not host it: it lacks `min`, `step` and
`readonly`, and its label never associates with the input. The inline styles
(and non-existent tokens with an off-palette navy fallback) are in the event
display instead.

Decisions:

- Admin forms look like the login and onboarding forms (my call), but with a
  visible label above each field, because edit mode pre-fills values and date
  and number inputs show no placeholder.
- Extend `<input-field>` rather than add a second input style.
- Trimmed scope to the form and one grouped card list pattern, which unblocks
  the 004f overview before Spellenspektakel (7–8 November). The table pattern
  and event-display clean-up are deferred.
- Start 004e only after 004c merges, because both change the same page.

Seed: [004e-design-system-forms-and-lists.md](../qubigo/docs/iterations/004e-design-system-forms-and-lists.md).

### 004f — competitions tab and overview (drifted, then refocused)

I suggested a Competitions tab in the bottom bar. The agent turned this into a
004f seed: the tab is always visible for admins and opens an overview grouped
by state, with a Joined section; players see it only while they have joined
an open or running competition. The existing player endpoint already returns
`status`, `running` and `closed`, so only the admin list needs a new endpoint.

At that point I noticed we had slid from the design system into slice
requirements and pulled the conversation back. Lesson: because everything is
connected, an agent will happily follow the thread into the next slice, so the
human has to hold the session's focus. The draft is committed but its
grouping, endpoint and refresh details are still proposals:
[004f-competitions-overview.md](../qubigo/docs/iterations/004f-competitions-overview.md).

### Sketching patterns before the run

Instead of letting the 004e run invent the visual details, the agent sketched
them in the existing clickable prototype (`qubigo-frontend/doc/prototype/`):
field states (empty, focused, hint, error with the 2926 typo, read-only), the
new and edit competition forms, participants and "My competitions" as grouped
card lists, and a 004f overview mock. The headless browser lacked system
libraries, so no screenshots; it checked each screen in jsdom instead (renders
without script errors, labels linked to inputs, error flow works). I reviewed
the page in my own browser and approved it.

Side question: GitHub does not render HTML files. Public repositories can use
a preview service such as raw.githack.com; a private repository needs Pages, a
local checkout, or a privately published page.

### The design system becomes the specification

The design system document said "where the code and this document disagree,
the code wins": it described the app after the fact, so each run decided the
details itself and wrote them down afterwards. I asked whether making it a
requirement would break the factory. Mechanically, no: nothing in the factory
reads it; agents reach it only through iteration briefs. What changes is
*when* design decisions are made, and by whom: by the owner, in the document
and prototype, before the run.

I needed a concrete before/after walkthrough of a run to understand this; the
abstract list of risks did not land. The risks and their guards:

- A run might use a pattern that does not exist yet, so patterns are marked
  **Planned (iteration)** until that iteration builds them.
- An agent might "fix" every listed drift, so drift is fixed only when an
  iteration names it.
- The 004e acceptance changed from "document the patterns" to "build to the
  specified patterns and remove the Planned markers; deviations are agreed
  and recorded".

The frontend `AGENTS.md` now points at the design system as the specification
for every UI change, so agents find it without a brief mentioning it.

### Commits (qubigo `main`)

- `d54fab55` — scope 004e (forms and card list)
- `2ebfe62f` — draft 004f seed (competitions tab and overview)
- `85a59aac` — prototype sketches of the 004e patterns
- `85fdb6bf` — design system becomes the UI specification, with the planned
  004e patterns

The design system document and prototype are application documentation and
are not part of the sharing snapshot; see the commits above.

### Next steps

- Let the 004c run finish and merge it, then start 004e from the updated seed.
- Settle 004f's proposed details (groups, participant count, tab refresh)
  before its run.
- Watch whether the 004e run follows the specified patterns or still invents
  details. That will show whether "decide in the document first" works.
