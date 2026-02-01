package az.theternal.core.framework.delegates.event_bus.emitter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.theternal.core.framework.delegates.event_bus.BaseEvent
import az.theternal.core.framework.delegates.event_bus.EventBus
import az.theternal.core.framework.delegates.event_bus.GlobalEventBus

context(viewModel: ViewModel, _: EventEmitter)
fun EventEmitterDelegate(
    eventBus: EventBus = GlobalEventBus,
): EventEmitterDelegate {
    return EventEmitterDelegate(
        scope = viewModel.viewModelScope,
        eventBus = eventBus,
    ).also { delegate ->
        viewModel.addCloseable(delegate)
    }
}

context(_: ViewModel, emitter: EventEmitter)
fun fireEvent(event: BaseEvent) {
    emitter.eventEmitterDelegate.fireEvent(event)
}
