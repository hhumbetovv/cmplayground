package az.theternal.samples.createpost.poll

import az.theternal.mvi.Logic
import az.theternal.mvi.StoreScope
import az.theternal.mvi.setState

/** A level 1 slice: its own rules (two to four options), nothing else. */
class PollLogic : Logic<PollState, PollIntent, Nothing> {
    private typealias Scope = StoreScope<PollState, Nothing>

    override fun Scope.onIntent(intent: PollIntent) {
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
