package az.theternal.samples.signin.level3

import az.theternal.samples.signin.SignInRules

/**
 * Level 3: the screen is in exactly one phase, and each phase carries only what it has. There is no
 * `isSubmitting` to forget — Submitting is a phase — and no guards: an intent a phase does not handle
 * (editing the email while Submitting, Resend while Verifying) is simply not in its graph.
 *
 * Work belongs to phases: Submitting's request, Locked's countdown and the code step's resend countdown
 * are side effects that start on entering their phase and are cancelled on leaving it. The two countdowns
 * of level 1 and their `Job`s are gone, and so is the question of which one is running.
 */
sealed interface SignInPhase {
    sealed interface Credentials : SignInPhase {
        val email: String
        val password: String
    }

    data class Editing(
        override val email: String = "",
        override val password: String = "",
        val failedAttempts: Int = 0,
        val error: String? = null,
    ) : Credentials

    data class Submitting(
        override val email: String,
        override val password: String,
        val failedAttempts: Int,
    ) : Credentials

    data class Locked(
        override val email: String,
        override val password: String,
        val secondsLeft: Int,
    ) : Credentials

    /** The code step. Its countdown runs across both phases, so verifying does not pause it. */
    sealed interface Code : SignInPhase {
        val email: String
        val password: String
        val challengeId: String
        val code: String
        val resendSeconds: Int

        /** How many times a new code was asked for: the resend side effect runs once per value. */
        val resends: Int
    }

    data class EnteringCode(
        override val email: String,
        override val password: String,
        override val challengeId: String,
        override val code: String = "",
        override val resendSeconds: Int = SignInRules.RESEND_SECONDS,
        override val resends: Int = 0,
        val error: String? = null,
    ) : Code

    data class Verifying(
        override val email: String,
        override val password: String,
        override val challengeId: String,
        override val code: String,
        override val resendSeconds: Int,
        override val resends: Int,
    ) : Code

    data object SignedIn : SignInPhase
}
