package az.theternal.core.framework.delegates.event_bus.emitter

import az.theternal.core.framework.delegates.event_bus.BaseEvent
import az.theternal.core.framework.delegates.event_bus.EventBus
import az.theternal.core.framework.delegates.event_bus.GlobalEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class EventEmitterDelegate(
    private val scope: CoroutineScope,
    private val eventBus: EventBus = GlobalEventBus,
) : AutoCloseable {
    fun fireEvent(event: BaseEvent) {
        scope.launch { eventBus.fire(event) }
    }

    override fun close() = Unit
}
