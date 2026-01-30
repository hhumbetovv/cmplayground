package az.theternal.cmplayground.feature.counter

import androidx.lifecycle.ViewModel
import az.theternal.cmplayground.feature.counter.CounterContract.*
import az.theternal.core.framework.delegates.EffectDelegate
import az.theternal.core.framework.delegates.EffectProducer
import az.theternal.core.framework.delegates.IntentConsumer
import az.theternal.core.framework.delegates.IntentDelegate
import az.theternal.core.framework.delegates.StateProvider
import az.theternal.core.framework.delegates.StateDelegate
import az.theternal.core.framework.delegates.sendEffect
import az.theternal.core.framework.delegates.setState

class CounterViewModel : ViewModel(), EffectProducer<Effect>, IntentConsumer<Intent>, StateProvider<State> {

    override val stateDelegate: StateDelegate<State> = StateDelegate { State() }
    override val effectDelegate: EffectDelegate<Effect> = EffectDelegate()

    override val intentDelegate: IntentDelegate<Intent> = IntentDelegate { intent ->
        when(intent) {
            Intent.Decrease -> onDecreaseIntent()
            Intent.Increase -> onIncreaseIntent()
        }
    }


    private fun onDecreaseIntent() {
        val updatedCount = currentState.count - 1
        setState { copy(count = updatedCount) }
        checkCount(updatedCount)
    }

    private fun onIncreaseIntent() {
        val updatedCount = currentState.count + 1
        setState { copy(count = updatedCount) }
        checkCount(updatedCount)
    }
    private fun checkCount(count: Int) {
        if(count % 10 == 0) {
            sendEffect(
                Effect.ShowSnackbar("Checkpoint: $count")
            )
        }
    }
}