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

- [x] Give target repository operations a cohesive owner.
  - [x] Extracted `TargetRepository` with `prepare`, `uncommittedProductChanges`, and `recordTaskChanges`; raw Git commands/results and exclusions remain private. Orchestration delegates without constructing Git commands; existing command arguments, order, diagnostics, and commit messages are unchanged.
  - [x] Added two focused repository scenarios for repeatable standalone preparation, containing-repository reuse, staged and untracked products (including spaced filenames), metadata/ignored-file exclusions, target-only commits, clean-target no-op, and preservation of unrelated staged/unstaged changes. Focused run: 9 scenarios / 117 steps passed including characterization coverage. `./test`: 9 passed, the same 45 baseline undefined scenarios, no failing assertions (exit 1 due to undefined steps). No real-agent examples run.
  - Extract the nested Git/process helpers, initialization, product-diff collection, and commit operation from `src/main/kotlin/Main.kt` into a target-bound collaborator.
  - Expose domain operations such as preparing the repository, collecting uncommitted product changes, and recording task changes; keep raw command results and path exclusions internal.
  - Preserve inclusion of untracked files, exclusion of `.factory` from validator diffs, containing-repository behavior, unrelated staged work, error handling, and commit messages.
  - Acceptance: focused repository tests and the baseline suite show no new failures; orchestration no longer builds Git commands.

- [x] Separate machine invocation and job construction from orchestration.
  - [x] Extracted on-demand `MachineConfiguration` for harness/lens defaults and configuration diagnostics, `MachineRunner` for target-bound process execution/model forwarding/inherited stderr/last-JSON decoding, and `MachineJobs` for shared context and simple role-specific prompts. Orchestration sends a complete prompt and receives a JSON object; validator lens lookup still precedes product-diff collection and harness lookup.
  - [x] Added nine focused invocation scenarios for absent/null configuration, malformed/non-object configuration, default/custom lenses, shared prompt context, model forwarding, startup failure and nonzero exit with stdout/inherited stderr. Existing characterization tests retain routing-field requests, last-JSON selection, retry behavior and commit ordering. Focused run: 16 scenarios / 220 steps passed.
  - [x] Ran `./test`: 18 passed, the same 45 baseline undefined scenarios, no failing assertions (exit 1 for undefined steps). Repository scenarios also pass. No fetched specs changed, no real-agent examples run, and no previous validator findings were supplied.
  - Move configuration loading, harness selection, process invocation, and result decoding into focused collaborators with narrow protocols. Keep dependencies injectable only where useful for isolated tests.
  - Encapsulate shared run context and role-specific job construction, retaining configurable validator lenses, dynamic routing-field requests, model forwarding, inherited stderr, and exact failure behavior.
  - Keep role-specific prompt selection simple; do not introduce an extensible role hierarchy merely to replace a small `when` expression.
  - Acceptance: focused tests cover configuration defaults, invocation failures, prompt context and last-JSON-result behavior; the orchestration loop no longer owns process/configuration/JSON parsing details; the baseline suite has no regressions.

- [ ] Make assembly-line responsibilities explicit.
  - [x] Moved loading into `readAssemblyLine` in `src/main/kotlin/AssemblyLine.kt`: permissive file parsing and factory-directory availability checks remain outside graph decisions. `AssemblyLine` owns structural validation, private edges/machines, next-machine selection and result-field requests; validation invokes availability checks in the original diagnostic order.
  - [x] Added 16 focused graph scenarios for permissive parsing/blank labels, structural diagnostic order, first unavailable/last unreachable machines, finish reachability, non-boolean fields, unmatched labels, unconditional-edge precedence and first matching labeled edges. Focused graph/characterization/invocation run: 32 scenarios / 407 steps passed.
  - [x] Ran `./test`: 34 passed, the same 45 baseline undefined scenarios, no failing assertions (exit 1 due to undefined steps). Repository coverage is included. No real-agent examples or commits; no validator findings were supplied. Requested `spec/refactoring-seed.md` was missing, so the plan's explicit preservation constraints guided this task; fetched specs were not edited.
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
