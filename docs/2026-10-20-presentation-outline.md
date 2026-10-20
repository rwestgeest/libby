# Presentation outline — Tuesday 20 October 2026

Course: Build a Software Factory: Hands-off agentic coding for experienced engineers.
Slot: **7 minutes**, tutors and fellow students. Not facilitating; tutors decide on discussion.
Audience knows the factory idea; **Qubigo needs a one-slide introduction**.

**Message in one sentence (draft):** A skeptic's factory: looking at the code
the factory produced (genchi genbutsu) found problems, and a kaizen ledger plus
"pull the cord" five-whys is how I'm learning what to change, not just what to patch.

**Ask of the room:** feedback. See slide 7.

## Timing (seven slides, about a minute each)

### 1. Where I am (0:00–0:45)
- Factory through homework 6: machines as ACP agents, `bin/factory --run/watch/stop`, assembly lines (`careful-plan`).
- Target: **Qubigo** — "chess meets 3D tic-tac-toe": a 3-in-a-row strategy board game for 2–3 players on a 3×3×3 cube, 27 stones with three properties each, 20–30 minutes. "Easy to learn, hard to master" ([qubigo.com](https://www.qubigo.com), [vision](../q/docs/vision.md)).
- The software: a web and mobile edition (play online, pass-and-play, 3D board, tutorial) that is an on-ramp to the **physical game**; its job is to turn a curious player into a fan and eventually an owner.
- Why it suits a factory: a real product with frequent releases, a real deadline, and a real product owner interview (not a toy like Tetris).
- Stance: skeptic about output quality, so I read the code, not just the green checks.

### 2. Genchi genbutsu on run 003 (0:45–1:30)
- Iteration 003, "sessions survive backend deployments": after each release players had to sign in again. Goal: persist refresh tokens in the existing SQLite database; keep expiry, logout and API unchanged ([spec](../q/docs/iterations/003-sessions-survive-backend-deployments.md)).
- Went and looked at the code from the iteration 003 run.
- Found: scenario-style session-persistence tests, hard to read, boolean flags, duplicated helpers, sleeping on wall-clock time.
- Root causes: no test standard, plan prescribed test mechanics, no validator checked test code.
- Evidence: [kaizen note](../qubigo/docs/kaizen/2026-10-16-session-persistence-tests-hard-to-read-and-badly-structured.md).

### 3. The kaizen ledger (1:30–2:15)
- Borrowed from Matt Wynne's Memba project: one note per problem, resolved or open.
- Same run exposed seven more notes (Java 25 harness, `gradlew` directory, plan outside repo, uninformative commits, missing `yx`, tests split by task, validator looping on evidence nobody can produce).
- Fixed what was cheap (installed `yx`, test guidelines in `AGENTS.md` and machine prompts, deleted the scenario tests).
- Visual idea: a short list showing note → fix → status.

### 4. Iteration 004: competitions, run, failure (2:15–3:15)
- Iteration 004, "event competitions": for the Spellenspektakel in Utrecht on 7–8 November 2026 (hard deadline). Visitors scan a QR code, register, join; random pairing, an allowance of games each, 3/1/0 points, speed breaks ties, unfinished games are lost at closure.
- Much larger than 003: admin lifecycle, onboarding, automatic pairing, ranking. Grilled the requirements in a product-owner interview; seed in `q/docs/iterations/004-competitions.md`.
- Run: `careful-plan`, mixed models (DeepSeek doer/planner; Claude/Gemini reviewers). Tasks 1–6 accepted; task 7 stuck after repeated review findings and a doer answer without JSON.
- Fixed factory issues along the way (result and log parsing, blocked run handling, validator result format).
- Visual: workflow diagram from [design-review.md](runs/qubigo-004/design-review.md).

### 5. Pulling the cord, then five whys (3:15–4:15)
- While repairing, I read the code and plan and found them **overly complicated**. Stopped and preserved the work (`221d7bb3`, no acceptance claimed).
- Five whys, done together with the agent ([summary and picture](runs/qubigo-004/five-whys-summary.md)). Headline: *the process checked whether machinery was correct more than whether it was needed.*
- Threads: speculative failure cases became mandatory guarantees; design intent stayed tacit; documented responsibilities were not upheld; plans detailed too far ahead; a doer answer with no JSON.
- Visual: `five-whys-map.svg`.

### 6. Second attempt: 004a (4:15–5:45)
- Changes made before the run: design checkpoint with the owner before planning ([walkthrough](runs/qubigo-004a/design-walkthrough.md)); detail only the next capability; domain primitives and bounded TDD; one tool-free repair for a missing doer JSON; shorter prompts (an `Argument list too long` failure on a 136 KB embedded diff, fixed by pointing machines at the working tree instead of a bigger limit).
- Result: the run finished, 17 of 17 plan tasks checked. Last stop was the human device-evidence gate, which I attested myself. The factory could not do that part.
- Evidence per capability in `q/docs/iterations/004a-*-evidence.md`. Example from the closure evidence (task 4a): 985 backend and 18 CLI tests, 870 frontend tests, all passing. (Agent-reported, not re-run by me; figures from later tasks will differ.)
- Delivered: draft, edit, open, join, participants, start play, matching and invitations, game labels, progress, automatic closure, standings, stand screen, reminders, onboarding.
- Screenshots (mobile width, **mock API data**, real frontend code from `feature/004a-competitions`):
  - [admin: new draft](runs/qubigo-004a/screens/1-admin-new-draft.png)
  - [admin: open competition with participants](runs/qubigo-004a/screens/2-admin-open-share.png)
  - [player: join](runs/qubigo-004a/screens/3-player-join.png)
  - [player: in play, own progress](runs/qubigo-004a/screens/4-player-playing.png)
  - [admin: live stand](runs/qubigo-004a/screens/5-admin-stand.png)
- Still honest to say: the screens are plain and utilitarian. The code is the thing I'm asking about.

### 7. What I'd like from you (5:45–7:00)
- Feedback wanted (lead with one, keep the rest as backup):
  - How do you guide design, not only implementation, so a plan stays simple?
  - Should a validator review design complexity, and with what lens?
  - The review burden: how do I verify 004a's output without reading everything?
- Possible closing line: *the factory got better because I went and looked.*

## Open decisions for you
- [x] Date: Tuesday 20 October. Demo: none; diagrams, kaizen list and screenshots carry it.
- [ ] Which single feedback question to lead with (slide 7).
- [x] "17 of 17 tasks" with the device evidence attested by me: keep as drafted on slide 6.
- [ ] Do you have an assessment of the 004a code quality (simpler than 004?) to offer? Not in the repo yet; the outline claims none.
- [x] Deadline (8 November): 004a is probably releasable in time. Say "probably" on slide 6 or 7.
- [ ] Screenshots use mock API data. Replace with real-app screenshots from a running backend if you prefer.

## Cut list if short on time
Drop slide 3 detail first (keep one sentence), then merge slides 2 and 3; shorten the delivered list on slide 6 to the three screenshots.
