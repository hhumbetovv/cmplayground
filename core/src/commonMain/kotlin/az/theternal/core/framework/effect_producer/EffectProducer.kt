package az.theternal.core.framework.effect_producer

interface ViewEffect

interface EffectProducer<Effect : ViewEffect> {
    val effectHandler: EffectHandler<Effect>
}
