# Homework 005a: internationalisation lens demo

The fresh target is `tetris/tetris-005a-i18n`, built from the unchanged
`tetris/spec.md` seed through the `careful` assembly line. Planner, doer and
validator all ran real pi with model `openai-codex/gpt-6-astra`. Each used its
own ignored copy of the pi login. No credentials are tracked.

The validator's baseline lens was testability. Its `AGENTS.md` was changed to
internationalisation, asking for replaceable user-visible messages and safe
handling of Unicode in terminal columns. Neither additional languages nor a
language selector were required. Planner and doer instructions stayed the same
as the examples. Factory source hashes before and after the run are identical.

## Live command

From the repository root:

```sh
bin/factory --run tetris-005a-i18n --line careful --seed tetris/spec.md --target tetris/tetris-005a-i18n
```

The command exited successfully with `complete: true` and `factory stopped`.
The run's completed plan and full output are in the ignored local directory
`factory/runs/tetris-005a-i18n/` (`plan.md` and `demo.log`).

## What the changed lens found

The engine was accepted. The first terminal interface was rejected with these
actual findings:

- `src/render.js:14,22-35; src/app.js:7`: User-visible labels, controls, status messages, resize guidance, and startup errors are embedded in application/rendering logic. Move them into one replaceable English message catalogue, including parameterized score and size messages.
- `src/render.js:14,39`: String.slice clips UTF-16 code units rather than terminal columns: replacement messages containing emoji can be split, combining sequences truncated, and wide characters allowed to wrap beyond the intended display. Use grapheme-safe, terminal-column-aware clipping at both sites; test wide characters, combining marks, and emoji while preserving the 24-row limit.

The doer recorded those findings as subtasks, added `src/messages.js` and
`src/text.js`, and added Unicode/catalogue regression tests. The validator then
accepted the revised interface. It also accepted the final documentation and
lifecycle-validation task. Three product tasks were committed:

```text
1d9e047 Record factory task
064ac8a Record factory task
669fe9d Record factory task
```

This demonstrates a real review following the chosen lens, with no factory source
change during the demo. It is not a controlled comparison against a second fresh
build under the original lens.

## Verification

- Factory suite before the demo: 115 scenarios and 1,646 steps passed.
- Independently reran `npm test` in the fresh target: 32 tests passed.
- Independently ran `python3 test/pty-smoke.py`: four scenarios passed, including
  actual `npm start` gameplay/pause/game-over/restart/quit and signal cleanup.
- Independently supplied Japanese title/score/line/level/status messages plus an
  emoji through the catalogue API: 22 output rows, each under 60 terminal columns.
- Copied machine logins are ignored by Git and untracked.

## Try the generated game

```sh
cd tetris/tetris-005a-i18n
npm start
```

Use an interactive terminal at least 60 columns by 22 rows. Arrow keys move and
rotate, Space hard-drops, P pauses, R restarts, and Q quits. User-visible messages
are in `src/messages.js`; no factory rebuild is needed to edit that catalogue.
The second optional demo, changing one machine's harness, was not run.
