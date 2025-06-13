package az.theternal.cmplayground.feature.counter

import az.theternal.cmplayground.feature.counter.CounterContract.*
import az.theternal.core.BaseViewModel

class CounterViewModel : BaseViewModel<Intent, State, Effect>() {

    override fun createState(): State = State()

    override fun onIntentUpdate(intent: Intent) {
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
            launchEffect(
                Effect.ShowSnackbar("Checkpoint: $count")
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}