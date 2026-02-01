package az.theternal.core.framework.delegates.intent_consumer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

context(viewModel: ViewModel, _: IntentConsumer<Intent>)
fun <Intent : ViewIntent> IntentDelegate(
    onIntent: (Intent) -> Unit,
): IntentDelegate<Intent> {
    return IntentDelegate(
        scope = viewModel.viewModelScope,
        onIntent = onIntent,
    )
}

fun <Intent : ViewIntent> IntentConsumer<Intent>.postIntent(
    intent: Intent
) {
    intentDelegate.postIntent(intent)
}