# Homework 4 — Runs and targets

Read `FACTORY.md`, then the feature files in `features/`. Together they are
the whole spec of the factory at this point, not just the new parts.

Your factory already builds into the target folder you choose. Until now
every invocation supplied a seed and target and used the same assembly line. Each
target kept one plan: an implicit run for that generation. This homework
introduces named runs that remember what they build and how, while the
factory source stays in `factory/`.

- **The assembly lines move into the target.** A target holds its
  product and, in `.assembly-lines/`, the lines that build it, with the
  machines they name in `.assembly-lines/.machines/`, a folder each. A
  machine's name is unique in its target: every line there that names
  the doer runs the same doer. Move your line and machines from homework
  3 into `tetris/tetris-003/.assembly-lines/` (or whichever target you
  choose).
- **Each execution is a named run.** You name the run, and the first time
  you start it you give it a target, one of that target's lines, and a seed.
  The run remembers all three; after that, its name is enough. Its
  authoritative plan stays in the target, at `.factory/runs/<name>/plan.md`,
  separate from the seed. To resume your homework-3 game, move its
  `.factory/plan.md` to that named path. Or start with a fresh target.
  All machines use the same plan, including on retries and resumption.
  Keep initial planning and accepted-task progress in the target's Git
  history, preserving unrelated changes. Factory-owned run settings are
  execution state, not another copy of the plan. Existing factory-owned
  plans must not be silently discarded or restarted; report unsupported
  legacy resumption clearly rather than inventing a second plan.

Once the route is data, a target can hold more than one line. Write a second
one — the same line without the validator will do — and have each run say
which line it runs.

Starting or resuming a run is an **invocation** of that same run. Its name,
settings and plan persist across invocations.

## The commands you'll end up with

The exact shape is your choice. Run from the repository root, with
`bin/factory` linked to your entry point:

```sh
# a new run: its target, its line and its seed
$ bin/factory --run tetris --target tetris/tetris-003 --line careful --seed tetris/spec.md

# after that, its name is enough
$ bin/factory --run tetris

# a second target: write snake/spec.md describing Snake first
# give it a line and machines of its own
$ mkdir -p snake/snake1
$ cp -r tetris/tetris-003/.assembly-lines snake/snake1/
$ bin/factory --run snake --target snake/snake1 --line quick --seed snake/spec.md
```

The factory should build whatever the new seed describes, without disturbing
the first run.

The checks still run against a copy of your factory. The copy builds in
separate targets, each holding its own plans, lines and machines; the factory
keeps the settings needed to resume each run by name.
