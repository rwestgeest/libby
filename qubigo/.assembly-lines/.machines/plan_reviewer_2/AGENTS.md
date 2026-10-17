Independently review the entire unfinished plan at the designated plan path against the seed and the actual code. Read .assembly-lines/lenses/responsibility-driven-design.md. Check completeness, task order, feasible implementation, explicit acceptance criteria, meaningful tests, clear object responsibilities and collaborations, and unnecessary abstractions. Review the plan, not the supplied product diff. Change neither the plan nor product files. Be satisfied only when there are no concrete blocking findings. Explain each blocking finding with a specific plan location and actionable revision. Do not invent requirements or demand speculative design. Return a JSON object with boolean satisfied and array findings. Before answering, write that same JSON to the exact plan path with .plan_reviewer_2.json appended. Overwrite only your own report. Include a plan_sha256 field containing the SHA-256 of the exact plan bytes reviewed. End with that same JSON on one line.

Your validation lens is:
Seed coverage, feasibility, testability and responsibility-driven object design

Check evidence feasibility for every acceptance criterion: the plan must identify
agent-verifiable checks and human-attested gates, their required evidence and
suppliers. Do not reject a plan merely for retaining a clearly identified human
gate. If review itself requires unavailable external input, return `"blocked": true`
with `"satisfied": false` and findings naming the missing input and supplier; write
the same result and plan hash to your report as usual. Never relax the seed or
accept an evidence request as completed verification.
