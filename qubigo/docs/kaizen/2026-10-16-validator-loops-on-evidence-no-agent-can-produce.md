# Problem: validator loop on evidence the line cannot produce (iteration 003, criterion 6)

Date: 2026-10-16

## Context
Iteration 003 (`feature/003-sessions-survive-backend-deployments`), line `careful-plan`, run `qubigo-003`. Seed: `docs/iterations/003-sessions-survive-backend-deployments.md`. Tasks 1-4 were committed between 21:55 and 22:18 on 2026-10-15. Task 5 was then blocked until the seed was edited on 2026-10-16 (machine config commit 12:11, task 5 commit 12:50, task 6 commit 12:55).

## Expected standard
Each task reaches an acceptance state that the doer and validator can achieve with the tools and inputs the line provides. The line's description (`.assembly-lines/careful-plan.md`) says a rejection retries the doer with findings and does not mention human input.

## What happened
- The original criterion 6 required verification that the deployed database and signing key persist across releases. Only a human operator can supply that.
- The doer correctly refused to fabricate it. The validator rejected the result each time. `docs/iterations/003-session-persistence-task-5.md` accumulates sections titled "Operator evidence request - blocked", "Current validator retry - still blocked", "Current doer attempt - task 5a blocked", "Short operator confirmation - pending", "Latest validator finding - operator evidence absent" and "Validator retry - lines 312-340". Three validator findings plus the 5a-5c subtasks are recorded in the plan.
- Several attempts were explicitly "documentation-only" and changed nothing but prose and the plan.
- Resolution was a human edit: the seed's criterion 6 was relaxed on 2026-10-16 and the owner's one-line dev confirmation was accepted.

## Impact
- About 14 hours elapsed between the task 4 commit (22:18) and the task 5 commit (12:50); most of it was waiting on the human, not on work.
- Task 5 evidence grew to 413 lines, with roughly 135 lines of superseded retry narrative that readers must now skip.
- Several doer and validator cycles produced no product change.

## What allowed it to happen
- The seed mixed agent-checkable and human-only acceptance criteria without marking which was which.
- The three-reviewer plan review (hash `edb29300...`, all `satisfied: true`, `findings: []`) judged "feasible ordering" but did not ask whether each criterion was achievable inside the line.
- The line has no `blocked / needs human input` exit. The only edges out of the validator are back to the doer or on to the planner.

## Observations
- The doer behaved well: it declined to infer or fabricate operator attestation and said what input was missing.
- The validator's finding text was accurate each time; the loop was structural, not a validator error.
- The accepted evidence (a one-sentence owner confirmation) is much weaker than what the validator demanded, so the original requirement was probably over-specified.

## Why this matters
A human-gated criterion will recur in any iteration involving deployment, production, app stores or credentials. Without an explicit path, the cost is idle time and noisy evidence each time.

## Open questions
- Did the factory retry limit (`--max-attempts`) apply here, and how many validator rounds actually ran? Factory logs would answer this.
- Should human-only criteria be split into a separate "release prerequisite" list at interview time?

## Possible prevention ideas
- Interview step: tag each acceptance criterion as `agent-verifiable` or `human-attested`.
- Give the validator and doer a documented "blocked on human" result that stops the loop.
- Add "can this be achieved without a human?" to the plan reviewers' criteria.
