package az.theternal.core.framework.delegates.effect_producer

interface EffectProducer<Effect : ViewEffect> {
    val effectHandler: EffectHandler<Effect>
}
