# Problem: prose-only doer output stops the run before validation

Date: 2026-10-18

## Current outcome — 05 implemented

Libby's machine runner now makes one format-only repair attempt when a
successfully completed pi doer returns no JSON value. Tools, extensions, MCP,
skills and prompt templates are disabled for repair; the original answer stays
in the run record. A repaired handoff still requires independent validation.
Blocked input remains blocked; another unreadable result stops the run.

This covers pi print and pi ACP invocations. Other harnesses retain their
existing failure behavior until a tool-free repair path is supported. Explicit
invalid JSON values and unsuccessful process exits are not format-repaired.
The cause of the original omission remains unknown; this is bounded recovery,
not a claim that omissions are prevented. Regression and full-suite results are
recorded below. No old run has been resumed.

Earlier sections preserve the original diagnosis and candidate discussion.

## Context and expected standard

Run `qubigo-004`, target `/home/rob/dev/qubigo` through Libby's `q` symlink,
line `careful-plan`, task 7 repair. A doer must return a JSON object with
`findings`; implementation acceptance belongs to the validator and planner.

## What happened and impact

The last doer supplied only a Markdown summary. The run stopped with
`failed: Could not read the doer's result`; task 7 remains unaccepted.
The checkpoint `221d7bb3` preserves the work, not proof of correctness.

## Investigation

Assessed 18 October 2026. No fixes or tests run. Local evidence below is ignored
and requires the Libby checkout; source references describe inspected current
code, not a captured historical prompt.

### Evidence

- [Run settings](/home/rob/dev/libby/factory/runs/qubigo-004/settings.json),
  [status](/home/rob/dev/libby/factory/runs/qubigo-004/status), and
  [daemon tail](/home/rob/dev/libby/factory/runs/qubigo-004/daemon.log): rejected
  validator review followed by the unreadable-result stop.
- [Machine record](/home/rob/dev/libby/factory/runs/qubigo-004/record.jsonl):
  line 1778 is the last rejected review; 1780 starts the final doer;
  1781–1800 contain Markdown claims and test counts; 1801 ends that machine.
  No JSON result or later validator appears. Earlier output at 1772–1775
  contains fenced JSON and proceeds to validation at 1777: a different case.
- [MachineJobs.kt](/home/rob/dev/libby/factory/src/main/kotlin/MachineJobs.kt),
  lines 12–24 and 32–37, supplies the single-line JSON request.
- [MachineRunner.kt](/home/rob/dev/libby/factory/src/main/kotlin/MachineRunner.kt),
  lines 97–146 and 231–277, collects stdout, checks exit success, then parses
  the result. Extraction already tolerates fences and multiline objects; no
  object means failure. There is no format-correction exchange on this path.
- [TaskLifecycle.kt](/home/rob/dev/libby/factory/src/main/kotlin/TaskLifecycle.kt),
  lines 16–22, 36–43 and 53–55: implementation retries depend on a decoded
  routing result; work is committed at the subsequent planner boundary.

### Five whys: occurrence and containment

1. **Why did the run stop?** The doer's captured answer contains no result
   object, so extraction cannot produce the object needed for routing.
2. **Why was validation not reached?** Decoding is a prerequisite to returning
   from the machine invocation. The implementation retry route cannot repair
   a failure that occurs before a decoded result exists.
3. **Why does one format omission end the invocation?** The inspected runner
   has one execution followed by parsing and failure, with no separate bounded
   handoff-repair path. This is an actionable orchestration mechanism.
4. **Why did the doer omit JSON?** Unknown. The current prompt requests it,
   but the record captures output and state, not the exact final input or full
   harness session. The provider is recorded as DeepSeek; attributing the
   omission to model capability, context overload or prompt conflict is an
   untested hypothesis. Stop this branch here.

The malformed handoff did **not** escape into acceptance. The existing gate
protected acceptance but lost automatic progress. Widening JSON extraction
cannot recover an object that was never emitted. Inventing success from prose
would remove that protection.

### Options and recommendation

First challenge the need for the artifact: the summary is for humans, but a
machine-readable result still carries routing and unavailable-input signals.
Keep that purpose; do not require the human summary as another deliverable.

- **Correction:** preserve this answer and work; obtain a valid result before
  ordinary independent validation. Do not replay the entire implementation just
  to repair serialization.
- **Containment, recommended first:** evaluate one bounded format-only repair
  after successful execution with an unreadable result. Preserve the raw answer,
  prohibit product edits during repair, retain blocked/findings meaning, and
  require normal validation afterward. If repair fails, stop explicitly. This
  addresses the demonstrated interruption; it does not prevent omissions.
- **Prevention option:** investigate a harness-enforced structured response if
  supported. It could remove reliance on prose instructions, but support across
  harnesses is unverified and may cost more than the first countermeasure.
- **Do nothing:** preserves safety but retains manual recovery on recurrence.

Before implementing, reproduce the exact prose-only answer in a factory
regression. Verify bounded failure, preserved output, no acceptance or commit
on malformed/blocked output, and successful repair routing to the validator.
Track omissions and repair attempts separately in a representative later run.

### Unknowns and disconfirming checks

Inspect any retained harness session before claiming the request was ignored
rather than absent from the historical input. Check nonzero-exit and truncated
output cases separately; this record alone does not diagnose them. A repair
that repeats product work or silently drops blocked findings is inadequate.
At the original assessment no countermeasure had been applied. The implementation
and verification below supersede that state; effectiveness in 004a remains to
be observed.

## Implementation and regression verification — 18 October 2026

A new prose-only fixture first reproduced `Could not read the doer's result`
before implementation. The response parser now distinguishes no JSON value
from a present but invalid final value; only the former pi-doer case receives
one tool-free formatting invocation. It receives the captured answer as data,
not a replay of the implementation job. The repaired object requires a findings
array and a boolean blocked field if present. Implementation retries, validator
routing and commit acceptance stay under the existing lifecycle.

Regression coverage includes successful formatting before validation, rejection
still preventing commits, exhaustion after one repair, preserved external-input
blocking, retained original output, and pi ACP formatting. Libby commit `07aa81b` contains the fix. `cd factory && ./test` passed:
140 scenarios / 2021 steps. The subsequently added pi ACP scenario also passed
(1 scenario / 14 steps), after recompiling tests. The focused invocation feature
passed all 14 scenarios before the extra ACP case was added.
Ledger Closed records the implemented bounded recovery, not prevention of the
original generation defect or support for every harness.
