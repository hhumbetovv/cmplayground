package az.theternal.cmplayground.feature.counter

import az.theternal.core.ViewEffect
import az.theternal.core.ViewIntent
import az.theternal.core.ViewState


sealed interface CounterContract {

    sealed interface Intent : ViewIntent {

        data object Increase : Intent

        data object Decrease : Intent

    }

    data class State(
        val count: Int = 0
    ) : ViewState

    sealed interface Effect : ViewEffect {

        data class ShowSnackbar(
            val text: String
        ) : Effect

    }

}