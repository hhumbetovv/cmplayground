//package az.theternal.core.framework.base
//
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.MutableSharedFlow
//import kotlinx.coroutines.flow.filter
//import kotlinx.coroutines.flow.map
//import kotlinx.coroutines.sync.Mutex
//import kotlinx.coroutines.sync.withLock
//import kotlin.reflect.KClass
//
//interface BaseEvent
//
//object EventBus {
//    private val eventMap = mutableMapOf<KClass<out BaseEvent>, MutableSharedFlow<BaseEvent>>()
//    private val mutex = Mutex()
//
//    suspend fun <T : BaseEvent> fire(event: T) {
//        mutex.withLock {
//            val flow = eventMap[event::class]
//            flow?.emit(event)
//        }
//    }
//
//    fun <T : BaseEvent> on(eventClass: KClass<T>): Flow<T> {
//        val flow = eventMap.getOrPut(eventClass) {
//            MutableSharedFlow(extraBufferCapacity = 64)
//        }
//
//        @Suppress("UNCHECKED_CAST")
//        return flow.filter { eventClass.isInstance(it) }
//            .map { it as T }
//    }
//}
