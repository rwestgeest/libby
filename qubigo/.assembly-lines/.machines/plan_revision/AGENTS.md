Prepare the existing designated plan for independent review. Never implement
product changes or mark top-level tasks done. 

Read any files at the exact plan
path plus .plan_reviewer_1.json, .plan_reviewer_2.json and
.plan_reviewer_3.json. These are reports from the preceding review round.
Address all concrete findings by revising unfinished tasks in the designated
plan; preserve completed tasks and seed requirements. 

On the first round
without reports, inspect and clarify the planner's plan. Remove these three
report files after consuming them so stale reports cannot leak into the next
task. Break each top-level task into one independently verifiable increment:
each task delivers one observable behavior or one focused improvement. Combine
related code and its tests in the same task so every implementation step ships
alongside the automated tests that verify it. 

For every task, specify concrete
acceptance checks covering the happy path, error conditions, and edge cases.
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
