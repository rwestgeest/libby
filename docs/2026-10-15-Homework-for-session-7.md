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


## 2026-10-18 — First design walkthrough for qubigo-004a

Tried the new human design checkpoint on the fresh competition run, using the
[004a run brief](../qubigo/docs/iterations/004a-competitions-run-brief.md).
The factory stopped at `design_walkthrough` before detailed planning. We then
used the discuss-factory-design skill for an interview, one question at a time,
starting with ubiquitous language rather than implementation mechanics.
The [consolidated walkthrough](runs/qubigo-004a/design-walkthrough.md) records
the resulting decisions; the [interview history](runs/qubigo-004a/plan.md.discussion.interview-history.md)
preserves the initial proposals and accumulated notes.

The language interview exposed useful distinctions. A competition has
**participants**; a game has **players**. An **organizer** is a domain role,
permitted by existing admin authorization. **Games per participant** replaced
“game allowance”: the competition arranges games, rather than granting permission
to play. Playing fewer is fine, but gives fewer opportunities to score.
**Suggested minimum participants** makes the advisory nature explicit.
**Open for joining** is separate from **not started / running / closed**, since
participants may join before or during play.

The owner changed the first slice materially during the discussion:

- Create a complete **draft**, edit it, then explicitly **open for joining**.
  All four settings are editable in draft; after opening, only name and
  suggested minimum participants remain editable. Organizers share access;
  ownership rules and co-organizership can wait.
- Joining is available to already signed-in users through a shared QR link,
  including forwarded links. There is no ordinary listing of unjoined
  competitions for regular users. This replaces the seed's admin-only joining
  boundary. Competition-specific registration/sign-in return flows remain later work.
- Joining is explicit, even for organizers. Repeated joining keeps the existing
  participation. New joining is refused at closing time, even if play never started.
- **CompetitionName** is a domain primitive: trim surrounding spaces, reject an
  empty result, permit only ASCII letters, digits and spaces; duplicate names
  are allowed. Games per participant is a whole number ≥1; suggested minimum
  participants is a whole number ≥2. Opening requires a future closing time,
  entered in the device's local timezone with the timezone shown.

A useful correction during the interview: questions drifted into matching and
invitations, and the owner brought the discussion back to creating and joining.
We preserved that later learning without making it part of the first slice.
The competition is the inviter; only the participant selected to move first
receives the actionable invitation. Separate competition invitations may be
appropriate, but representation and timing still need later discussion.
Matching by wins or speed was an idea, not a new agreed algorithm.

Lessons: interviewing for language uncovered product behavior that an upfront
technical plan would have assumed incorrectly. Keep proposals distinct from
owner decisions, update the capability sketch when scope changes, and avoid
turning every possible reliability concern into a required guarantee. The
accumulated notes initially retained stale questions and contradictory proposals;
consolidation was necessary before handing them back to the factory.

The discussion is now marked resolved for this scoped capability, ready for
planning. This is a successful human walkthrough, not evidence of working
competition software or completion of iteration 004. No application code or
factory behavior changed, and no tests were run during the walkthrough.
The factory was not restarted by the agent. The owner will resume the existing
run from Libby's root with `bin/factory --run qubigo-004a`; a fresh run is not
needed. The saved `q` target is a symlink to the live Qubigo repository, not a
separate target. Next observe whether the resumed planner follows the recorded
decisions and returns for genuinely new scope rather than reopening settled ones.
