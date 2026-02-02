# CMPlayground

A modular MVI architecture for Kotlin Multiplatform using composable delegate handlers.

## Philosophy

Traditional MVI implementations bundle everything into massive ViewModels. This framework separates concerns into **independent delegate handlers** that you can mix and match.

Want state management? Implement `StateHolder`. Need side effects? Add `EffectProducer`. Each responsibility gets its own delegate.

## Architecture Pattern

Each delegate follows the same pattern:

1. **Interface** - Defines the contract (e.g., `EffectProducer`)
2. **Handler** - Implements the logic (e.g., `EffectHandler`)
3. **Extensions** - Provides ergonomic APIs using context receivers

Let's explore this with `EffectProducer` as an example.

### Example: EffectProducer Delegate

**1. The Interface**

```kotlin
interface EffectProducer<Effect : ViewEffect> {
    val effectHandler: EffectHandler<Effect>
}
```

Simple. Your ViewModel implements this and provides a handler instance.

**2. The Handler**

```kotlin
class EffectHandler<Effect : ViewEffect> internal constructor(
    private val scope: CoroutineScope,
) : AutoCloseable {
    private val _effects = Channel<Effect>()
    val effects: Flow<Effect> = _effects.receiveAsFlow()

    fun sendEffect(effect: Effect) {
        scope.launch {
            _effects.send(effect)
        }
    }

    override fun close() {
        _effects.close()
    }
}
```

The handler manages the actual implementation - in this case, a Channel for one-time effects.

**3. The Extensions**

```kotlin
// Factory with context receivers - auto-wires scope and lifecycle
context(viewModel: ViewModel, _: EffectProducer<Effect>)
fun <Effect : ViewEffect> EffectHandler(): EffectHandler<Effect> {
    return EffectHandler<Effect>(
        scope = viewModel.viewModelScope
    ).also { handler ->
        viewModel.addCloseable(handler)
    }
}

// Convenience function for sending effects
context(viewModel: ViewModel, producer: EffectProducer<Effect>)
fun <Effect : ViewEffect> sendEffect(effect: Effect) {
    viewModel.viewModelScope.launch {
        producer.effectHandler.sendEffect(effect)
    }
}

// Compose integration
@Composable
fun <Effect : ViewEffect> EffectProducer<Effect>.OnEffectUpdate(
    collector: FlowCollector<Effect>
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(this, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            effectHandler.effects.collect(collector)
        }
    }
}
```

Extensions leverage context receivers to provide clean APIs without manual scope management.

### Other Delegates

The framework includes four more delegates following the same pattern:

- **StateHolder** - Reactive state management with `StateFlow`
- **IntentConsumer** - User action processing with `SharedFlow`
- **EventObserver** - Subscribe to global events via EventBus
- **EventEmitter** - Broadcast global events via EventBus

Each has its own interface, handler, and extensions. Check `/core/framework/` for implementations.

---

## Complete Example: Counter

### Contract

```kotlin
sealed interface CounterContract {
    sealed interface Intent : ViewIntent {
        data object Increase : Intent
        data object Decrease : Intent
    }

    data class State(
        val count: Int = 0
    ) : ViewState

    sealed interface Effect : ViewEffect {
        data class ShowSnackbar(val text: String) : Effect
    }
}
```

### ViewModel

```kotlin
class CounterViewModel : ViewModel(),
    StateHolder<State>,
    IntentConsumer<Intent>,
    EffectProducer<Effect>,
    EventObserver {

    override val stateHandler = StateHandler { State() }
    override val effectHandler = EffectHandler()
    override val eventObserverHandler = EventObserverHandler {
        on<NotifyEvent> {
            setState { copy(count = -100) }
        }
    }
    override val intentHandler = IntentHandler { intent ->
        when (intent) {
            Intent.Increase -> handleIncrease()
            Intent.Decrease -> handleDecrease()
        }
    }

    private fun handleIncrease() {
        val newCount = currentState.count + 1
        setState { copy(count = newCount) }
        if (newCount % 10 == 0) {
            sendEffect(Effect.ShowSnackbar("Milestone: $newCount"))
        }
    }

    private fun handleDecrease() {
        val newCount = currentState.count - 1
        setState { copy(count = newCount) }
    }
}
```

### UI

```kotlin
@Composable
fun CounterView(
    viewModel: CounterViewModel = viewModel { CounterViewModel() }
) {
    viewModel.OnEffectUpdate { effect ->
        when (effect) {
            is Effect.ShowSnackbar -> {
                // Show snackbar
            }
        }
    }

    val state by viewModel.collectAsState()

    CounterContent(
        count = state.count,
        onIncrease = { viewModel.postIntent(Intent.Increase) },
        onDecrease = { viewModel.postIntent(Intent.Decrease) }
    )
}
```

---

## Key Benefits

**Separation of Concerns**
Each delegate handles one responsibility. State management, effects, intents, and events are isolated.

**Composable**
Mix and match only the delegates you need. Read-only screen? Just `StateHolder`. Complex form? Use all five.

**Type Safety**
Sealed interfaces ensure exhaustive when expressions and compile-time guarantees.

**Lifecycle Aware**
Handlers automatically integrate with ViewModel lifecycle. No manual cleanup needed.

**Ergonomic APIs**
Context receivers eliminate boilerplate. Write `setState { }` instead of `stateHandler.setState { }`.

---

## Project Structure

```
core/framework/
├── state_holder/
│   ├── StateHolder.kt          # Interface
│   ├── StateHandler.kt         # Handler implementation
│   └── StateExtensions.kt      # Context receiver extensions
├── effect_producer/
│   ├── EffectProducer.kt
│   ├── EffectHandler.kt
│   └── EffectExtensions.kt
├── intent_consumer/
├── event_bus/
│   ├── observer/
│   └── emitter/
```

Each delegate follows the same structure: interface, handler, extensions.

---

## Learn More

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Kotlin Context Receivers](https://github.com/Kotlin/KEEP/blob/master/proposals/context-receivers.md)
