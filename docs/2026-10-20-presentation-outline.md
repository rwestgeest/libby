# Presentation outline — Tuesday 20 October 2026

Course: Build a Software Factory: Hands-off agentic coding for experienced engineers.
Slot: **7 minutes**, tutors and fellow students. Not facilitating; tutors decide on discussion.
Audience knows the factory idea; **Qubigo needs a one-slide introduction**.

**Message in one sentence (draft):** A skeptic's factory: looking at the code
the factory produced (genchi genbutsu) found problems, and a kaizen ledger plus
"pull the cord" five-whys is how I'm learning what to change, not just what to patch.

**Ask of the room:** feedback. See the question on slide 6.

## Timing (about 1 min per slide)

### 1. Where I am (0:00–0:45)
- Factory through homework 6: machines as ACP agents, `bin/factory --run/watch/stop`, assembly lines (`careful-plan`).
- Target: **Qubigo** — "chess meets 3D tic-tac-toe": a 3-in-a-row strategy board game for 2–3 players on a 3×3×3 cube, 27 stones with three properties each, 20–30 minutes. "Easy to learn, hard to master" ([qubigo.com](https://www.qubigo.com), [vision](../q/docs/vision.md)).
- The software: a web and mobile edition (play online, pass-and-play, 3D board, tutorial) that is an on-ramp to the **physical game**; its job is to turn a curious player into a fan and eventually an owner.
- Why it suits a factory: a real product with frequent releases, a real deadline, and a real product owner interview (not a toy like Tetris).
- Stance: skeptic about output quality, so I read the code, not just the green checks.

### 2. Genchi genbutsu on run 003 (0:45–1:45)
- Iteration 003, "sessions survive backend deployments": after each release players had to sign in again. Goal: persist refresh tokens in the existing SQLite database; keep expiry, logout and API unchanged ([spec](../q/docs/iterations/003-sessions-survive-backend-deployments.md)).
- Went and looked at the code from the iteration 003 run.
- Found: scenario-style session-persistence tests, hard to read, boolean flags, duplicated helpers, sleeping on wall-clock time.
- Root causes: no test standard, plan prescribed test mechanics, no validator checked test code.
- Evidence: [kaizen note](../qubigo/docs/kaizen/2026-10-16-session-persistence-tests-hard-to-read-and-badly-structured.md).

### 3. The kaizen ledger (1:45–2:45)
- Borrowed from Matt Wynne's Memba project: one note per problem, resolved or open.
- Same run exposed seven more notes (Java 25 harness, `gradlew` directory, plan outside repo, uninformative commits, missing `yx`, tests split by task, validator looping on evidence nobody can produce).
- Fixed what was cheap (installed `yx`, test guidelines in `AGENTS.md` and machine prompts, deleted the scenario tests).
- Visual idea: a short list showing note → fix → status.

### 4. Iteration 004: competitions, run, failure (2:45–4:15)
- Iteration 004, "event competitions": for the Spellenspektakel in Utrecht on 7–8 November 2026 (hard deadline). Visitors scan a QR code, register, join; random pairing, an allowance of games each, 3/1/0 points, speed breaks ties, unfinished games are lost at closure.
- Much larger than 003: admin lifecycle, onboarding, automatic pairing, ranking. Grilled the requirements in a product-owner interview; seed in `q/docs/iterations/004-competitions.md`.
- Run: `careful-plan`, mixed models (DeepSeek doer/planner; Claude/Gemini reviewers). Tasks 1–6 accepted; task 7 stuck after repeated review findings and a doer answer without JSON.
- Fixed factory issues along the way (result and log parsing, blocked run handling, validator result format).
- Visual: workflow diagram from [design-review.md](runs/qubigo-004/design-review.md).

### 5. Pulling the cord (4:15–5:30)
- While repairing, I read the code and plan and found them **overly complicated**.
- Stopped. Preserved the work (`221d7bb3`, no acceptance claimed).
- Five whys in progress, two chains: unreadable doer result; increasing complexity of repair and design cycle.
- Rule for myself: discover causes first; a rewrite is not already decided.
- Status: [18 October checklist](runs/qubigo-004/2026-10-18-todo.md).

### 6. Next, and what I'd like from you (5:30–7:00)
- Next: apply five-whys learnings, then try again as **004a**. Not started yet.
- Feedback wanted (pick one to say aloud, keep the rest as backup):
  - How do you guide design, not only implementation, so a plan stays simple?
  - Should a validator review design complexity, and with what lens?
  - Restart whole, or keep tasks 1–6?

## Open decisions for you
- [x] Date: Tuesday 20 October. Demo: none; diagrams and kaizen list carry it. Feedback questions as drafted on slide 6.
- [ ] Which single feedback question to lead with (slide 6).
- [ ] Deadline pressure (8 November) is a real part of the 004a decision; say so on slide 5 or 6?
- [ ] Five-whys results: update slides 5 and 6 if they land before Tuesday.

## Cut list if short on time
Drop slide 3 detail first (keep one sentence), then merge slides 2 and 3.
