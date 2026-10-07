# FactorySteps.kt — responsibility-driven refactoring

Based on [validator-lens.md](./validator-lens.md) review: extract narrow collaborators from the
God-step class so each concern has a clear home.

## Tasks

- [x] **Extract `runProcess` helper** — `git()` and `runOnePass` share the same
  `ProcessBuilder` → `.directory()` → `.redirectErrorStream()` → `.start()` →
  read output → `.waitFor()` skeleton. Pull it into a `runProcess(command, dir)`
  that returns `(output, exitCode)`. Both callers delegate to it.

- [x] **Extract `Plan` value object** — The `- [ ] taskname` format is written
  in `planWithThreeTasks` and parsed by hand in `planHasExactlyTwoTasks`.
  Create a `Plan(path)` that owns the format: `tasks()` returns the list,
  `write(tasks)` writes the file. The two steps become one-liners.

- [x] **Replace `chooseAgent` boolean with `agentArgs` list** — The flag drives a
  conditional inside `runOnePass`. Change to a `lateinit var agentArgs: List<String>`
  that each `@Given` step sets directly: `noHarnessChosen()` → `listOf()`,
  `prepareAgent()` → `listOf("--agent", agent.toString())`. The conditional disappears
  from `runOnePass`.

- [x] **Extract `setupTestEnvironment` from `runOnePass`** — The `bin/` directory
  creation, agent-copy-as-`pi`, and `PATH` manipulation are environment setup, not
  execution. Pull them into a named helper so `runOnePass` only builds the command,
  runs it, and captures output.

- [x] **Extract `GitRepo` collaborator** — The `git()` helper returns raw strings;
  callers must know `rev-parse`, `log --format=%H`, `diff-tree --no-commit-id`
  flags. Replace with a `GitRepo(workspace)` object that offers named queries:
  `head()`, `logSince(commit, path)`, `diffTree(commit)`, and setup methods
  `init()`, `commit(msg)`. The existing `git()` method becomes a private
  delegator (or is absorbed into `GitRepo`).

## Dependencies

- All tasks touch only `factory/src/test/kotlin/steps/FactorySteps.kt`.
- `runProcess` must be extracted first (both `git()` and `runOnePass` depend on it).
- `GitRepo` uses `runProcess`.
- `Plan` is independent of the others.
- `setupTestEnvironment` and `agentArgs` change only `runOnePass` and its callers.