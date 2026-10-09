# CMPlayground — MVI patterns

This branch (`feat/mvi-patterns-v2`) holds MVI at four levels of complexity, with rules for choosing a level
and moving between levels, and the parts of Orbit MVI and Decompose worth having. Start with
[`docs/mvi/`](docs/mvi/README.md):

- [guide](docs/mvi/guide.md): writing a screen, choosing a level, the rules, and a review checklist;
- [migrations](docs/mvi/migrations.md): when to move a screen between levels or hosts, in both directions,
  and how;
- [reference](docs/mvi/reference.md): how the runtime works and why.

- `mvi/` is the runtime: `Store`, `Logic`, `ActionLogic` with policies, phases (`whileIn`, `runOn`),
  `Slice` and `Slot`, `MachineLogic` (over dev.klitsie.statemachine), `StateKeeper`, `ContentState`, and
  the Compose helpers, per-item ViewModel stores among them.
- `mvi-test/` is the test DSL: `storeTest { send(…); expectState { copy(…) }; expectEffect(…) }`.
- `samples/` has one sample per level, sign-in written four ways, a create-post screen whose draft
  survives process death, and a feed with per-card ViewModels and a report sheet. Each has tests in
  `samples/src/commonTest`.
- Tests: `./gradlew :mvi:jvmTest :samples:jvmTest`. The app opens on the samples catalog.

---

This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…