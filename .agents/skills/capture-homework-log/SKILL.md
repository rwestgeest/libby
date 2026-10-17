---
name: capture-homework-log
description: Capture learning and results from the current session in the homework log, refresh the Qubigo sharing snapshot, and commit and push the relevant documentation for tutors and fellow students.
---

Capture the current conversation's useful homework evidence and publish it from this repository. Invoking this workflow authorizes committing and pushing the capture; no additional confirmation is needed unless a concrete ambiguity or execution restriction requires it.

## Locate the context

Work from the repository root and read its agent instructions. Inspect Git status, staged changes, branch and upstream before editing so existing work can be kept separate.

Read the conversation and the relevant homework log in `docs/`. The session 7 log is `docs/2026-10-15-Homework-for-session-7.md`. Prefer the session named by the student or established in the conversation; otherwise inspect existing homework logs and `factory/ITERATION`. Course session numbers and homework iteration numbers are different. Ask only if the intended log remains ambiguous. Do not create a new session log merely because the date changed.

The live capstone is normally reached through `q`, a symlink to `~/dev/qubigo`; resolve the actual path, including `Q` if that is what exists. This repository's `qubigo/` directory is the sharing snapshot. Do not confuse it with the live application repository or commit in the live repository.

Read `bin/take-over-qubigo` to confirm its current paths and export scope. It copies vision, backlog, iterations, kaizen notes, assembly lines and skills, preserving their layout. Use this script rather than reproducing its copy logic.

## Capture the session

Preserve the log's previous notes and style. Add a dated entry or extend the matching entry without duplicating earlier captures. Write for tutors and fellow students who have not read the conversation. Capture the attempt and its purpose, observed results and obstacles, decisions and reasons, lessons, open questions and concrete next steps. Include useful commands and links to relevant iteration documents, assembly lines, kaizen notes, reports or commits.

Use evidence from the conversation and inspected artifacts. Distinguish observed results from proposals and unverified explanations. Do not invent successful tests or completed homework. Keep the entry an account of learning rather than a transcript or exhaustive change list. Use relative links to shared repository material, not absolute filesystem paths or the local `q` symlink.

Run `bin/take-over-qubigo` from the repository root. Pass the resolved source and this repository's `qubigo/` destination explicitly when needed. If required source material is missing or the script fails, report the blocker without fabricating files or silently substituting a partial copy. The script updates files without deleting stale snapshot files; do not infer deletions from absent source files.

## Review and publish

Review the log and snapshot diff before staging. Check links and exclude credentials, private conversation details and unrelated application code. Include snapshot changes explaining the captured work; include pre-existing changes only when their relevance is established by the conversation and source material. Leave unrelated changes unstaged. Do not edit `factory/spec/`, adopt another iteration, mark homework Done or commit factory implementation as a side effect.

Stage explicit paths or selected hunks for the log and relevant supporting material. Inspect the staged diff and run `git diff --cached --check`. Preserve unrelated staged changes; use a scoped commit strategy or ask if they cannot safely be separated. Documentation capture does not require the factory test suite. Report tests only when actually observed.

Commit with a descriptive message identifying the course session, then push the current branch to its established upstream. If no upstream exists, inspect remotes and use the student's fork when unambiguous; never default to the tutorial repository. Do not force-push, reset changes or automatically pull/rebase after a rejected push. Stop and report a rejection or authentication failure with the local commit preserved. If no capture changes exist, avoid an empty commit; check whether an earlier capture commit still needs pushing.

Finish with the log path, what was captured, the commit identifier and push outcome. If blocked, state exactly what remains unpublished.
