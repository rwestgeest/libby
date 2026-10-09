# Factory refactoring plan

Seed: `spec/refactoring-seeed.md`
Design lens: `validator-lens.md` (responsibility-driven object design).

## Scope and constraints

Refactor the existing Kotlin factory without changing its observable behavior. Keep work in this target; do not edit `spec/`, implement new homework requirements, or create commits manually. The factory owns commits and task completion. Preserve CLI behavior, diagnostics, routing, prompt contents, result parsing, retry limits, and target-scoped Git operations, including their ordering. The factory must continue treating plans as agent-owned opaque files.

Use cohesive collaborators and intention-revealing messages, not a class per function or speculative interfaces. Retain useful Kotlin functions. Each task includes focused tests and the non-real-agent suite (`./test`); distinguish pre-existing failures from regressions. Run real-agent examples only explicitly.

## Tasks

- [x] Establish behavior-preserving refactoring coverage.
  - [x] Run `./test` and record the baseline, including any existing failures or undefined scenarios. Recorded in `src/test/REFACTORING-BASELINE.md`: 45 undefined scenarios, no failing assertions.
  - [x] Examine existing step definitions and doubles before adding coverage. Add focused characterization tests only where missing for last-JSON-line result selection, labeled routing, retry exhaustion, accepted-task commit ordering, and target-only diff/commit behavior. Seven characterization scenarios pass (97 steps).
  - [x] Preserve existing behavior rather than implementing currently unsupported spec requirements. Do not modify fetched feature files. Production source and fetched specs are unchanged; `./test` retains the same 45 undefined scenarios with seven additional passing scenarios.
  - Acceptance: the baseline is documented and new characterization tests pass against the existing implementation.

- [ ] Give target repository operations a cohesive owner.
  - Extract the nested Git/process helpers, initialization, product-diff collection, and commit operation from `src/main/kotlin/Main.kt` into a target-bound collaborator.
  - Expose domain operations such as preparing the repository, collecting uncommitted product changes, and recording task changes; keep raw command results and path exclusions internal.
  - Preserve inclusion of untracked files, exclusion of `.factory` from validator diffs, containing-repository behavior, unrelated staged work, error handling, and commit messages.
  - Acceptance: focused repository tests and the baseline suite show no new failures; orchestration no longer builds Git commands.

- [ ] Separate machine invocation and job construction from orchestration.
  - Move configuration loading, harness selection, process invocation, and result decoding into focused collaborators with narrow protocols. Keep dependencies injectable only where useful for isolated tests.
  - Encapsulate shared run context and role-specific job construction, retaining configurable validator lenses, dynamic routing-field requests, model forwarding, inherited stderr, and exact failure behavior.
  - Keep role-specific prompt selection simple; do not introduce an extensible role hierarchy merely to replace a small `when` expression.
  - Acceptance: focused tests cover configuration defaults, invocation failures, prompt context and last-JSON-result behavior; the orchestration loop no longer owns process/configuration/JSON parsing details; the baseline suite has no regressions.

- [ ] Make assembly-line responsibilities explicit.
  - Separate file parsing and factory-directory machine availability checks from graph routing/structural validation where this clarifies ownership.
  - Keep edges private and expose intention-revealing operations for selecting the next machine and requesting its result fields. Avoid exposing the machine set solely so other objects can make graph decisions.
  - Preserve permissive line parsing, validation order and diagnostics, finish reachability, label matching, and handling of absent/non-boolean result fields.
  - Acceptance: focused graph tests exercise current routing and refusal behavior; parsing, environment checks, and graph decisions have clear owners without speculative abstractions; the baseline suite has no regressions.

- [ ] Encapsulate task lifecycle and leave a small orchestration entry point.
  - Replace the loop's dispersed `taskInProgress`, `taskAccepted`, attempt-count, and findings decisions with cohesive lifecycle behavior where it reduces coupling.
  - Express beginning an attempt, receiving validator findings, accepting work, and resetting after planner execution through intention-revealing operations; keep assembly-line routing authoritative, including custom machines and validator-free lines.
  - Keep `main` responsible for wiring and CLI exit behavior, with orchestration collaborating with the graph, machine runner/job builder, and target repository rather than accessing their internal state.
  - Preserve commit-before-accepted-planner and commit-after-planner ordering, retry counting/reset, planner prompts, final commit, and stop output.
  - Review changed code against `validator-lens.md`, remove unnecessary abstractions, and document the resulting responsibility boundaries briefly if useful.
  - Acceptance: lifecycle tests and `./test` show no new failures compared with the baseline; public command behavior is unchanged and the entry point no longer mixes domain state transitions with filesystem/process details.
