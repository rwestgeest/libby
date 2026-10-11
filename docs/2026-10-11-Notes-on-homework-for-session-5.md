# Notes on homework

## Preparation

### preparing vm

Needed vm for security reasons and because qubigo is an existing project. 

- got lost a bit in managing and provisioning vm scripts

A bit of work to get the vm running properly with docker and the full local setup. Good exercise but outside course scope learned that. 

- docker setup works
- problems with selinux on a fedora cloud machine 
- the factory has no problems with accessing another git repo
- testing against a local machine without a browser is still an issue

## first factory run on qubigo

### from vision to initial iteration with the prompt:

```
study @docs/vision.md

You are my product owner and chief architect. I want to get started on this project and plan our first iteration. Work with me to create a plan in docs/iterations/001-<goal>.md

Keep interviewing me until we agree on the scope and details. Write enough detail in the plan that other agents could pick it up and implement without any other context.
```

## First attempt

I had a set of small improvemements in one iteration. 001-improvements.md was way to bit and lacked focus. Initial run with

```
./bin/factory --run qubigo-001 --target qubigo --line careful --seed qubigo/docs/iterations/001-polish-bugs.md
```

Resulted in the factory stopping after creating the plan. The plan contained no tasks '[ ]'. I didn't understand and reverted.

## Second attempt

### Backlog an initial plan

More focus. Created a backlog.md in docs/ and used the prompt to create an initial iteration. 

The llm was a bit eager to make the iteration a kind of implementation plan. Had do steer a bit, to stick more to the goal.

[qubigo/docs/iterations/001-clickable-app-banner.md](/qubigo/docs/iterations/001-clickable-app-banner.md)


### Making sense of the result

There are quite some files generated and it is somewhat hard to make sense of it. I tend to start wondering. This, and the potential improvements to the factory to deal with this may be part of the tutorial. Or should i fix this myself now? 

Wondering is time consuming. 

So i aske codex:

> explain the latest factory log. what is where? There is some stuff in factory/.factory the assembly line is in qubigo/.assembly-line and more information is in qubigo/docs/iterations 
  - how is it all tied together

Result in: [2026-10-11-qubigo-factory-run-explained.md](./2026-10-11-qubigo-factory-run-explained.md)

The only thing I don't really understand is the expected plan missing. Apparently there's a plan supplied by the factory in the runs directory, but the agents are also instructed to work only inside the qubigo target. There's an ambiguity there??? 

Analysis an direction here: [2026-10-11-factory-plan-ownership.md](./2026-10-11-factory-plan-ownership.md)
