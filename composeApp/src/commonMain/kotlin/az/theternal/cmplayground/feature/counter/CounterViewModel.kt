package az.theternal.cmplayground.feature.counter

import androidx.lifecycle.ViewModel
import az.theternal.cmplayground.feature.counter.CounterContract.*
import az.theternal.core.framework.delegates.EffectEmitter
import az.theternal.core.framework.delegates.EffectProducer
import az.theternal.core.framework.delegates.IntentHandler
import az.theternal.core.framework.delegates.IntentProcessor
import az.theternal.core.framework.delegates.StateHolder
import az.theternal.core.framework.delegates.StateStore
import az.theternal.core.framework.delegates.currentState
import az.theternal.core.framework.delegates.sendEffect
import az.theternal.core.framework.delegates.setState

class CounterViewModel : ViewModel(), EffectProducer<Effect>, IntentHandler<Intent>, StateHolder<State> {

    override val stateStore: StateStore<State> = StateStore { State() }
    override val effectEmitter: EffectEmitter<Effect> = EffectEmitter()

    override val intentProcessor: IntentProcessor<Intent> = IntentProcessor { intent ->
        when(intent) {
            Intent.Decrease -> onDecreaseIntent()
            Intent.Increase -> onIncreaseIntent()
        }
    }


    private fun onDecreaseIntent() {
        val updatedCount = currentState().count - 1
        setState { copy(count = updatedCount) }
        checkCount(updatedCount)
    }

    private fun onIncreaseIntent() {
        val updatedCount = currentState().count + 1
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