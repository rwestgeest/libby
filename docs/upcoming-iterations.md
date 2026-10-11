# Upcoming course iterations

Course roadmap checked on 2026-10-11, after completing **004 — Runs and targets**.
These are goals supplied by the course, not project-specific goals.

| Iteration | Goal | What it adds |
|---|---|---|
| [005 — Fan out](https://github.com/lean-software-production/tutorial/blob/main/docs/iterations/005-fan-out/README.md) | Run machines in parallel, then combine their results. | Assembly lines can branch and rejoin. The course example uses three reviewers on different providers’ models, followed by a synthesiser that decides what happens next. |
| [006 — Watching](https://github.com/lean-software-production/tutorial/blob/main/docs/iterations/006-watching/README.md) | Observe a factory running independently of your terminal. | Background execution, a persistent output record, live watching with catch-up, token usage by machine/provider/run, and stop/resume commands. Machines communicate through ACP. |
| [007 — Steering](https://github.com/lean-software-production/tutorial/blob/main/docs/iterations/007-steering/README.md) | Intervene while a machine is working. | Send a message to a specific active machine, including during parallel work. Steering is temporary; lasting instructions belong in the seed, plan, or machine prompt. |

The progression is **parallel work → visibility → live intervention**.

The [course ledger](https://github.com/lean-software-production/tutorial/blob/main/docs/iterations/README.md)
ends at 007 as of the date above.
