package az.theternal.core.framework.delegates.event_bus

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class EventObserverDelegate(
    private val scope: CoroutineScope,
    private val eventBus: EventBus = GlobalEventBus,
    builder: (EventObserver.Scope.() -> Unit)
) : EventObserver {

    private val jobs = mutableListOf<Job>()

    private val dsl = object : EventObserver.Scope {
        override fun <T : BaseEvent> on(
            eventClass: KClass<T>,
            block: suspend (T) -> Unit
        ) {
            val job = scope.launch {
                eventBus.on(eventClass)
                    .onEach { block(it) }
                    .launchIn(this)
            }
            jobs += job
        }
    }

    init { builder(dsl) }

    override fun close() {
        jobs.forEach { it.cancel() }
        jobs.clear()
    }
}