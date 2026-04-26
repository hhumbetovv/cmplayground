package az.theternal.cmplayground.core.navigation.guard

import androidx.navigation3.runtime.NavKey

sealed interface GuardResult {
    data object Allow : GuardResult
    data object Block : GuardResult
    data class Redirect(val destination: NavKey) : GuardResult
}
