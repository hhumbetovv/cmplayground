package az.theternal.core.framework.delegates.event_bus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
//import az.theternal.core.framework.extensions.plugin

fun ViewModel.eventEmitter(
    eventBus: EventBus = GlobalEventBus,
): EventEmitter {
//    return plugin {
        return EventEmitterDelegate(scope = viewModelScope)
//    }
}

fun ViewModel.eventObserver(
    eventBus: EventBus = GlobalEventBus,
    builder: EventObserver.Scope.() -> Unit,
): EventObserver {
//    return plugin {
        return EventObserverDelegate(
            scope = viewModelScope,
            builder = builder
        )
//    }
}