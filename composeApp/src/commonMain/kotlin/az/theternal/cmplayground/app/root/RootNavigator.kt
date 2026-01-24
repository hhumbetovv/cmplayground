package az.theternal.cmplayground.app.root

import androidx.navigation3.runtime.NavKey
import az.theternal.cmplayground.core.navigation.common.NavStackBlock
import az.theternal.cmplayground.core.navigation.common.NavStackBuilder
import az.theternal.cmplayground.core.navigation.navigator.Navigator

class RootNavigator(
    val stack: List<NavKey>,
    val buildRootStack: NavStackBuilder,
) : Navigator {

    companion object {
        private const val REQUIRED_VIEW_COUNT_FOR_POP = 2
    }

    override fun buildStack(block: NavStackBlock) = buildRootStack(block)

    override fun push(routeKey: NavKey) {
        buildStack { add(routeKey) }
    }

    override fun replace(routeKey: NavKey) {
        buildStack {
            removeLastOrNull()
            add(routeKey)
        }
    }

    override fun replaceAll(routeKey: NavKey) {
        buildStack {
            clear()
            add(routeKey)
        }
    }

    override fun replaceAll(routeKeys: List<NavKey>) {
        buildStack {
            clear()
            addAll(routeKeys)
        }
    }

    override fun canPop(): Boolean {
        return stack.size >= REQUIRED_VIEW_COUNT_FOR_POP
    }

    override fun popBack() {
        buildStack {
            if (size >= REQUIRED_VIEW_COUNT_FOR_POP) {
                removeLastOrNull()
            }
        }
    }

    override fun popBackTo(
        inclusive: Boolean,
        predicate: (NavKey) -> Boolean,
    ): Boolean {
        val index = stack.indexOfLast(predicate)
        if (index == -1) return false

        val newSize = if (inclusive) index else index + 1
        val newStack = stack.take(newSize)

        buildStack {
            clear()
            addAll(newStack)
        }

        return true
    }
}