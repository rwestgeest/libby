---
name: analyze-factory-run
description: Analyze a factory run when the user asks what happened, requests run analysis, or wants a diagnosis of a failed or stalled run. Read the run directory under q/.factory/runs/ and propose a solution when relevant.
---

Analyze the run from its recorded evidence and explain what happened, how it compares with the plan, and what should happen next if there is a problem.

## Locate and inspect

Work from the repository root. Runs live in `q/.factory/runs/<run>/`; `q` may be a symlink to the live target repository. Keep that repository distinct from any sharing snapshot.

Use the run named by the user or established in the conversation. Otherwise list available runs and use an unambiguous contextual match; ask which run to analyze if several remain plausible. Do not silently select by lexical ordering.

Start with these files in the selected run directory:

- `settings.json`: target, assembly line and other recorded configuration.
- `status`: recorded current status.
- `plan.md`: intended work, task dependencies and recorded progress.
- `daemon.log`: orchestration events and execution history.

Inventory the rest of the directory, including hidden entries. The directory contains the run's information: follow references and inspect relevant task records, machine logs, prompts, outputs, reviews and validation results. Discover their actual names and layout rather than assuming a fixed schema. Use settings to interpret the run. If inspection outside the run is needed, explain why and distinguish current files from configuration captured at run time.

Missing files are evidence gaps, not proof of failure. Continue with what exists and identify what cannot be established. For large logs, start with the beginning, end and searches for relevant task identifiers and events, then read enough surrounding context to reconstruct the sequence. A final error may hide an earlier cause.

## Reconstruct and diagnose

Connect planned work to observed execution: what started, completed, failed, retried, waited or was skipped, and the last recorded activity. Trace significant problems through task and machine artifacts. Distinguish the initiating cause from later symptoms, normal review/revision cycles from unproductive loops, and a proposed change from a fix that was applied and validated.

Compare status, plan progress and log evidence. Call out contradictions or stale records. For an active run, describe the observation point and re-read status and the log tail before concluding if activity may have continued. Do not infer a stall from silence alone or declare success solely from a completion message when validation evidence disagrees.

Support material findings with artifact links and line numbers, timestamps or task identifiers where available. Separate observed facts, likely explanations and unresolved questions. State evidence limits instead of inventing events or results.

## Present the analysis

Lead with the outcome or current situation and the intended objective, target and assembly line. Summarize the consequential execution sequence and how it followed or diverged from the plan. Explain any problem, its impact, evidence for its cause and remaining uncertainty. Keep the report proportional to the run; do not reproduce whole logs or expose secrets found in artifacts.

When relevant, propose a concrete correction at the appropriate layer: target code, task plan, machine instructions, assembly line or factory orchestration. Explain why it addresses the evidence, what would verify it and any prerequisites for resuming work. If evidence is insufficient for a fix, propose the next diagnostic step. A healthy run needs no invented problem or remedy.

An analysis request authorizes inspection and recommendations. Do not edit artifacts, implement fixes, restart or resume a run as a side effect; follow any separate user authorization for those actions.
