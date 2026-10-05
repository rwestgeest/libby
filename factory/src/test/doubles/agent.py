#!/usr/bin/env python3
import json
import sys
from pathlib import Path

# Keep call records beside this double, in the temporary workspace.
calls = Path(__file__).parent / "calls"
calls.mkdir(exist_ok=True)
number = len(list(calls.glob("*.json")))
(calls / f"{number}.json").write_text(json.dumps({
    "args": sys.argv[1:],
    "cwd": str(Path.cwd()),
}))

# The factory must run the agent inside its target.
plan = Path(".factory/plan.md")
plan.parent.mkdir(exist_ok=True)

if not plan.exists():
    plan.write_text("- [ ] alpha\n- [ ] beta\n")
    result = {"complete": False}
else:
    lines = plan.read_text().splitlines()
    pending = next(
        (i for i, line in enumerate(lines) if line.startswith("- [ ] ")),
        None,
    )
    if pending is None:
        result = {"complete": True}
    else:
        task = lines[pending][6:]
        Path(f"{task}.txt").write_text(f"Work for {task}\n")
        lines[pending] = f"- [x] {task}"
        plan.write_text("\n".join(lines) + "\n")
        result = {"complete": False, "task": task}

print(json.dumps(result))