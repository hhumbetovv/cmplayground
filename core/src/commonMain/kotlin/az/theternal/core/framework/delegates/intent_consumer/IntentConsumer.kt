package az.theternal.core.framework.delegates.intent_consumer

interface IntentConsumer<Intent : ViewIntent> {
    val intentHandler: IntentHandler<Intent>
}
