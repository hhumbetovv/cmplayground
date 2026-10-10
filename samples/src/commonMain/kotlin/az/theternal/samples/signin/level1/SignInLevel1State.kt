package az.theternal.samples.signin.level1

import az.theternal.samples.signin.SignInStep
import kotlinx.coroutines.Job

/**
 * Level 1, past its limit — on purpose. It works (the behaviour test passes), and this is what it costs:
 * five flags whose combinations are mostly impossible (`isSubmitting` and `isVerifying`; `lockedSeconds`
 * on the code step), a guard at the top of every handler, and two `Job`s kept by hand so that two
 * countdowns never tick at once. Every new rule touches all of them.
 *
 * The signals that said "move up": a second request that can overlap the first (level 2), and flags that
 * exclude each other (level 3).
 */
data class SignInLevel1State(
    val step: SignInStep = SignInStep.Credentials,
    val email: String = "",
    val password: String = "",
    val code: String = "",
    val challengeId: String? = null,
    val error: String? = null,
    val isSubmitting: Boolean = false,
    val isVerifying: Boolean = false,
    val isSignedIn: Boolean = false,
    val failedAttempts: Int = 0,
    val lockedSeconds: Int = 0,
    val resendSeconds: Int = 0,
) {
    val canEditCredentials: Boolean
        get() = step == SignInStep.Credentials && !isSubmitting && !isSignedIn && lockedSeconds == 0

    val canEditCode: Boolean
        get() = step == SignInStep.Code && !isVerifying && !isSignedIn
}
