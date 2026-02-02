package az.theternal.core.framework.event_bus.observer

import az.theternal.core.framework.event_bus.BaseEvent
import kotlin.reflect.KClass

interface EventObserver {
    val eventObserverHandler: EventObserverHandler

    interface Scope {
        fun <T : BaseEvent> on(
            eventClass: KClass<T>,
            block: suspend (T) -> Unit
        )
    }
}

inline fun <reified T : BaseEvent> EventObserver.Scope.on(
    noinline callback: suspend (T) -> Unit
) {
    return on(
        eventClass = T::class,
        block = callback
    )
}
