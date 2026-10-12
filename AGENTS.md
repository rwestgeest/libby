# Agent instructions

This is the starter for a software factory built during the lean software
manufacturing course. Open the agent at the repository root. Student
factory source and fetched homework live in `factory/`. Setup creates
`bin/factory`, a symlink to the entry point in the student's chosen language.
The homework specs define how the factory runs and manages its targets.

When helping a student, use the skills below for the homework they request.
When maintaining the starter itself, work on the requested scaffolding,
skills or tooling change; homework adoption is a separate student action.
Course content is authored in the tutorial repository and fetched here.

Students name their fork for their capstone project. Tetris is the shared
practice target their factories build first, not their capstone. Explain
that distinction when introducing the course to a student.

- `factory/spec/` holds the current homework iteration, fetched from the course.
  Its feature files are the factory's test suite. Preserve it during ordinary
  homework implementation.
- `factory/ITERATION` holds the student's progress, e.g. `001 WIP`.

## Changing behavior with TDD

When the user explicitly requests a behavior change, directly or through a
supplied iteration seed or recorded product-owner instructions, that change
takes precedence over older conflicting expectations. The doer should update
the affected specs, tests and step definitions here using TDD, including fetched
specs; no upstream course change is required. This is an explicit exception to
the skills' usual rule against editing `spec/` during homework implementation.

Record which requirement supersedes the old expectation and why. Preserve checks
for requirements that still apply; do not delete, skip, filter out or weaken
tests just to get green. If the intended behavior is ambiguous, ask rather than
inventing a requirement. Required tests must pass before acceptance; failures
during the red phase do not prohibit implementation. If the implementation
already satisfies the revised expectation, record that result without
manufacturing a failure.

Skills, in `.agents/skills/` at the repository's root:

- **fetch-iteration** — when the student says "fetch iteration", or there is no iteration in progress.
- **set-up-factory** — when there is no factory project yet: sets up a Gherkin runner for `factory/spec/features/` in the student's language.
- **coach-me** — when the student says "coach me", asks to be coached, or wants to work through their homework with guidance.
- **implement-it** — when the student wants you to build the iteration and demo it.
- **implement-fast** — when the student wants you to just build the iteration.

If your harness doesn't load skills, read the skill's `SKILL.md` and follow it.

## Checks

From `factory/`, run `./test` (excludes `@real-agent`); use `./test -Preal-agent` to run real-agent examples explicitly. Full output is in `factory/target/test.log`; `mvn test` shows raw output. Requires JDK 21 and Maven.
