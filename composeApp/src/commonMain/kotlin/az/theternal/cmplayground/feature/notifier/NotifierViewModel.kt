package az.theternal.cmplayground.feature.notifier

import androidx.lifecycle.ViewModel
import az.theternal.cmplayground.feature.NotifyEvent
import az.theternal.cmplayground.feature.notifier.NotifierContract.*
import az.theternal.core.framework.event_bus.emitter.EventEmitter
import az.theternal.core.framework.event_bus.emitter.EventEmitterHandler
import az.theternal.core.framework.event_bus.emitter.fireEvent
import az.theternal.core.framework.intent_consumer.IntentConsumer
import az.theternal.core.framework.intent_consumer.IntentHandler

class NotifierViewModel : ViewModel(), IntentConsumer<Intent>, EventEmitter {
    override val eventEmitterHandler: EventEmitterHandler = EventEmitterHandler()

    override val intentHandler: IntentHandler<Intent> = IntentHandler { intent ->
        when(intent) {
            Intent.Notify -> {
                fireEvent(NotifyEvent)
            }
        }
    }
}