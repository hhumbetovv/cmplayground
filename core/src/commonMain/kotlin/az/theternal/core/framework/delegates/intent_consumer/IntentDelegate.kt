package az.theternal.core.framework.delegates.intent_consumer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

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
