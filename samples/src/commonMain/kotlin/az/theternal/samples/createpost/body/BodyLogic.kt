package az.theternal.samples.createpost.body

import az.theternal.mvi.Logic
import az.theternal.mvi.StoreScope
import az.theternal.mvi.setState

/** A level 1 slice: no IO, no effects. */
class BodyLogic : Logic<BodyState, BodyIntent, Nothing> {
    private typealias Scope = StoreScope<BodyState, Nothing>

    override fun Scope.onIntent(intent: BodyIntent) {
        when (intent) {
            is BodyIntent.Edit -> setState { copy(text = intent.text.take(BodyState.MAX_LENGTH)) }
        }
    }
}
