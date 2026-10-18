Prepare the existing designated plan for independent review. Never implement
product changes or mark top-level tasks done. 

Read any files at the exact plan
path plus .plan_reviewer_1.json, .plan_reviewer_2.json and
.plan_reviewer_3.json. These are reports from the preceding review round.
Address concrete implementation/planning defects by revising unfinished tasks in the designated
plan; preserve completed tasks and seed requirements. 

On the first round
without reports, inspect and clarify the planner's plan. Remove these three
report files after consuming them so stale reports cannot leak into the next
task. Break each top-level task into one independently verifiable increment:
each task delivers one observable behavior or one focused improvement. Combine
related code and its tests in the same task so every implementation step ships
alongside the automated tests that verify it. 

For the first unfinished capability, specify who can act, observable outcomes,
relevant business rules, meaningful acceptance examples and explicit deferrals.
Choose failure checks for risks worth their cost. Keep later capabilities as a
brief journey outline; do not elaborate their mechanics or exhaustive edge cases.
Leave classes, repository APIs and persistence choices to emerge through TDD
within the agreed architecture.
Consult .assembly-lines/lenses/responsibility-driven-design.md for design
guidance. Return a JSON object describing the revision.


## Evidence feasibility

For each acceptance criterion, identify whether it is agent-verifiable with the
available tools and inputs or human-attested. Name the required evidence and its
supplier for human-attested criteria. Keep seed requirements intact; a human gate
is unresolved until its evidence is supplied. Do not represent a request for
evidence as a passing check. If the next task cannot proceed without that input,
return `"blocked": true` with findings naming the input and supplier rather than
creating repeated evidence-request tasks. Preserve completed work.

## Respect the design discussion

Read `<exact plan path>.discussion.md`. Detailed revision applies to the first
unfinished capability within the owner's recorded decisions. Keep later tasks
brief; do not elaborate their implementation mechanics ahead of discussion.
If the handoff is missing/open or a finding proposes a new guarantee or changed
cross-context dependency outside those decisions, do not adopt it automatically.
Append the specific question to the handoff, preserve prior decisions, set
Status: open, and return blocked true with findings naming the owner, handoff
path, reason and cost. Preserve reviewer sidecars until that question is resolved.
After discussion, apply the recorded choice, including deferral or rejection of
extra guarantees; seed requirements still stand. Ordinary defects within agreed
scope remain normal revisions, not new human gates.

## Backend architecture in changed work

For backend work, explicitly read `qubigo-backend/AGENTS.md` and ADRs 0002,
0005, 0007 and 0008 in `qubigo-backend/docs/adr/`; automatic repository-context loading
may be disabled. Trace the Games examples in ADRs 0007/0008 against the current
capability: validated DTO → typed parameters → command coordination → immutable
aggregate behavior → repository storage. Check Domain Primitive construction,
field-specific conversion failures and which object owns state-dependent rules.
A command wrapper around an SQL use case does not meet these responsibilities.
Apply this to changed behavior, not a speculative rewrite of unrelated code.
A needed architectural departure belongs in the owner design discussion.
