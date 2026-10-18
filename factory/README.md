# Kotlin factory

Requires JDK 21 and Maven. From this folder:

```sh
./test                  # excludes @real-agent; concise results
./test -Preal-agent      # real-agent examples only
```

`./test` shows Cucumber results and undefined step expressions, with the counts
at the bottom. Generated code templates stay in the log. It saves full Maven output in
`target/test.log`. Failed or undefined examples still return a nonzero exit code.
Build errors are displayed if Cucumber cannot run. Use `mvn test` for raw output.

From the repository root:

```sh
bin/factory --run tetris --target tetris/tetris-005 --line careful --seed tetris/spec.md
bin/factory --run tetris
```

The launcher builds the Kotlin entry point in a source checkout and preserves
the caller's working folder. A compiled distribution without `pom.xml` runs
directly through the same launcher.

## Create a distribution

From the repository root, build a ZIP containing the compiled factory and its
runtime dependencies:

```sh
bin/package-factory
```

This creates `dist/factory.zip`. Pass a different ZIP path as the first
argument to choose another destination. The archive excludes source, tests,
homework specs, build logs, and local run/job state. Its recipient needs Bash,
JDK 21, Git, and the configured agent harnesses, but does not need Maven.

The suite compiles the factory once before running examples. Each example gets
its own copy of `target/classes`, the runtime classpath file, and the launcher.
No factory classes are symlinked or shared between examples. Only dependency
JARs in Maven's cache are shared, as they were before this optimization.
Each example starts a separate JVM and owns its temporary Git repository,
seed, target, plan, agent double and call records. Cleanup removes that workspace.
Specs and step definitions stay out of the factory copy.
Test doubles live with the steps and are selected from outside the factory.
They record calls and inputs in the example's workspace, which is removed
after the example. A fake `pi` on PATH will check default agent selection.

Build configuration references: [Kotlin with Maven](https://kotlinlang.org/docs/maven.html)
and [Cucumber CLI](https://cucumber.io/docs/cucumber/api/).

## Current iteration behavior

The first invocation of a named run supplies its target, seed, and assembly
line. Later invocations need only `--run`; settings are immutable and persist
under `runs/<name>/`. Each run keeps its own `plan.md` there.

Targets own their routes and machine configuration. Assembly lines are
`.assembly-lines/<name>.dot`, and machine configuration is under
`.assembly-lines/.machines/<machine>/config.json`. Product commits exclude
`.assembly-lines` and preserve unrelated repository changes. Optional
`--model` is forwarded to every machine harness.

## Watching runs (homework 006)

Runs start in the background. The command returns after starting a run; attach
to its live output or saved record with `bin/factory watch <name>`. A record
in `runs/<name>/record.jsonl` keeps each machine's generated text and token
usage. `bin/factory stop <name>` stops that run and leaves the factory
available for another one; `bin/factory stop` stops the active run and the
factory. Start a stopped run again with `bin/factory --run <name>`.

Machines use ACP bridges. The default harness is `pi-rpc-acp`, which runs pi
in RPC mode. Set `harness` in a machine's `config.json` to `claude-agent-acp`
or `codex-acp` to use those adapters. Each machine keeps its harness and model
in its own configuration folder; the factory records usage under the model's
provider and for the whole run.

## Parallel review (homework 005)

Several unlabelled edges leaving a machine run their destinations concurrently.
Each branch must have exactly one unlabelled edge to the same meeting machine:

```dot
doer -> reviewer_1
doer -> reviewer_2
doer -> reviewer_3
reviewer_1 -> synthesiser
reviewer_2 -> synthesiser
reviewer_3 -> synthesiser
synthesiser -> doer [label="not satisfied"]
synthesiser -> planner [label="satisfied"]
```

Use this in place of the validator edges in a target's line; keep the existing
start and planner edges. Each reviewer and the synthesiser needs its own
folder under `.assembly-lines/.machines/`.

A machine's optional `prompt` configuration supplies its job. Branches can
have different jobs. For the three big brains, give all reviewers the same
configuration apart from `model`, choosing models from three providers you
have signed into in Pi. For example (replace `provider/model-id`):

```json
{
  "harness": "pi-rpc-acp",
  "model": "provider/model-id",
  "lens": "testability",
  "prompt": "Review only the supplied product changes through the configured lens. Change neither the plan nor the product. Return a JSON result with a boolean satisfied and an array findings."
}
```

A machine's `model` overrides the CLI's shared `--model`. Reviewer prompts
include current uncommitted product changes, excluding completed work.
The meeting machine receives only the branches' JSON results, keyed by machine
name, together with its job and requested result fields. Its prompt can be:

```json
{
  "harness": "pi-rpc-acp",
  "prompt": "Assess the reviewers' reports and decide whether to accept this attempt. Return a boolean satisfied and an array findings. Use only the reports; do not inspect or change files."
}
```

Only the synthesiser's result routes to the doer or planner. Its findings go
to the doer's retry, and retries obey `--max-attempts`. A branch that crashes,
cannot start, or returns no JSON result stops the other branches and the run,
without running the synthesiser or committing that attempt.

With that line saved as `three-big-brains.dot` in the target, try from the
repository root:

```sh
bin/factory --run tetris-brains --target tetris/tetris-005 --line three-big-brains --seed tetris/spec.md
bin/factory --run tetris-brains
```

These commands call real agents. To exercise the course's real-agent examples
explicitly, select them with `./test -Preal-agent` from `factory/`. That profile
still has undefined real-agent steps from earlier iterations; real Pi behavior
can be tried manually through the CLI after configuring the machines above.
The default suite uses
external doubles, including FIFO barriers that prove concurrent execution and
keep sibling reviewers at work until the factory stops them on a failure.

Any machine may return `"blocked": true` with `findings` naming missing external
input, access or human attestation and who can supply it. The run stops with a
`blocked` status before routing, retrying or committing that task. Existing work
and unfinished plan entries remain for inspection. After supplying the input,
start the same named run again to use its existing plan. Fixable product defects
should use normal rejection and retry findings instead. Omit `blocked` (or use
`false`) for normal results.

### Missing doer result

When a pi doer finishes successfully but emits no JSON value, the runner makes
one format-only invocation with tools, extensions, MCP, skills and prompt
templates disabled. It supplies the original answer as data; it does not repeat
the implementation job. Both answers remain in the run record. The repaired
object must contain a findings array, preserves external-input blocking, and
still goes through normal independent validation before acceptance or commit.

Another unreadable answer stops the run. This repair does not consume another
implementation attempt. Explicit invalid final JSON values and unsuccessful
process exits are not repaired. Other harnesses retain their existing failure
behavior; this bounded recovery currently supports pi print and pi ACP only.
