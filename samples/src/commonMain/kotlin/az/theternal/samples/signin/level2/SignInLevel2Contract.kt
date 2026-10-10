package az.theternal.samples.signin.level2

import az.theternal.mvi.Policy
import az.theternal.samples.signin.level1.SignInLevel1State

/**
 * Level 2: the same state as level 1, the concurrency moved into policies. The hand-kept `Job`s are gone —
 * a countdown is an action under [Policy.Latest], so starting one cancels the one before and a countdown
 * to 0 stops it — and a double tap on submit is [Policy.Ignore]ped by name.
 *
 * What level 2 does not fix: the flags. `isSubmitting`, `isVerifying`, `lockedSeconds` and `step` still
 * have to be read together in every guard. That is the level 3 signal.
 */
typealias SignInLevel2State = SignInLevel1State

sealed interface SignInLevel2Action {
    data class SignIn(
        val email: String,
        val password: String,
    ) : SignInLevel2Action

    data class Verify(
        val challengeId: String,
        val code: String,
    ) : SignInLevel2Action

    data class Resend(
        val challengeId: String,
    ) : SignInLevel2Action

    /** Counts [seconds] down to 0; one per [kind] runs, and a new one replaces it — 0 just stops it. */
    data class Countdown(
        val kind: Kind,
        val seconds: Int,
    ) : SignInLevel2Action {
        enum class Kind { Lock, Resend }
    }
}
