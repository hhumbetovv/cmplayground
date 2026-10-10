package az.theternal.samples.signin.level3

import az.theternal.samples.data.SignInResult

/** The machine's events: the user's, gathered from intents, and its own, which the screen cannot send. */
sealed interface SignInEvent {
    data class EmailEdited(
        val value: String,
    ) : SignInEvent

    data class PasswordEdited(
        val value: String,
    ) : SignInEvent

    data object Submit : SignInEvent

    data class CodeEdited(
        val value: String,
    ) : SignInEvent

    data object Verify : SignInEvent

    data object Resend : SignInEvent

    data object Back : SignInEvent

    data class SignInAnswered(
        val result: Result<SignInResult>,
    ) : SignInEvent

    data class CodeAnswered(
        val result: Result<Boolean>,
    ) : SignInEvent

    data object Tick : SignInEvent
}
