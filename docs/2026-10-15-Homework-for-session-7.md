pull changes from upstream fork
then pull in iteration 5a

5a - Machine config
- create AGENTS.md
- possibly auth

For example create smaller fine grained steps in plan

-----

6 agents in the background

finer grained control (start, stop, watch, resume)

creates events.jsonl

so we can analyse machine configuration 

--- bonus bonus ----

Ability to steer. 007

## 2026-10-16 — Kaizen on iteration 003 tests; start of qubigo iteration 004

Started by reviewing the iteration 003 factory run output. The tests the model
had written for session persistence (Process, Restart, Wiring scenario tests in
`qubigo-backend/qubigo-servlet/src/test/kotlin/.../repositories/`) were long,
scenario-style, hard to read, used boolean flags, duplicated helpers, and slept
on wall-clock time. This triggered a kaizen deep-dive:
[2026-10-16-session-persistence-tests-hard-to-read-and-badly-structured.md
](../qubigo/docs/kaizen/2026-10-16-session-persistence-tests-hard-to-read-and-badly-structured.md)
— the most detailed kaizen note yet, covering root causes (no test standard,
plan prescribed test mechanics, no validator checked test code), recommendations
and a resolution that deleted the scenario tests, replaced them with one simple
wiring test, and added test-design guidelines to `qubigo-backend/AGENTS.md` and
the machine prompts.

Reviewed the other iteration 003 kaizen notes that the run exposed simultaneously:

- [Java 25 harness breaks the build](../qubigo/docs/kaizen/2026-10-16-java-25-harness-and-machine-local-paths-in-evidence.md)
  — default JDK 25 fails Gradle; disposable JDK 21 paths in evidence are machine-local
  and not reproducible.
- [Recurring setup failures — gradlew wrong directory](../qubigo/docs/kaizen/2026-10-16-recurring-setup-failures-gradlew-wrong-directory.md)
  — each task rediscovers that `./gradlew` lives in `qubigo-backend`, not the repo root.
- [Plan lives outside repo and doubles as a log](../qubigo/docs/kaizen/2026-10-16-plan-lives-outside-repo-and-doubles-as-a-log.md)
  — the plan at `factory/runs/qubigo-003/plan.md` is not in the repo; checkboxes
  are unreliable.
- [Uninformative factory commits](../qubigo/docs/kaizen/2026-10-16-uninformative-record-factory-task-commits.md)
  — every commit reads "Record factory task", no subject or task number.
- [yx list missing in every task](../qubigo/docs/kaizen/2026-10-16-yx-command-missing-in-every-task.md)
  — `yx` was not installed, so every task wasted a command. Resolved by installing
  yx v0.5.1 at `~/.local/bin/yx`.
- [Tests after implementation split by task](../qubigo/docs/kaizen/2026-10-16-tests-after-implementation-split-by-task.md)
  — the plan decomposed work by test layer rather than behaviour slice, so
  implementation landed before acceptance tests existed.
- [Validator loops on evidence no agent can produce](../qubigo/docs/kaizen/2026-10-16-validator-loops-on-evidence-no-agent-can-produce.md)
  — validator kept demanding operator evidence the runner could not supply.

Then started the sizable change for **qubigo iteration 004 (competitions)**.
The seed is at `docs/iterations/004-competitions.md` and the implementation
evidence at `docs/iterations/004-competitions-implementation.md`. The
`careful-plan` assembly line was used with DeepSeek for doer/planner/plan_revision
and Claude/Gemini for reviewers, though several machines still used the default
OpenAI Codex.

## 2026-10-17–18 — Qubigo-004 run review and reconsideration

The detailed [run notes](runs/qubigo-004/session-notes.md) now live with the
[illustrated design review](runs/qubigo-004/design-review.md) in the
[qubigo-004 run folder](runs/qubigo-004/README.md). The historical notes were
moved intact apart from relative-link updates.

Tasks 1–6 are recorded complete; task 7 remains unaccepted after repeated review
findings and a final doer response without JSON. After reading the code and plan,
I find the design overly complicated and want to reconsider how implementation
and design are guided before choosing whether to start again.

[Today's checklist, 18 October](runs/qubigo-004/2026-10-18-todo.md) records the
proposed preservation, five-whys analysis, factory correction and possible 004a
experiment, with decision points to avoid repeating the same approach.
