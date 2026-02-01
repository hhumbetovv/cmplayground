package az.theternal.cmplayground.feature.counter

import androidx.lifecycle.ViewModel
import az.theternal.cmplayground.feature.NotifyEvent
import az.theternal.cmplayground.feature.counter.CounterContract.*
import az.theternal.core.framework.delegates.effect_producer.EffectHandler
import az.theternal.core.framework.delegates.effect_producer.EffectProducer
import az.theternal.core.framework.delegates.effect_producer.sendEffect
import az.theternal.core.framework.delegates.event_bus.observer.EventObserver
import az.theternal.core.framework.delegates.event_bus.observer.EventObserverHandler
import az.theternal.core.framework.delegates.event_bus.observer.on
import az.theternal.core.framework.delegates.intent_consumer.IntentConsumer
import az.theternal.core.framework.delegates.intent_consumer.IntentHandler
import az.theternal.core.framework.delegates.state_holder.StateHolder
import az.theternal.core.framework.delegates.state_holder.StateHandler
import az.theternal.core.framework.delegates.state_holder.setState

class CounterViewModel : ViewModel(), EffectProducer<Effect>, IntentConsumer<Intent>,
    StateHolder<State>, EventObserver {

    override val stateHandler: StateHandler<State> = StateHandler { State() }
    override val effectHandler: EffectHandler<Effect> = EffectHandler()
    override val eventObserverHandler: EventObserverHandler = EventObserverHandler {
        on<NotifyEvent> {
            setState { copy(count = -100) }
        }
    }

    override val intentHandler: IntentHandler<Intent> = IntentHandler { intent ->
        when (intent) {
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
        if (count % 10 == 0) {
            sendEffect(
                Effect.ShowSnackbar("Checkpoint: $count")
            )
        }
    }
}
