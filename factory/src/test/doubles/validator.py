#!/usr/bin/env python3
import json
import os
import sys
import subprocess
from pathlib import Path

sys.stdin.read()

config_keys = ("PI_CODING_AGENT_DIR", "CLAUDE_CONFIG_DIR", "CODEX_HOME")
active = [key for key in config_keys if os.environ.get(key)]
assert len(active) == 1, "Expected only the selected machine's configuration"
configuration = Path(os.environ[active[0]])
if active[0] == "CLAUDE_CONFIG_DIR":
    assert sys.argv[sys.argv.index("--setting-sources") + 1] == "user"
    assert (configuration / "CLAUDE.md").read_text().strip() == "@AGENTS.md"
    instruction_file = configuration / "AGENTS.md"
elif active[0] == "CODEX_HOME":
    assert sys.argv[1] == "exec" and "project_doc_max_bytes=0" in sys.argv
    instruction_file = configuration / "AGENTS.md"
else:
    assert "--no-context-files" in sys.argv
    instruction_file = Path(sys.argv[sys.argv.index("--append-system-prompt") + 1])
    assert instruction_file == configuration / "AGENTS.md"
instructions = instruction_file.read_text()
prompt = instructions + "\n" + sys.argv[-1]
login_file = ".credentials.json" if active[0] == "CLAUDE_CONFIG_DIR" else "auth.json"
auth = "login" if (configuration / login_file).is_file() else "api-key"
if auth == "api-key":
    config_file = configuration / "config.json"
    config = json.loads(config_file.read_text()) if config_file.exists() else {}
    model_provider = config.get("model", "").split("/")[0]
    key = config.get("apiKeyEnv") or ("CODEX_API_KEY" if active[0] == "CODEX_HOME" else
        "GEMINI_API_KEY" if model_provider in ("google", "google-gemini") else "ANTHROPIC_API_KEY")
    assert os.environ.get(key), "The harness received no provider API key"
assert not any(os.environ.get(key) for key in ("ANTHROPIC_AUTH_TOKEN", "ANTHROPIC_OAUTH_TOKEN", "CLAUDE_CODE_OAUTH_TOKEN"))

calls = Path(__file__).parent / "calls"
calls.mkdir(exist_ok=True)
number = len(list(calls.glob("*.json")))
(calls / f"{number}.json").write_text(json.dumps({
    "args": sys.argv[1:],
    "cwd": str(Path.cwd()),
    "configuration": str(configuration), "instructions": instructions, "auth": auth,
}))
(calls / f"{number}.txt").write_text(prompt)

# Follow the factory's working-tree review instructions, including new files.
pathspec = [".", ":(exclude).factory", ":(exclude).assembly-lines"]
assert "git diff --no-ext-diff HEAD --" in prompt
assert "git ls-files --others --exclude-standard --" in prompt
has_head = subprocess.run(["git", "rev-parse", "--verify", "HEAD"], capture_output=True).returncode == 0
changes = subprocess.check_output(["git", "diff", "--no-ext-diff", "HEAD", "--", *pathspec]) if has_head else b""
new_files = subprocess.check_output([
    "git", "ls-files", *([] if has_head else ["--cached"]), "--others", "--exclude-standard", "-z", "--", *pathspec,
]).decode().split("\0")
for path in filter(None, new_files):
    changes += subprocess.run([
        "git", "diff", "--no-ext-diff", "--no-index", "--", "/dev/null", path,
    ], capture_output=True, check=False).stdout
(calls / f"{number}.product-changes").write_bytes(changes)

workspace = os.environ.get("FACTORY_TEST_WORKSPACE")
if workspace:
    with (Path(workspace) / "call-order.txt").open("a") as order:
        order.write("validator\n")

message_file = Path(__file__).parent / "before-result.txt"
if message_file.exists():
    print(message_file.read_text())
if (Path(__file__).parent / "no-result").exists():
    print("I checked the work, but have no structured result.")
else:
    folder = Path(__file__).parent
    rejected = (folder / "never-satisfied").exists() or (
        (folder / "reject-first").exists() and number == 0
    ) or (
        (folder / "reject-alternate").exists() and number % 2 == 0
    )
    field_file = folder / "result-field.txt"
    field = field_file.read_text().strip() if field_file.exists() else "satisfied"
    print(json.dumps({
        "blocked": (folder / "blocked").exists(),
        field: not rejected,
        "findings": ["Owner must confirm signing key persistence."] if (folder / "blocked").exists() else ["Separate game logic from terminal input so it can be tested."] if rejected else [],
    }))
    trailing = folder / "after-result.txt"
    if trailing.exists():
        print(trailing.read_text())
