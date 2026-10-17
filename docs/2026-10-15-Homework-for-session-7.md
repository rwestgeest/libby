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

## 2026-10-17 — Factory run issues on the qubigo-004 sizable change

Continued the qubigo-004 run and inspected its outputs. The run progressed
through tasks 1–5 (competition drafts, publication/invitations, enrollment,
competition context, manual start/matching) but stalled repeatedly:

### Stop causes

- **Codex usage-limit errors** hit the doer and plan_initializer three times
  (recorded in `daemon.log`). Machines without explicit model config still used
  `openai-codex/gpt-6-astra` by default, burning quota even though the main
  doer was switched to DeepSeek.
- **Unparseable plan_revision results** — the machine returned fenced,
  pretty-printed JSON. The factory's ACP reader accumulated streamed text but
  only parsed single lines, stopping with `Could not read the plan_revision's
  result`. Fixed by updating the factory result reader to accept fenced and
  multiline JSON. Factory suite passed: 136 scenarios, 1,966 steps.
- **Unparseable rdd_validator result** — the run ended with `Could not read
  the rdd_validator's result` (same root cause as plan_revision, presumably).
  Task 6 work was left uncommitted.

### Plan revision cycles

Reviewers ran 4 times, not once as designed. The crash/recovery re-triggered
the initialization path (`plan_initializer` → `plan_revision` → reviewers).
Reviewer 1 found multiple contradictions in tasks 6–7 (read-only linkage,
board-full semantics, transaction contract, move-notification ownership),
each requiring a revision round until all three reviewers were satisfied.

### Friction throughout the run

A detailed `plan.md.friction.md` was produced (see
`factory/runs/qubigo-004/plan.md.friction.md`). Notable items:

- **Java 25** — `./gradlew` fails with `What went wrong: 25.0.4`; must use
  existing documented `/tmp/qubigo-jdk21`.
- **Wrong directory** — validators launched commands from `qubigo/` root
  instead of `qubigo-backend/` or `qubigo-frontend/`, causing ENOENT errors.
- **Edit tool collisions** — repeated `Found N occurrences` rejections on
  batched edits (tasks 2, 3, 4, 5, 6), each needing a retry.
- **Test-report lookup** — result XML is under `build/reports/`, not
  `build/test-results/`.
- **Missing acceptance runner** — `browser.cjs` reference in iteration 003
  docs was deleted by the kaizen resolution, causing a rebuild of a disposable
  runner.
- **Spring devtools** — disposable acceptance runner restarted repeatedly due
  to devtools; had to disable them.
- **Local browser proxy** — security config only allowed port 9090; acceptance
  runner on 5196 returned 403 before reconfiguring.
- **Multiple edit retries** — tasks 3, 4, 5, 6 each needed at least one
  retried edit call because the replacement text was not unique.

### Validator findings on task 5

The rdd_validator found a test-design violation introduced by the doer:
`CompetitionMatchingTest` passed positional boolean flags to `ReservedPair`,
obscuring intent. The doer replaced those with `completedPair`/`pendingPair`
fixture helpers in a follow-up, and the validator accepted the fix.

### Cross-cutting kaizen from this run

All the 2026-10-16 kaizen notes (Java 25, gradlew directory, plan location,
commit messages, yx, tests-after-implementation, validator loops) were
initiated by reviewing iteration 003 and are stored in the qubigo snapshot
at `qubigo/docs/kaizen/`. Their resolutions vary: some were fixed (yx
installed, test guidelines added, scenario tests deleted), others are
still open (plan lifecycles, commit messages, JDK toolchain).

The run's `plan.md`, `record.jsonl` and `daemon.log` are in the ignored
local `factory/runs/qubigo-004/`.

## 2026-10-17 — Competition run and structured results

Continued iteration 004 on the live QubiGo target through the `careful-plan`
line. The saved `qubigo-004` plan has tasks 1–5 checked and task 6, invitation
acceptance and game creation, still open. The live target contains uncommitted
task 6 work; this session did not accept or commit it.

The run exposed two different stop causes. Its earlier output reported Codex
usage-limit errors for the doer and plan initializer, so those attempts exited
unsuccessfully. Later, `plan_revision` returned its result as a fenced,
pretty-printed JSON object. The factory's ACP reader accumulated streamed text
but only parsed individual lines, then stopped with `Could not read the
plan_revision's result`. The reported `complete: false` results were expected:
unfinished tasks remained in the plan. Restarting the named run starts again at
the assembly line's start node, which explains the repeated initialization and
review work; it does not resume at the machine that last failed.

Updated the factory result reader to accept fenced and multiline JSON while
preserving the rule that the last JSON value controls the result. Added an end
to end regression where agents return fenced, pretty-printed results. The
factory suite passed: 136 scenarios and 1,966 steps. The provider usage limit is
separate from parsing and may still stop a future agent attempt.

Relevant material: [iteration 004 seed](../qubigo/docs/iterations/004-competitions.md),
[implementation evidence](../qubigo/docs/iterations/004-competitions-implementation.md),
[careful-plan line](../qubigo/.assembly-lines/careful-plan.md),
[validator-loop kaizen note](../qubigo/docs/kaizen/2026-10-16-validator-loops-on-evidence-no-agent-can-produce.md).

After provider access is available, restart and inspect the saved run with
`bin/factory --run qubigo-004` and `bin/factory watch qubigo-004`. Then verify
whether task 6's existing changes satisfy the acceptance checks before
accepting the task; do not infer completion from the earlier plan reviews.

## 2026-10-17 — Provider usage analysis and reviewer cycle audit

Inspected the live `qubigo-004` run's `record.jsonl` and `daemon.log` to
understand why OpenAI Codex credits were still being consumed after the
model config changes that switched the doer and plan_revision to
DeepSeek.

### Config change effect

The following machines had explicit model overrides in
`.assembly-lines/.machines/`:

| Machine | Model | Burns OpenAI? |
|---------|-------|--------------|
| doer | `deepseek/deepseek-v4-flash` | No |
| plan_revision | `deepseek/deepseek-v4-flash` | No |
| planner | `deepseek/deepseek-v4-flash` | No |
| plan_reviewer_2 | `openrouter/anthropic/claude-opus-4.8` | No |
| plan_reviewer_3 | `openrouter/~google/gemini-pro-latest` | No |
| **plan_reviewer_1** | **`openai-codex/gpt-6-astra`** | **Yes** |
| plan_initializer | *(no config — uses harness default)* | Yes |
| plan_consensus | *(no config — uses harness default)* | Yes |
| validator/rdd_validator | *(no config — uses harness default)* | Yes |

### Reviewers ran 4 times, not once

The `careful-plan.md` line promises the three-reviewer brain runs only
during initial planning. In practice it ran four complete cycles:

1. **Initial plan** (normal) — all satisfied → doer tasks 1–5
2. **After usage-limit crash** — plan_initializer re-ran during recovery,
   plan got revised, reviewers ran again → reviewer_1 found 4 issues
3. **Revision round 2** — reviewers ran again → reviewer_1 still found 1
   issue
4. **Revision round 3** — all satisfied → doer for task 6

The extra cycles were caused by the crash/recovery re-triggering the
initialization path (`plan_initializer` → `plan_revision` → reviewers).

### Current pipeline status (as of inspection)

The doer completed task 6 (fixed `AcceptInvitationPage.open()` async race
condition). The `rdd_validator` was running. Going forward the flow is
`planner → doer → rdd_validator → planner` for each remaining task
(7–18) — the reviewers will **not** run again unless a plan revision cycle
is triggered.

### Still burning OpenAI credits

While the reviewers are done, these machines still use the harness default
(Codex/OpenAI) and run on every task:
- `rdd_validator` — runs after every doer task
- `plan_consensus` — runs during review cycles (done for now)
- `plan_initializer` — only if recovery triggers again

To fully stop OpenAI consumption, give them explicit models matching the
doer, e.g.:
```json
{"harness": "pi", "model": "deepseek/deepseek-v4-flash"}
```

### Useful commands
```
# Inspect machine configs
cat .assembly-lines/.machines/{machine}/config.json

# Count reviewer cycles from the record
python3 -c "
import json
with open('factory/runs/qubigo-004/record.jsonl') as f:
    for i, line in enumerate(f):
        rec = json.loads(line)
        if rec.get('type') == 'machine-state' and rec.get('running'):
            print(f'line {i:3d}: ▶ {rec[\"machine\"]}')
"
```

Open question: whether the Codex daemon's default model for unconfigured
machines can be set globally, or each machine needs an explicit model
override.

## 2026-10-17 — RDD validator omitted its JSON result

**Problem.** `qubigo-004` stopped during task 6 with
`Could not read the rdd_validator's result`. Tasks 1–5 remain accepted;
task 6 remains unchecked. The validator's recorded response was a prose
assessment ending with “No blocking findings”, without the JSON decision
the factory needs to route to the planner. This differs from the earlier
fenced/multiline JSON failures: there was no JSON for the improved parser
to extract. The stop does not establish a product defect or task acceptance.

**Cause.** The agent omitted a required response format, and the factory
depends on a textual instruction rather than enforced structured output.
The validator now uses Pi with `deepseek/deepseek-v4-flash`. That switch is
a possible trigger, but the evidence does not isolate the harness from
the model or prove why the instruction was missed. The current invocation
passes the machine instructions via `--append-system-prompt` and includes
the JSON requirement in the task prompt; the run does not capture the
complete effective model request. Prose approval must not be interpreted
as a machine-readable acceptance decision.

**Fix.** Added a final response contract to the live
[RDD validator instructions](../qubigo/.assembly-lines/.machines/rdd_validator/AGENTS.md#final-response-contract).
It requires a single-line JSON object as the final line, without Markdown
fences or trailing prose, and supplies examples for acceptance, fixable
defects and unavailable external input. Acceptance explicitly returns
`{"satisfied":true,"findings":[]}`. The validator must check the JSON syntax
and field types before sending. Refreshed the sharing snapshot with this
instruction change; `git diff --check` passed.

The fix has not yet been verified in a new agent invocation. No logs,
status or plan history were rewritten, and this change did not restart
the run. Next: restart with `bin/factory --run qubigo-004`, inspect with
`bin/factory watch qubigo-004`, and confirm a valid validator decision
before accepting task 6. Restart enters the assembly line at its start,
so planning/review work may repeat. If the format failure persists,
inspect the effective Pi prompt and compare with the previous working
harness/model configuration. A bounded response-repair step is a proposed
later factory improvement, not part of this instruction fix.

