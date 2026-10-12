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
under the factory's `runs/<name>/`. The authoritative implementation plan is
separate from the seed, at `<target>/.factory/runs/<name>/plan.md`. Every machine
uses that same path, including retries and name-only resumption. Plans for runs
sharing a target remain distinct; `.factory/plan.md` is not used.

Before planning, the factory prepares only the authoritative plan's target-local
parent directories. The planner must create that exact file before routing can
continue, even if it reports completion. Missing plans or planner failures stop
execution; a doer cannot run without the authoritative plan, even on a route
that bypasses planning. No fallback plan is used. Retries and resumption instruct
the planner to read existing progress rather than regenerate it from the seed.

Run settings record this ownership contract. Legacy settings without it, or a
factory-owned `runs/<name>/plan.md`, are refused explicitly, without migration
or replacement of recorded progress. Plan path components cannot be symlinks
(including aliases within the target), and the plan cannot alias the seed.
Run names allow only letters, digits, dots, underscores and hyphens, starting
with a letter or digit.

Targets own their routes and machine configuration. Assembly lines are
`.assembly-lines/<name>.dot`, and machine configuration is under
`.assembly-lines/.machines/<machine>/config.json`. Product commits exclude
`.assembly-lines` and `.factory` and preserve unrelated repository changes.
After successful planning, a plan-only commit records the authoritative document
before implementation starts. At an accepted task boundary (routing back to the
planner), product work is committed first, followed by selected evidence; the
planner then updates completion status, and a run-labelled plan-only commit records it before further work or
completion. Commit failures stop execution. Rejected retries do not record work
or completed status; an arbitrary route to finish does not commit pending work.
Only this run's exact plan is included in plan commits, not other runs, private
metadata or assembly-line configuration.

Select evidence explicitly in `<target>/.factory/runs/<name>/evidence.list`, one
run-relative path per line (spaces are literal; blank lines are ignored). Keep
concise evidence beside the plan and reference it from the plan. Only listed
existing regular files and the manifest are included in run-labelled evidence
commits; unlisted files stay untouched, even if already staged. Selection cannot
include the plan, the manifest itself, symlinks, absolute/parent paths or
assembly-line configuration. Selected evidence and the exact plan are force-added
to survive `.gitignore`; product staging still respects ignore rules. The factory
does not parse plan contents to discover evidence. Preserve the manifest on
resumption; no manifest means no selected-evidence commit. Evidence created by
the planner is recorded before its plan commit too. Invalid selections stop
execution before task recording. Accepted implementation, evidence and progress
commits carry a `Factory-Run` trailer; evidence and progress identify the exact
implementation SHA in `Factory-Implementation`. Planner-created evidence uses
the same implementation link. The ordered commits are recorded before advancing
or reporting completion. Unchanged categories do not create empty commits;
initial planning and completed resumption do not invent implementation links.

Every target must provide an executable `./test` adapter that runs its complete
required suite and returns nonzero on any failure (including pre-existing ones).
The factory independently executes it in the target before accepted-work product
and evidence commits, then again after planner edits before evidence/progress
commits or advancement. Completed planning and name-only completed resumption
also require a passing suite before completion is printed. A missing/unexecutable
adapter blocks acceptance; validator satisfaction and recorded baseline failures
provide no waiver. This is not a red-phase gate: doers can revise authorized tests
and implement before it runs. Other manual acceptance requirements still apply.
Initial incomplete planning remains outside this gate, pending scope clarification.
Failure stops execution without committing changes at the blocked boundary;
already accepted earlier boundaries are not rolled back.

Validators review product diffs and
must not treat plan status as acceptance evidence. Optional
`--model` is forwarded to every machine harness.

Acceptance is also target-owned. The planner records required manual checks and
acceptance evidence in the plan or target-local documents referenced by it.
Completed automated tasks do not satisfy unavailable or unperformed manual
checks. On name-only resumption the planner reads the existing record, including
owner-supplied evidence, without regenerating progress or requiring factory-side
acceptance edits. Owners can commit evidence and update the acceptance record in
the target, then resume the run. The planner, not factory checkbox parsing or an
acceptance schema, decides whether the seed's required acceptance is supported.
Assembly-line routing remains target-defined: operators must provide a stopping
or handoff route for outstanding manual checks rather than assuming a doer can
perform them. Reaching a line's finish node alone is not proof of acceptance.
`src/test/features/target-acceptance.feature` exercises this contract with a
synthetic owner/check fixture; it makes no real human or device acceptance claim.

For reproducible lifecycle checks, evidence mapped to all eight iteration 0041
criteria, and the explicit fetched-spec ownership conflict, see
[the target-owned plan validation record](docs/target-owned-plan-validation.md).
That record distinguishes deterministic evidence from unperformed live-agent,
human and device checks; it does not claim a green full suite.
