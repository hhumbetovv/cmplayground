package az.theternal.cmplayground

import androidx.lifecycle.ViewModel
import az.theternal.cmplayground.DemoContract.*
import az.theternal.core.framework.delegates.EffectDelegate
import az.theternal.core.framework.delegates.EffectProducer
import az.theternal.core.framework.delegates.IntentConsumer
import az.theternal.core.framework.delegates.IntentDelegate
import az.theternal.core.framework.delegates.StateProvider
import az.theternal.core.framework.delegates.StateDelegate
import az.theternal.core.framework.delegates.ViewEffect
import az.theternal.core.framework.delegates.ViewIntent
import az.theternal.core.framework.delegates.ViewState
import az.theternal.core.framework.delegates.currentState
import az.theternal.core.framework.delegates.setState

class DemoViewModel : ViewModel(), StateProvider<State>, IntentConsumer<Intent>, EffectProducer<Effect> {
    override val stateDelegate: StateDelegate<State> = StateDelegate(
        initializer = { State() }
    )
    override val intentDelegate: IntentDelegate<Intent> = IntentDelegate { intent ->
        when(intent) {
            Intent.Decrement -> increment()
            Intent.Increment -> decrement()
        }
    }
    override val effectDelegate: EffectDelegate<Effect> = EffectDelegate()

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