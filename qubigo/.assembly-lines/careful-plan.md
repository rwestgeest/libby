# Careful plan assembly line

Run from the factory checkout:

```sh
bin/factory --run qubigo-careful-plan --target q --line careful-plan --seed q/docs/iterations/002-clearer-onboarding.md
```

The authoritative runnable line is `.assembly-lines/careful-plan.dot`. Existing planner and doer configurations are reused unchanged. Dedicated machines leave the other lines unchanged.

The initial planner (`plan_initializer`) creates the run plan, or reads an existing plan without marking tasks done. A completed plan routes through the regular planner to finish. The revision machine reads reviewer reports beside that exact plan, revises unfinished tasks and removes consumed reports. Three independent model families review in parallel. The consensus machine requires unanimous approval of the same plan hash. Rejection repeats revision and review. Approval runs the doer, followed by an OO/RDD validator; rejection retries the doer with findings. Acceptance returns to the planner to commit work and mark the task done. Each remaining task routes directly from the regular planner to the doer. The three-brain review runs only during initial planning, repeating revisions until unanimous approval; implementation never routes back to it.

Reviewer models are configured individually and override the shared CLI model. Pi needs access to OpenAI Codex and OpenRouter. Reviewers share the same review criteria; only model and report filename differ. Report sidecars live beside the run plan, outside product commits. The lens is stored under `.assembly-lines/lenses/` and embedded in the validator configuration because the factory treats `lens` as literal text.

Plan review repeats until unanimous approval; the factory's `--max-attempts` bounds implementation attempts, not plan revision rounds. Stop the run manually if reviewers cannot converge. Consensus and report writing rely on the configured agents following their instructions. No real-agent run is performed by installing this line.
