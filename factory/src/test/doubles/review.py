#!/usr/bin/env python3
import json
import os
import sys
from pathlib import Path

sys.stdin.read()
folder = Path(__file__).parent
calls = folder / "calls"
calls.mkdir(exist_ok=True)
number = len(list(calls.glob("*.json")))
(calls / f"{number}.json").write_text(json.dumps({"args": sys.argv[1:], "cwd": str(Path.cwd())}))
(calls / f"{number}.txt").write_text(sys.argv[-1])
(folder / "pid").write_text(str(os.getpid()))

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
    print(json.dumps({"satisfied": not rejected, "findings": [finding.read_text()] if rejected else []}))
