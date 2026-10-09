package az.theternal.samples.createpost.body

import az.theternal.mvi.Logic
import az.theternal.mvi.StoreScope
import az.theternal.mvi.setState

data class BodyState(
    val text: String = "",
) {
    val remaining: Int get() = MAX_LENGTH - text.length

    companion object {
        const val MAX_LENGTH = 280
    }
}

sealed interface BodyIntent {
    data class Edit(
        val text: String,
    ) : BodyIntent
}

/** A level 1 slice: no IO, no effects. */
class BodyLogic : Logic<BodyState, BodyIntent, Nothing> {
    override fun StoreScope<BodyState, Nothing>.onIntent(intent: BodyIntent) {
        when (intent) {
            is BodyIntent.Edit -> setState { copy(text = intent.text.take(BodyState.MAX_LENGTH)) }
        }
    }
}
