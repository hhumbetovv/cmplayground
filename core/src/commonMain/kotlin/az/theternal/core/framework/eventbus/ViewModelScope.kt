//package az.theternal.core.framework.base
//
//import androidx.compose.runtime.Stable
//import kotlin.reflect.KClass
//
//@Stable
//class ViewModelScope {
//    private val viewModelStacks = mutableMapOf<KClass<*>, MutableList<BaseViewModel<*, *, *>>>()
//
//    @Suppress("UNCHECKED_CAST")
//    fun <T : BaseViewModel<*, *, *>> get(key: KClass<T>): T? {
//        return viewModelStacks[key]?.lastOrNull() as? T
//    }
//
//    fun <T : BaseViewModel<*, *, *>> push(key: KClass<T>, viewModel: T) {
//        viewModelStacks.getOrPut(key) { mutableListOf() }.add(viewModel)
//    }
//
//    @Suppress("UNCHECKED_CAST")
//    fun <T : BaseViewModel<*, *, *>> pop(key: KClass<T>): T? {
//        val stack = viewModelStacks[key]
//        return if (stack.isNullOrEmpty()) {
//            null
//        } else {
//            stack.removeLastOrNull() as? T
//        }
//    }
//
//    fun <T : BaseViewModel<*, *, *>> getOrPush(key: KClass<T>, factory: () -> T): T {
//        return get(key) ?: factory().also { push(key, it) }
//    }
//
//    fun inheritFrom(parent: ViewModelScope) {
//        parent.viewModelStacks.forEach { (key, stack) ->
//            if (!viewModelStacks.containsKey(key) && stack.isNotEmpty()) {
//                viewModelStacks[key] = stack.toMutableList()
//            }
//        }
//    }
//
//    fun clear() {
//        viewModelStacks.clear()
//    }
//
//    fun getStackSize(key: KClass<*>): Int = viewModelStacks[key]?.size ?: 0
//}