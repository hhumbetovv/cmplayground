package az.theternal.core.framework.delegates.intent_consumer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

context(viewModel: ViewModel, _: IntentConsumer<Intent>)
fun <Intent : ViewIntent> IntentHandler(
    onIntent: (Intent) -> Unit,
): IntentHandler<Intent> {
    return IntentHandler(
        scope = viewModel.viewModelScope,
        onIntent = onIntent,
    )
}

fun <Intent : ViewIntent> IntentConsumer<Intent>.postIntent(
    intent: Intent
) {
    intentHandler.postIntent(intent)
}