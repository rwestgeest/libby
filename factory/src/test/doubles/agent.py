#!/usr/bin/env python3
import json
import os
import sys
import subprocess
import re
from pathlib import Path

# Like pi, wait for piped input to finish before processing the prompt.
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

# Keep call records beside this double, in the temporary workspace.
calls = Path(__file__).parent / "calls"
calls.mkdir(exist_ok=True)
number = len(list(calls.glob("*.json")))
(calls / f"{number}.json").write_text(json.dumps({
    "args": sys.argv[1:],
    "cwd": str(Path.cwd()),
    "configuration": str(configuration), "instructions": instructions, "auth": auth,
}))
(calls / f"{number}.txt").write_text(prompt)

hold = Path(__file__).parent / "hold"
if hold.exists():
    message_file = Path(__file__).parent / "before-result.txt"
    print(message_file.read_text().strip() if message_file.exists() else "working", flush=True)
    (Path(__file__).parent / "ready").write_text("ready")
    while not (Path(__file__).parent / "release").exists():
        import time
        time.sleep(0.02)

# The factory must run the agent inside its target, while the prompt points it
# at the plan belonging to the named run.
plan_match = re.search(r'The plan is at "([^"]+)"\.', prompt)
if not plan_match:
    raise RuntimeError("The prompt did not name a plan")
plan = Path(plan_match.group(1))
plan.parent.mkdir(exist_ok=True)

prose_plan = (Path(__file__).parent / "prose-plan").exists()
planner = "You are the planner." in prompt
validator = "You are the validator." in prompt
if planner:
    def git(*args):
        return subprocess.run(
            ["git", *args], text=True, capture_output=True,
        ).stdout.strip()

    (calls / f"{number}.git-state").write_text(json.dumps({
        "head": git("rev-parse", "HEAD"),
        "status": git("status", "--porcelain", "--", "."),
    }))
workspace = os.environ.get("FACTORY_TEST_WORKSPACE")
if workspace:
    role = "planner" if planner else "validator" if validator else "doer"
    with (Path(workspace) / "call-order.txt").open("a") as order:
        order.write(role + "\n")

if validator:
    print(json.dumps({"satisfied": True, "findings": []}))
    sys.exit(0)

if not plan.exists():
    if prose_plan:
        plan.write_text("First make alpha; afterward make beta. Neither is ready.\n")
    else:
        plan.write_text("- [ ] alpha\n- [ ] beta\n")
    result = {"complete": False}
else:
    text = plan.read_text()
    if prose_plan:
        task = ("alpha" if "Neither is ready" in text else
                "beta" if "Beta is next" in text else None)
        next_plan = ("Alpha is ready. Beta is next.\n" if task == "alpha" else
                     "Both alpha and beta are ready.\n")
    else:
        lines = text.splitlines()
        pending = next(
            (i for i, line in enumerate(lines) if line.startswith("- [ ] ")),
            None,
        )
        task = lines[pending][6:] if pending is not None else None
        if task is not None:
            lines[pending] = f"- [x] {task}"
        next_plan = "\n".join(lines) + "\n"

    if task is None:
        result = {"complete": True}
    elif planner:
        if "Task work committed: true." in prompt:
            plan.write_text(next_plan)
            complete = ("Both alpha and beta are ready." in next_plan if prose_plan
                        else "- [ ] " not in next_plan)
            result = {"complete": complete}
        else:
            result = {"complete": False}
    else:
        name_file = Path(__file__).parent / "product-name.txt"
        product = name_file.read_text().strip() if name_file.exists() else f"{task}.txt"
        Path(product).write_text(f"Work for {task}\n")
        result = {"task": task}

message_file = Path(__file__).parent / "before-result.txt"
if message_file.exists():
    print(message_file.read_text())
if (Path(__file__).parent / "no-result").exists():
    print("I did some work, but have no structured result.")
else:
    print(json.dumps(result))
after_file = Path(__file__).parent / "after-result.txt"
if after_file.exists():
    print(after_file.read_text())
