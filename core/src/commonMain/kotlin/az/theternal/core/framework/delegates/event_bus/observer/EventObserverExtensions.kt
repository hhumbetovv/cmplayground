package az.theternal.core.framework.delegates.event_bus.observer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.theternal.core.framework.delegates.event_bus.EventBus
import az.theternal.core.framework.delegates.event_bus.GlobalEventBus

context(viewModel: ViewModel, _: EventObserver)
fun EventObserverDelegate(
    eventBus: EventBus = GlobalEventBus,
    builder: EventObserver.Scope.() -> Unit,
): EventObserverDelegate {
    return EventObserverDelegate(
        scope = viewModel.viewModelScope,
        eventBus = eventBus,
        builder = builder,
    ).also { delegate ->
        viewModel.addCloseable(delegate)
    }
}
