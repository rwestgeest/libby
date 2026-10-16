# Problem: java 25 harness breaks the build; reproduction steps embed machine-local paths

Date: 2026-10-16

## Context
Iteration 003 task 1 onward. Evidence: `docs/iterations/003-session-persistence-investigation.md` ("Baseline and commands"), tasks 2-6, `docs/iterations/session-acceptance/browser.cjs`.

## Expected standard
`./gradlew test` and `./gradlew build` run in the agent environment. Reproduction instructions in committed evidence work on another machine.

## What happened
- Under the harness's Temurin 25.0.4, `./gradlew test` and `./gradlew build` both exit 1 with `What went wrong: 25.0.4`. The backend targets Java 21.
- The doer downloaded a disposable JDK 21 to `/tmp/qubigo-003-jdk` from Adoptium. Later tasks reuse that path.
- Committed evidence includes machine-local paths: `JAVA_HOME=/tmp/qubigo-003-jdk`, `PLAYWRIGHT_MODULE=/home/rob/.npm/_npx/e41f203b7505f1fb/node_modules/playwright`, `LD_LIBRARY_PATH=/tmp/qubigo-002-browser-libs/usr/lib64`, `/tmp/qubigo-003-browser-classpath`. These paths depend on temp files and caches from earlier runs (including iteration 002).
- Task 5 notes "Replace local tool/cache paths on another machine".

## Impact
- Task 1 spent effort on a download and workaround before any real work.
- The acceptance commands are not reproducible by someone else without reverse-engineering the setup.
- The browser acceptance depends on libraries left over from a previous iteration; a clean `/tmp` would break it.

## What allowed it to happen
- The machine environment has Java 25 as default while the project requires 21; no machine config or preflight pins the JDK.
- The backend docs say Java 17, the build targets 21 (investigation file), so even the written standard is wrong.
- No convention for what is a repository-level reproducible command versus a local-only note.

## Observations
- `~/.sdkman/candidates/java/current/bin` is on PATH, so SDKMAN may already hold a Java 21.
- The doer correctly stated "use an installed Java 21 JDK, not the temporary path's existence".

## Why this matters
Every iteration pays the JDK cost again, and evidence that cannot be rerun is weaker than it looks.

## Open questions
Is a Java 21 JDK installed through SDKMAN? Could the line set `JAVA_HOME` per machine?

## Possible prevention ideas
Set `JAVA_HOME` in the doer and validator environments; add a `.sdkmanrc` or toolchain declaration; keep a single `docs/iterations/session-acceptance/README` with parameterised paths.
