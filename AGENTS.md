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

- `factory/spec/` holds the current homework iteration, fetched from the course. Don't edit it. Its feature files are the factory's test suite.
- `factory/ITERATION` holds the student's progress, e.g. `001 WIP`.

Skills, in `.agents/skills/` at the repository's root:

- **fetch-iteration** — when the student says "fetch iteration", or there is no iteration in progress.
- **set-up-factory** — when there is no factory project yet: sets up a Gherkin runner for `factory/spec/features/` in the student's language.
- **coach-me** — when the student says "coach me", asks to be coached, or wants to work through their homework with guidance.
- **implement-it** — when the student wants you to build the iteration and demo it.
- **implement-fast** — when the student wants you to just build the iteration.
- **capture-homework-log** — capture session learning in the homework log, refresh the Qubigo sharing snapshot, and commit and push the capture.
- **discuss-factory-design** — pick up a factory design sketch with the student and record decisions before detailed planning; shared with pi through `.pi/skills/`.
- **analyze-factory-run** — analyze run evidence in `q/.factory/runs/<run>/`, diagnose problems and propose solutions when relevant.

If your harness doesn't load skills, read the skill's `SKILL.md` and follow it.

## Open design discussions

On session startup, look for `plan.md.discussion.md` handoffs under
`factory/runs/` and the live target's `.factory/runs/` when present. Briefly
surface files with `Status: open`; do not start or resolve a discussion without
the student choosing to work on it. Use **discuss-factory-design** when asked to
pick one up. If automatic skill loading is unavailable, read
`.agents/skills/discuss-factory-design/SKILL.md` explicitly. This is discussion
discovery, not an instruction to restart blocked factory runs.

## Checks

From `factory/`, run `./test` (excludes `@real-agent`); use `./test -Preal-agent` to run real-agent examples explicitly. Full output is in `factory/target/test.log`; `mvn test` shows raw output. Requires JDK 21 and Maven.
