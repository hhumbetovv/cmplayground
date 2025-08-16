package az.theternal.cmplayground.snackbar.manager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val DEFAULT_SNACKBAR_DURATION = 2000L

class SnackbarManagerImpl() : SnackbarManager {
    private var _currentSnackbar = mutableStateOf<SnackbarData?>(null)
    override val currentSnackbar: State<SnackbarData?> = _currentSnackbar

    private var _isVisible = mutableStateOf(false)
    override val isVisible: State<Boolean> = _isVisible

    private val snackbarQueue = mutableListOf<SnackbarData>()
    private var dismissJob: Job? = null
    private val mutex = Mutex()

    override fun showSnackbar(data: SnackbarData) {
        CoroutineScope(Dispatchers.Main).launch {
            mutex.withLock {
                if (_currentSnackbar.value == null) {
                    showSnackbarInternal(data)
                } else {
                    snackbarQueue.add(data)
                }
            }
        }
    }

    private fun showSnackbarInternal(data: SnackbarData) {
        dismissJob?.cancel()

        _currentSnackbar.value = data
        _isVisible.value = true

        dismissJob = CoroutineScope(Dispatchers.Main).launch {
            delay(DEFAULT_SNACKBAR_DURATION)
            dismiss()
        }
    }

    override fun dismiss() {
        _isVisible.value = false

        CoroutineScope(Dispatchers.Main).launch {
            delay(300)

            mutex.withLock {
                _currentSnackbar.value?.onDismiss?.invoke()
                _currentSnackbar.value = null

                if (snackbarQueue.isNotEmpty()) {
                    val nextSnackbar = snackbarQueue.removeAt(0)
                    delay(100)
                    showSnackbarInternal(nextSnackbar)
                }
            }
        }
    }
}

@Composable
fun rememberSnackbarManager() = remember {
    SnackbarManagerImpl()
}