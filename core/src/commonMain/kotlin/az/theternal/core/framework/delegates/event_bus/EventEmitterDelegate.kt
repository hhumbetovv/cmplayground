package az.theternal.core.framework.delegates.event_bus

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class EventEmitterDelegate(
    private val scope: CoroutineScope,
    private val eventBus: EventBus = GlobalEventBus,
) : EventEmitter {
    override fun fireEvent(event: BaseEvent) {
        scope.launch { eventBus.fire(event) }
    }
    override fun close() = Unit
}