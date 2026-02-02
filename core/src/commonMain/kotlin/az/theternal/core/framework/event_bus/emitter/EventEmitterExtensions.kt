package az.theternal.core.framework.event_bus.emitter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.theternal.core.framework.event_bus.BaseEvent
import az.theternal.core.framework.event_bus.EventBus
import az.theternal.core.framework.event_bus.GlobalEventBus

context(viewModel: ViewModel, _: EventEmitter)
fun EventEmitterHandler(
    eventBus: EventBus = GlobalEventBus,
): EventEmitterHandler {
    return EventEmitterHandler(
        scope = viewModel.viewModelScope,
        eventBus = eventBus,
    ).also { handler ->
        viewModel.addCloseable(handler)
    }
}

context(_: ViewModel, emitter: EventEmitter)
fun fireEvent(event: BaseEvent) {
    emitter.eventEmitterHandler.fireEvent(event)
}
