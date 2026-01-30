package az.theternal.cmplayground

import androidx.lifecycle.ViewModel
import az.theternal.cmplayground.DemoContract.*
import az.theternal.core.framework.delegates.EffectEmitter
import az.theternal.core.framework.delegates.EffectProducer
import az.theternal.core.framework.delegates.IntentHandler
import az.theternal.core.framework.delegates.IntentProcessor
import az.theternal.core.framework.delegates.StateHolder
import az.theternal.core.framework.delegates.StateStore
import az.theternal.core.framework.delegates.ViewEffect
import az.theternal.core.framework.delegates.ViewIntent
import az.theternal.core.framework.delegates.ViewState
import az.theternal.core.framework.delegates.currentState
import az.theternal.core.framework.delegates.setState

class DemoViewModel : ViewModel(), StateHolder<State>, IntentHandler<Intent>, EffectProducer<Effect> {
    override val stateStore: StateStore<State> = StateStore(
        initializer = { State() }
    )
    override val intentProcessor: IntentProcessor<Intent> = IntentProcessor { intent ->
        when(intent) {
            Intent.Decrement -> increment()
            Intent.Increment -> decrement()
        }
    }
    override val effectEmitter: EffectEmitter<Effect> = EffectEmitter()

    private fun increment() {
        setState { copy(count = count + 1) }
        if(currentState().count % 10 == 0) {
            sendEffect(Effect.ShowToast)
        }
    }

    private fun decrement() {
        setState { copy(count = count - 1) }
    }
}

sealed interface DemoContract {
    sealed interface Intent : ViewIntent {
        data object Increment : Intent
        data object Decrement : Intent
    }
    data class State(
        val count: Int = 0,
    ) : ViewState

    sealed interface Effect : ViewEffect {
        data object ShowToast : Effect
    }
}