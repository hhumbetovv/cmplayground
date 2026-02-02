package az.theternal.core.framework.intent_consumer

interface ViewIntent

interface IntentConsumer<Intent : ViewIntent> {
    val intentHandler: IntentHandler<Intent>
}
