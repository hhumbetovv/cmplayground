# MVI docs

One architecture, MVI, at four levels of complexity. Every screen starts at the simplest level and moves up
only when it crosses a measurable line. The screen never sees the move.

| Read | When |
|---|---|
| [guide.md](guide.md) | You are writing or reviewing a screen. Start here: the model, your first screen step by step, choosing a level, the rules, the review checklist |
| [migrations.md](migrations.md) | A screen crossed a line, or crossed one back. When to move between levels and hosts, in both directions, and the steps for each move |
| [reference.md](reference.md) | You need the details: how the store runs, the pipeline, slices and slots, item stores, process death, testing, the state-machine library, and what was taken from Orbit and Decompose and why |

**In one minute:**

- A screen has a **State** (one immutable value), **Intents** (what the user did) and **Effects** (one-off
  outputs), and one **Logic** class that turns intents into new states and effects.
- The **ViewModel** is a `StoreOwner` with one line, `override val store = store(initial, logic)`. The screen
  gets `viewModel { … }.store`.
- **Levels:**
  - level 1 changes the state directly;
  - level 2 adds actions and policies, for work that overlaps;
  - level 3 adds phases, for modes that exclude each other;
  - level 4 splits a big screen into slices.
- **Hosts:** a part of a screen can also become a slot (a dialog), a component ViewModel (per list item) or
  a use case. A logic moves between hosts unchanged.

The code: the runtime is in `mvi/`, the test DSL in `mvi-test/`, and the samples, one per level with tests,
in `samples/`.
