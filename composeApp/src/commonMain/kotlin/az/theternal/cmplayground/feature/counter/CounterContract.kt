package az.theternal.cmplayground.feature.counter

import az.theternal.core.framework.delegates.ViewEffect
import az.theternal.core.framework.delegates.ViewIntent
import az.theternal.core.framework.delegates.ViewState


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