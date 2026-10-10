package az.theternal.mvi

/** When a store runs its logic's `onStart`. */
enum class Start {
    /** When the store is created. */
    Eagerly,

    /**
     * On first use: the first intent, or the first collector of the state or of the effects. A store that
     * is created but never shown does no work. The default, as in Orbit.
     */
    Lazily,

    /** Only when [StoreHost.start] is called. For tests that check the state before the logic starts. */
    Manually,
}

/**
 * Sees everything that goes through a store, in order: for logs in debug builds, for analytics, and for
 * the test DSL. Named [Partial]s are why logs read well: `SearchPartial.Results`, not `<lambda>`.
 */
interface StoreObserver {
    fun onIntent(intent: Any?) {}

    /** Called only when the state changed. */
    fun onState(
        partial: Partial<*>,
        previous: Any?,
        state: Any?,
    ) {}

    fun onEffect(effect: Any?) {}
}

/**
 * How a store runs, as opposed to what it does — the part an app sets once, for every store, through
 * its base ViewModel or its DI.
 */
class StoreSettings(
    val start: Start = Start.Lazily,
    /**
     * Gets every failure: in an intent, and in work the logic launched. The default fails fast — it
     * throws, so a bug crashes in development and in tests. An app sets one that reports instead, and the
     * store then keeps working.
     */
    val onError: (Throwable) -> Unit = { error -> throw error },
    /** Every observer sees everything, in the order of the list. One that fails is reported to [onError]. */
    val observers: List<StoreObserver> = emptyList(),
)
