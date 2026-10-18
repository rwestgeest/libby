Independently review the entire unfinished plan at the designated plan path against the seed and the actual code. Read .assembly-lines/lenses/responsibility-driven-design.md. Check completeness, task order, feasible implementation, explicit acceptance criteria, meaningful tests, clear object responsibilities and collaborations, and unnecessary abstractions. Review the plan, not the supplied product diff. Change neither the plan nor product files. Be satisfied only when there are no concrete blocking findings. Explain each blocking finding with a specific plan location and actionable revision. Do not invent requirements or demand speculative design. Return a JSON object with boolean satisfied and array findings. Before answering, write that same JSON to the exact plan path with .plan_reviewer_1.json appended. Overwrite only your own report. Include a plan_sha256 field containing the SHA-256 of the exact plan bytes reviewed. End with that same JSON on one line.

Your validation lens is:
Seed coverage, feasibility, testability and responsibility-driven object design

Check evidence feasibility for every acceptance criterion: the plan must identify
agent-verifiable checks and human-attested gates, their required evidence and
suppliers. Do not reject a plan merely for retaining a clearly identified human
gate. If review itself requires unavailable external input, return `"blocked": true`
with `"satisfied": false` and findings naming the missing input and supplier; write
the same result and plan hash to your report as usual. Never relax the seed or
accept an evidence request as completed verification.

## Added guarantees and context boundaries

Read `<exact plan path>.discussion.md` and compare the first unfinished capability
with its recorded owner decisions. A new guarantee beyond the seed or a changed
cross-context dependency needing an owner decision is a discussion question,
not authority to impose more machinery. Report blocked true and satisfied false,
with findings naming the owner, the exact discussion path, proposed choice,
reason and cost. Write your own sidecar as usual; do not edit the shared discussion
file from parallel review. The interactive session brings your question into it.
Do not reject a deliberately limited increment for deferring later planned
behavior, or re-demand a guarantee the owner explicitly deferred. Ordinary defects
against seed behavior or agreed decisions remain non-blocked review findings.

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

## Capability scope and test-driven design

Judge the current capability against its behavioral goal and recorded scope.
A brief outline for later capabilities is intentional; do not demand future
implementation detail or promote deferred behavior into this increment.
Allow collaborators to emerge through TDD within the ADRs. For implemented
behavior, examine the recorded failing-test → change → refactor sequence as
well as final results; do not infer TDD from a passing suite alone. Report an
actual evidence gap honestly without inventing a past red run or demanding
unrelated test machinery. Plan reviewers assess whether test-first work is
feasible, not whether a not-yet-implemented task already has execution evidence.
