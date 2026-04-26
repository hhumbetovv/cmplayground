package az.theternal.cmplayground.core.navigation.guard

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey

interface NavGuard {
    @Composable
    fun evaluate(key: NavKey?): GuardResult
}
