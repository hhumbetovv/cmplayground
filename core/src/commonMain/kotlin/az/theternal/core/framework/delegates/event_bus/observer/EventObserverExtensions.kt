package az.theternal.core.framework.delegates.event_bus.observer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.theternal.core.framework.delegates.event_bus.EventBus
import az.theternal.core.framework.delegates.event_bus.GlobalEventBus

context(viewModel: ViewModel, _: EventObserver)
fun EventObserverHandler(
    eventBus: EventBus = GlobalEventBus,
    builder: EventObserver.Scope.() -> Unit,
): EventObserverHandler {
    return EventObserverHandler(
        scope = viewModel.viewModelScope,
        eventBus = eventBus,
        builder = builder,
    ).also { handler ->
        viewModel.addCloseable(handler)
    }
}
