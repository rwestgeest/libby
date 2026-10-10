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
