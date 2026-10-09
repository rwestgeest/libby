#!/usr/bin/env python3
import json
import os
import sys
from pathlib import Path

# Like pi, wait for piped input to finish before processing the prompt.
sys.stdin.read()

# Keep call records beside this double, in the temporary workspace.
calls = Path(__file__).parent / "calls"
calls.mkdir(exist_ok=True)
number = len(list(calls.glob("*.json")))
(calls / f"{number}.json").write_text(json.dumps({
    "args": sys.argv[1:],
    "cwd": str(Path.cwd()),
}))
(calls / f"{number}.txt").write_text("\n".join(sys.argv[1:]))

# The factory must run the agent inside its target.
plan = Path(".factory/plan.md")
plan.parent.mkdir(exist_ok=True)

prose_plan = (Path(__file__).parent / "prose-plan").exists()
prompt = sys.argv[-1]
planner = "You are the planner." in prompt
validator = "You are the validator." in prompt
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
        if "The task's work has been committed." in prompt:
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
