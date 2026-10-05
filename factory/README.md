# Kotlin factory

Requires JDK 21 and Maven. From this folder:

```sh
mvn test                 # excludes @real-agent
mvn test -Preal-agent    # real-agent examples only
```

From the repository root:

```sh
bin/factory --seed tetris/spec.md --target tetris/tetris-001
```

The launcher builds the Kotlin entry point and preserves the caller's working
folder. It currently prints `not built yet`.

Step definitions will live in `src/test/kotlin/steps/`. Each example will use a
copy of the factory code in its own temporary workspace, sharing dependencies
rather than copying them. Specs and step definitions stay out of that copy.
Test doubles live with the steps and are selected from outside the factory.
They record calls and inputs in the example's workspace, which is removed
after the example. A fake `pi` on PATH will check default agent selection.

Build configuration references: [Kotlin with Maven](https://kotlinlang.org/docs/maven.html)
and [Cucumber CLI](https://cucumber.io/docs/cucumber/api/).
