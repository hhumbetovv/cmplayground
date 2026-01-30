package az.theternal.core.framework.delegates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

interface ViewIntent

interface IntentHandler<Intent : ViewIntent> {
    val intentProcessor: IntentProcessor<Intent>

    fun postIntent(newIntent: Intent) = intentProcessor::postIntent
}

class IntentProcessor<Intent : ViewIntent> internal constructor(
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

context(viewModel: ViewModel)
fun <Intent : ViewIntent> IntentHandler<Intent>.IntentProcessor(
    onIntent: (Intent) -> Unit,
): IntentProcessor<Intent> {
    return IntentProcessor(
        scope = viewModel.viewModelScope,
        onIntent = onIntent,
    )
}