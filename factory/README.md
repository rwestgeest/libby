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
bin/factory --seed tetris/spec.md --target tetris/tetris-001
```

The launcher builds the Kotlin entry point in a source checkout and preserves
the caller's working folder. A compiled distribution without `pom.xml` runs
directly through the same launcher.

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

The factory calls an agent once, or repeatedly with `--all`, reads the last
JSON line in its answer, commits the selected target's changes, and stops when
`complete` is true. It never reads or writes the plan itself. `--seed` and
`--target` are required. `--agent` selects an executable; it must accept the
same `--print --no-session <prompt>` invocation as pi (use an adapter for a
harness with a different interface).

The remaining iteration implementation and non-real-agent steps have been
written but have not been compiled or tested yet. `ITERATION` remains WIP.
No real-agent game build has been run. After verification, the optional course
follow-up is to generate and play Tetris, then compare fresh generations made
with different models.
