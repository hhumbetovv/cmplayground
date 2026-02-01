package az.theternal.cmplayground.feature.notifier

import az.theternal.core.framework.delegates.intent_consumer.ViewIntent

sealed interface NotifierContract {
    sealed interface Intent : ViewIntent {
        data object Notify : Intent
    }
}