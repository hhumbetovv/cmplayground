package az.theternal.cmplayground.core.navigation.common

import androidx.navigation3.runtime.NavKey

typealias NavStackBlock = MutableList<NavKey>.() -> Unit
typealias NavStackBuilder = (block: NavStackBlock) -> Unit