//package az.theternal.core.framework.base
//
//import androidx.lifecycle.viewModelScope
//import az.theternal.common.utils.Logger
//import kotlinx.coroutines.flow.launchIn
//import kotlinx.coroutines.flow.onEach
//
//abstract class EventScope {
//
//    context(viewModel: BaseViewModel<*, *, *>)
//    inline fun <reified T : BaseEvent> onEvent(noinline callback: suspend (T) -> Unit) {
//        val job = EventBus.on(T::class)
//            .onEach { event ->
//                if (viewModel.logEventCatch) {
//                    Logger.d("\uD83D\uDD25 Event Catch - ${event::class.simpleName} in ${viewModel::class.simpleName}")
//                }
//                callback(event)
//            }
//            .launchIn(viewModel.viewModelScope)
//
//        viewModel.eventSubscriptions += job
//    }
//
//    abstract fun setupObserver()
//}