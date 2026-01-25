package az.theternal.cmplayground.core.navigation.result

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

val LocalResultManager = staticCompositionLocalOf<ResultManager> {
    error("ResultManager not provided in composable tree")
}

@Composable
fun ResultManagerProvider(
    content: @Composable (() -> Unit)
) {
    val resultManager = remember { ResultManager() }

    CompositionLocalProvider(
        LocalResultManager provides resultManager,
        content = content
    )
}
