#!/usr/bin/env python3
import json
import os
import sys
import subprocess
from pathlib import Path

sys.stdin.read()
folder = Path(__file__).parent
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

calls = folder / "calls"
calls.mkdir(exist_ok=True)
number = len(list(calls.glob("*.json")))
(calls / f"{number}.json").write_text(json.dumps({"args": sys.argv[1:], "cwd": str(Path.cwd()), "configuration": str(configuration), "instructions": instructions, "auth": auth}))
(calls / f"{number}.txt").write_text(prompt)
(folder / "pid").write_text(str(os.getpid()))

if "git diff --no-ext-diff HEAD --" in prompt:
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

if (folder / "hold").exists():
    print("reviewing", flush=True)
    (folder / "ready").write_text("ready")
    import time
    while not (folder / "release").exists():
        time.sleep(0.02)

gates = folder.parent / "stop-gates"
if gates.exists():
    if folder.name in ("reviewer_1", "reviewer_3"):
        gate = os.open(gates / folder.name, os.O_RDWR)
        os.read(gate, 1)
        raise RuntimeError("A held reviewer should be stopped by the factory")
    elif folder.name == "reviewer_2":
        # Opening each peer's FIFO waits until it has begun. No bytes are sent:
        # peers stay at work while this branch fails.
        for peer in gates.iterdir():
            os.close(os.open(peer, os.O_WRONLY))

if (folder / "crash").exists():
    print("reviewer crashed", file=sys.stderr)
    sys.exit(7)

barrier = folder.parent / "review-barrier"
if folder.name.startswith("reviewer_") and barrier.exists():
    # Each reviewer opens its mailbox before sending to its peers. A sequential
    # runner blocks on the first peer; concurrent reviewers exchange two bytes.
    mailbox = os.open(barrier / folder.name, os.O_RDWR)
    for peer in barrier.iterdir():
        if peer.name != folder.name:
            with peer.open("wb", buffering=0) as writer:
                writer.write(b"1")
    for _ in range(2):
        os.read(mailbox, 1)
    os.close(mailbox)

if (folder / "no-result").exists():
    print("I checked the work, but have no structured result.")
else:
    rejected = (folder / "never-satisfied").exists() or ((folder / "reject-first").exists() and number == 0)
    finding = folder / "finding.txt"
    blocked = (folder / "blocked").exists()
    print(json.dumps({"blocked": blocked, "satisfied": not rejected, "findings":
        ["Owner must confirm signing key persistence."] if blocked else [finding.read_text()] if rejected else []}))
