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

