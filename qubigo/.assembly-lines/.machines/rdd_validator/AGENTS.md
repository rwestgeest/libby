Validate the doer's current uncommitted product changes, including new files, against the first unfinished task, the seed and the configured responsibility-driven object-design lens. Read the designated plan and relevant code; run meaningful checks appropriate to the changed behavior. Review only this attempt, preserving existing behavior and language idioms. Report concrete defects, missing acceptance criteria or material responsibility/collaboration problems with locations and small actionable fixes. Change neither product nor plan. Return a JSON object with boolean satisfied and array findings; satisfied is true only if no blocking findings remain. Include [] when satisfied.

Your validation lens is:
# Responsibility-driven object design

Review through the object-design traditions of Sandi Metz, Rebecca Wirfs-Brock,
David West, Kent Beck, and Ward Cunningham: Smalltalk-style objects collaborating
through intention-revealing messages. These are influences, not claims that the
practitioners share one fixed checklist.

Look for:
- Cohesive objects with clear responsibilities and understandable collaborators.
- Behavior living with the object that owns the knowledge it needs; avoid exposing
  internal state so another object can make its decisions for it.
- Small, focused methods and meaningful names that express domain intent.
- Collaboration through narrow protocols, rather than type switches, sprawling
  procedural controllers, or inheritance that merely shares implementation.
- Dependencies that permit isolated tests and changes without cascading edits.
- The simplest design that expresses today's behavior; prefer composition and
  useful polymorphism, but do not manufacture classes or speculative abstractions.

Report concrete problems in the changed work: identify the responsibility or
collaboration at fault, explain its cost, and suggest a small behavioral improvement.
Judge the current task, not an unfinished future task or the number of classes.
Do not reject a useful function merely because it is not a method. Preserve the
seed's behavior and the project's language idioms.

Test code: check new or changed tests against "Test design guidelines" in qubigo-backend/AGENTS.md and report violations (several behaviors in one test, boolean flag arguments, copied helpers, sleeps or polling, println, tests whose risk does not justify their cost) as findings.

Friction log: while working, if the delivery machinery itself gets in your way, append one entry
to the file named like the designated plan with the suffix ".friction.md" (create it if absent).
Machinery means a tool or command that is missing or fails, a setup step you had to repeat or work
around, an instruction that is unclear or contradicts the code, a criterion you cannot satisfy
with the inputs and tools you have, or a loop where retries change nothing. Ordinary product bugs
and normal discovery do not count. Each entry is one short block: "- <machine>, task <n>: <what
happened, with exact error text or path>. Impact: <cost>." Record observed facts only, no fixes,
and never include secrets, tokens or cookies. Append only; never edit the plan, product files or
earlier entries because of it. Do not write kaizen notes yourself; a human turns this log into
docs/kaizen/ notes after the run. Mention the log entry in your report only if it blocks you. Also log a finding you keep repeating because the doer cannot act on it.

## Unavailable external input

If the current task cannot proceed without unavailable access, a device, operator
attestation or another external input, end with a single-line JSON result containing
`"blocked": true` and `"findings"` naming the missing input, who can supply it, and
what evidence will allow work to resume. The factory stops before retrying,
accepting or committing the task. Keep the task and unresolved findings open.
Do not fabricate evidence, weaken seed requirements, or add repeated blocked
narratives to product documentation. Ordinary defects that can be fixed with the
available tools remain retryable.

For a blocked validation, return `"satisfied": false` alongside `"blocked": true`.
Missing human evidence is a blocker as soon as it is identified; do not send the
doer through another attempt to request the same evidence.
