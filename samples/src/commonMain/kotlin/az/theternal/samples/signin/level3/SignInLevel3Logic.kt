package az.theternal.samples.signin.level3

import az.theternal.mvi.Logic
import az.theternal.mvi.StoreScope
import az.theternal.mvi.runOn
import az.theternal.mvi.safeCall
import az.theternal.mvi.setState
import az.theternal.mvi.whileIn
import az.theternal.samples.data.AuthRepository
import az.theternal.samples.signin.SignInEffect
import az.theternal.samples.signin.SignInIntent
import az.theternal.samples.signin.SignInRules
import kotlinx.coroutines.delay

/**
 * Level 3. The phases are [SignInPhase] and the transitions are the pure functions in
 * `SignInLevel3Transitions.kt`; the wiring is plain code:
 * - an intent is handled by the phase the screen is in, in one `when`. A phase that does not list an
 *   intent ignores it, which is what the library's graph does;
 * - a phase's work is a `whileIn`: it starts on entering the phase and is cancelled on leaving it, and a
 *   result that arrives late cannot overwrite the phase the user moved to;
 * - the resend request is `runOn` the code step, so going back cancels it.
 *
 * No events and no `send`: a result goes straight into `setState`, and the graph is the `when` in `onIntent`
 * plus the `whileIn`s in `onStart`. The compiler checks the `when` over the sealed phase.
 */
class SignInLevel3Logic(
    private val auth: AuthRepository,
) : Logic<SignInPhase, SignInIntent, SignInEffect> {
    private typealias Scope = StoreScope<SignInPhase, SignInEffect>

    override fun Scope.onStart() {
        whileIn(SignInPhase.Submitting::class) { submitting ->
            val result = safeCall { auth.signIn(submitting.email, submitting.password) }
            if (setState { answered(result) } == SignInPhase.SignedIn) emit(SignInEffect.SignedIn)
        }
        whileIn(SignInPhase.Locked::class) {
            while (true) {
                delay(SignInRules.TICK_MILLIS)
                setState { ticked() }
            }
        }
        // One countdown for the whole code step: Verifying does not pause it. It restarts when a new
        // countdown begins and ends at 0.
        whileIn(SignInPhase.Code::class, key = { it.resendSeconds > 0 }) {
            while (state.resendSeconds > 0) {
                delay(SignInRules.TICK_MILLIS)
                setState { ticked() }
            }
        }
        whileIn(SignInPhase.Verifying::class) { verifying ->
            val result = safeCall { auth.verifyCode(verifying.challengeId, verifying.code) }
            if (setState { answered(result) } == SignInPhase.SignedIn) emit(SignInEffect.SignedIn)
        }
    }

    override fun Scope.onIntent(intent: SignInIntent) {
        when (val phase = state) {
            is SignInPhase.Editing -> when (intent) {
                is SignInIntent.EditEmail -> setState { phase.copy(email = intent.value, error = null) }
                is SignInIntent.EditPassword -> setState { phase.copy(password = intent.value, error = null) }
                SignInIntent.Submit -> setState { phase.submitted() }
                else -> Unit
            }
            is SignInPhase.EnteringCode -> when (intent) {
                is SignInIntent.EditCode -> setState { phase.copy(code = intent.value, error = null) }
                SignInIntent.Verify -> setState { phase.verified() }
                SignInIntent.Resend -> resend(phase)
                SignInIntent.Back -> setState { SignInPhase.Editing(phase.email, phase.password) }
                else -> Unit
            }
            // Busy, locked or done: these phases take no intents.
            is SignInPhase.Submitting, is SignInPhase.Locked, is SignInPhase.Verifying, SignInPhase.SignedIn -> Unit
        }
    }

    private fun Scope.resend(phase: SignInPhase.EnteringCode) {
        val next = phase.resent()
        if (next == phase) return
        setState { next }
        runOn(SignInPhase.Code::class) { code -> safeCall { auth.resendCode(code.challengeId) } }
    }
}
