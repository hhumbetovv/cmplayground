Architecture Overview

This project implements a lightweight, explicit navigation architecture on top of Navigation3. The core ideas:

- Single source of truth for navigation state (a list of `NavKey`).
- A small, UI-agnostic `Navigator` abstraction to request navigation.
- A thin composition layer that renders the back stack and provides the `Navigator` to the UI.

Navigation State
- Back stack lives in the root layer as `List<NavKey>`.
- Default owner is `RootViewModel` (persistent, lifecycle-aware).
- A minimal alternative exists with Compose state for prototypes.
— The owner can also be a single composable holding state via `remember` (no ViewModel) when lifecycle persistence isn’t needed.

Navigator
- Defines a tiny API: push, replace, replaceAll, popBack, popBackTo and etc.
- Stateless implementation (`RootNavigator`) applies mutations via a function injected from the state owner.
- No platform/UI details in the navigator; only transforms a list of `NavKey`.

Composition Layer
- `NavigatorProvider` exposes a `Navigator` using a composition local (no prop drilling).
- `NavDisplay` renders the current back stack and resolves entries from graphs.
- Graphs register destinations for sealed `NavKey`s (e.g., `authGraph`, `postGraph`).

Transitions
- Push/pop transitions are centralized and pluggable.
- Predictive pop transition integrates with Android 33+ back progress.

Platform Notes
- Android: predictive back enabled at the application level and used by transitions.
- iOS: root ViewModel is created via a factory to avoid unsupported default factories.

Extending the App
- Add a sealed route type implementing `NavKey` (serialize if it has arguments).
- Register it in a feature graph (`entry<MyRoute.Foo> { FooView() }`).
- Include the graph in root `entryProvider { ... }`.
- Navigate from UI via `LocalNavigator.current` using the API.

Principles
- Keep state in one place; mutate through narrow APIs.
- Keep navigation logic pure/testable (no UI or platform leakage).
- Keep composables dumb: emit intents, don’t own navigation state.
