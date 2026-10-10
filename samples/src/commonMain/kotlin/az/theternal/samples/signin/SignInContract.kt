package az.theternal.samples.signin

import az.theternal.mvi.ViewEffect
import az.theternal.mvi.ViewIntent

/**
 * The sign-in screen's contract. It is the same for the three implementations — level 1, 2 and 3 — and so
 * is the screen: each level keeps its own state and maps it onto [SignInUi]. Moving between levels
 * changes the logic and its mapper, nothing the user sees, and one behaviour test runs against all three.
 */
sealed interface SignInIntent : ViewIntent {
    data class EditEmail(
        val value: String,
    ) : SignInIntent

    data class EditPassword(
        val value: String,
    ) : SignInIntent

    data object Submit : SignInIntent

    data class EditCode(
        val value: String,
    ) : SignInIntent

    data object Verify : SignInIntent

    data object Resend : SignInIntent

    data object Back : SignInIntent
}

sealed interface SignInEffect : ViewEffect {
    data object SignedIn : SignInEffect
}

enum class SignInStep { Credentials, Code }

/** What the screen draws. */
data class SignInUi(
    val step: SignInStep = SignInStep.Credentials,
    val email: String = "",
    val password: String = "",
    val code: String = "",
    val error: String? = null,
    val isBusy: Boolean = false,
    val canEditCredentials: Boolean = true,
    val lockedSeconds: Int = 0,
    val resendSeconds: Int = 0,
)
