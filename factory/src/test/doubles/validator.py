#!/usr/bin/env python3
import json
import sys
from pathlib import Path

sys.stdin.read()

calls = Path(__file__).parent / "calls"
calls.mkdir(exist_ok=True)
number = len(list(calls.glob("*.json")))
(calls / f"{number}.json").write_text(json.dumps({
    "args": sys.argv[1:],
    "cwd": str(Path.cwd()),
}))
(calls / f"{number}.txt").write_text("\n".join(sys.argv[1:]))

print(json.dumps({"satisfied": True, "findings": []}))
