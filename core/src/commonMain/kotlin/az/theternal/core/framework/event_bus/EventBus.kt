package az.theternal.core.framework.event_bus

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass

interface BaseEvent

interface EventBus {
    suspend fun fire(event: BaseEvent)
    suspend fun <T : BaseEvent> on(eventClass: KClass<T>): Flow<T>
}

open class DefaultEventBus : EventBus {
    private val mutex = Mutex()
    private val eventMap = mutableMapOf<KClass<out BaseEvent>, MutableSharedFlow<BaseEvent>>()

    private suspend fun flowOf(clazz: KClass<out BaseEvent>): MutableSharedFlow<BaseEvent> =
        mutex.withLock {
            eventMap.getOrPut(clazz) {
                MutableSharedFlow(extraBufferCapacity = 64)
            }
        }

    override suspend fun fire(event: BaseEvent) {
        flowOf(event::class).emit(event)
    }

    override suspend fun <T : BaseEvent> on(eventClass: KClass<T>): Flow<T> {
        val flow = flowOf(eventClass)

        @Suppress("UNCHECKED_CAST")
        return flow
            .filter { eventClass.isInstance(it) }
            .map { it as T }
    }
}

object GlobalEventBus : DefaultEventBus()