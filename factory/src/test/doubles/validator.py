#!/usr/bin/env python3
import json
import os
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
        field: not rejected,
        "findings": ["Separate game logic from terminal input so it can be tested."] if rejected else [],
    }))
    trailing = folder / "after-result.txt"
    if trailing.exists():
        print(trailing.read_text())
