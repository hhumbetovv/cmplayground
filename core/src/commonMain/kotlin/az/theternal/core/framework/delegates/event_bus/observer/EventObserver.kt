package az.theternal.core.framework.delegates.event_bus.observer

import az.theternal.core.framework.delegates.event_bus.BaseEvent
import kotlin.reflect.KClass

interface EventObserver {
    val eventObserverDelegate: EventObserverDelegate

    interface Scope {
        fun <T : BaseEvent> on(
            eventClass: KClass<T>,
            block: suspend (T) -> Unit
        )
    }
}

inline fun <reified T : BaseEvent> EventObserver.Scope.on(
    noinline callback: suspend (T) -> Unit
) = on(T::class, callback)
