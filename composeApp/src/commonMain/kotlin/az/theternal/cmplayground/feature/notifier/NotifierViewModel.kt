package az.theternal.cmplayground.feature.notifier

import androidx.lifecycle.ViewModel
import az.theternal.cmplayground.feature.NotifyEvent
import az.theternal.cmplayground.feature.notifier.NotifierContract.*
import az.theternal.core.framework.delegates.event_bus.emitter.EventEmitter
import az.theternal.core.framework.delegates.event_bus.emitter.EventEmitterDelegate
import az.theternal.core.framework.delegates.intent_consumer.IntentConsumer
import az.theternal.core.framework.delegates.intent_consumer.IntentDelegate

class NotifierViewModel : ViewModel(), IntentConsumer<Intent>, EventEmitter {
    override val eventEmitterDelegate: EventEmitterDelegate = EventEmitterDelegate()

    override val intentDelegate: IntentDelegate<Intent> = IntentDelegate { intent ->
        when(intent) {
            Intent.Notify -> {
                eventEmitterDelegate.fireEvent(NotifyEvent)
            }
        }
    }

}