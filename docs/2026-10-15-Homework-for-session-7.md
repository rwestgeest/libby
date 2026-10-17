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

