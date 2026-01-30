package az.theternal.core.framework.delegates.event_bus

import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass

interface EventEmitter : AutoCloseable {
    fun fireEvent(event: BaseEvent)
}
interface EventObserver : AutoCloseable {
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

interface EventBus {
    suspend fun fire(event: BaseEvent)
    suspend fun <T : BaseEvent> on(eventClass: KClass<T>): Flow<T>
}