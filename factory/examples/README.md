# Machine configuration examples

The `.machines/` folders are editable examples, not defaults embedded in the factory.
Copy planner, doer and validator into your target's `.assembly-lines/.machines/`.
Copy an assembly line such as `tetris/tetris-005/.assembly-lines/careful.dot`
into the target's `.assembly-lines/` too. Edit each machine's `AGENTS.md` to
change its job or review lens. The factory sends run data and a result schema;
the harness loads the machine's instructions separately.

`config.json` can select `harness` (default `pi`) and `model`. A CLI `--model`
is a fallback for machines without a model. For pi, model names can include
the provider prefix, such as `openai/<model>`. `apiKeyEnv` optionally names
the provider's API-key variable when it cannot be inferred from that prefix
or from the harness. With no model prefix, pi's key defaults to
`ANTHROPIC_API_KEY`; set `apiKeyEnv` when native pi settings select another provider.

Copy a login into each folder yourself: `auth.json` for pi or Codex,
`.credentials.json` for Claude Code. Keep the example `.gitignore` so
credentials and generated harness state stay out of Git. Without a login,
the harness uses its provider's API key from the environment. Missing
credentials for any coding-agent machine stop the run before the first call.
Ordinary executable machines do not need coding-agent credentials.

To use Claude Code, set `harness` to `claude` and add `CLAUDE.md` containing
`@AGENTS.md`. Claude runs with `CLAUDE_CONFIG_DIR` pointing to this folder and
`--setting-sources user`. For Codex, set `harness` to `codex`, configure the
model and permissions in the folder's `config.toml`, and use its own
`auth.json` or `CODEX_API_KEY`. Codex runs with `CODEX_HOME` set to this folder
and `project_doc_max_bytes=0` to suppress target and ancestor instructions.
pi runs with `PI_CODING_AGENT_DIR`, `--no-context-files` and its own
`AGENTS.md` passed via `--append-system-prompt`.

From the repository root, after configuring the target's machines:

```sh
bin/factory --run tetris-005a --line careful --seed tetris/spec.md --target tetris/tetris-005a
```

This command contacts real agents. Resume the same run with:

```sh
bin/factory --run tetris-005a
```
