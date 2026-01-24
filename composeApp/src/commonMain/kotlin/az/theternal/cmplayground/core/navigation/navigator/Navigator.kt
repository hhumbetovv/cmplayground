package az.theternal.cmplayground.core.navigation.navigator

import androidx.navigation3.runtime.NavKey
import az.theternal.cmplayground.core.navigation.common.NavStackBlock

interface Navigator {
    fun buildStack(block: NavStackBlock)
    fun push(routeKey: NavKey)
    fun replace(routeKey: NavKey)
    fun replaceAll(routeKey: NavKey)
    fun replaceAll(routeKeys: List<NavKey>)
    fun canPop(): Boolean
    fun popBack()
    fun popBackTo(
        inclusive: Boolean = false,
        predicate: (NavKey) -> Boolean,
    ): Boolean
}