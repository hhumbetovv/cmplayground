package az.theternal.core.framework.delegates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

interface ViewIntent

interface IntentConsumer<Intent : ViewIntent> {
    val intentDelegate: IntentDelegate<Intent>

    fun postIntent(newIntent: Intent) = intentDelegate::postIntent
}

class IntentDelegate<Intent : ViewIntent> internal constructor(
    private val scope: CoroutineScope,
    onIntent: (Intent) -> Unit,
) {

    private val intents = MutableSharedFlow<Intent>()

    init {
        intents.onEach(onIntent).launchIn(scope)
    }

    fun postIntent(intent: Intent) {
        scope.launch {
            intents.emit(intent)
        }
    }
}

context(viewModel: ViewModel, _: IntentConsumer<Intent>)
fun <Intent : ViewIntent> IntentDelegate(
    onIntent: (Intent) -> Unit,
): IntentDelegate<Intent> {
    return IntentDelegate(
        scope = viewModel.viewModelScope,
        onIntent = onIntent,
    )
}