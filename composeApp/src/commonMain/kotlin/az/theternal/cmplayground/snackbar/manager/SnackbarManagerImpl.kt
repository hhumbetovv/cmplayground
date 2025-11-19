package az.theternal.cmplayground.snackbar.manager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.concurrent.Volatile

class SnackbarManagerImpl : SnackbarManager {
    private val _currentSnackbar = mutableStateOf<SnackbarData?>(null)
    override val currentSnackbar: State<SnackbarData?> = _currentSnackbar

    private val _isVisible = mutableStateOf(false)
    override val isVisible: State<Boolean> = _isVisible

    private val queueMutex = Mutex()
    @Volatile
    private var currentDismissRequest: CompletableDeferred<Unit>? = null

    override suspend fun showSnackbar(data: SnackbarData) {
        queueMutex.withLock {
            val dismissRequest = CompletableDeferred<Unit>()
            currentDismissRequest = dismissRequest

            _currentSnackbar.value = data
            _isVisible.value = true

            try {
                withTimeoutOrNull(timeMillis = data.durationMillis) {
                    dismissRequest.await()
                }
            } finally {
                currentDismissRequest = null
                dismissInternal()
            }
        }
    }

    override suspend fun dismiss() {
        currentDismissRequest?.complete(Unit)
    }

    private suspend fun dismissInternal() {
        if (_currentSnackbar.value == null) return

        _isVisible.value = false
        delay(300)

        _currentSnackbar.value?.onDismiss?.invoke()
        _currentSnackbar.value = null

        delay(100)
    }
}

@Composable
fun rememberSnackbarManager(): SnackbarManager = remember {
    SnackbarManagerImpl()
}