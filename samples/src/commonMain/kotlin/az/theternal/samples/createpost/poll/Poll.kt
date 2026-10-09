package az.theternal.samples.createpost.poll

import az.theternal.mvi.Logic
import az.theternal.mvi.StoreScope
import az.theternal.mvi.setState

data class PollState(
    val isOn: Boolean = false,
    val options: List<String> = List(MIN_OPTIONS) { "" },
) {
    val canAdd: Boolean get() = options.size < MAX_OPTIONS
    val canRemove: Boolean get() = options.size > MIN_OPTIONS

    companion object {
        const val MIN_OPTIONS = 2
        const val MAX_OPTIONS = 4
    }
}

sealed interface PollIntent {
    data object Toggle : PollIntent

    data class EditOption(
        val index: Int,
        val text: String,
    ) : PollIntent

    data object AddOption : PollIntent

    data class RemoveOption(
        val index: Int,
    ) : PollIntent
}

/** A level 1 slice: its own rules (two to four options), nothing else. */
class PollLogic : Logic<PollState, PollIntent, Nothing> {
    override fun StoreScope<PollState, Nothing>.onIntent(intent: PollIntent) {
        when (intent) {
            PollIntent.Toggle -> setState { copy(isOn = !isOn) }
            is PollIntent.EditOption -> setState {
                copy(options = options.mapIndexed { index, option -> if (index == intent.index) intent.text else option })
            }
            PollIntent.AddOption -> if (state.canAdd) setState { copy(options = options + "") }
            is PollIntent.RemoveOption -> if (state.canRemove) {
                setState { copy(options = options.filterIndexed { index, _ -> index != intent.index }) }
            }
        }
    }
}
