package az.theternal.samples.signin

import az.theternal.mvi.Logic
import az.theternal.mvi.ViewEffect
import az.theternal.mvi.ViewIntent
import az.theternal.samples.data.AuthRepository

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

object SignInRules {
    const val MAX_ATTEMPTS = 3
    const val LOCK_SECONDS = 60
    const val RESEND_SECONDS = 30
    const val CODE_LENGTH = 6
    const val TICK_MILLIS = 1_000L
}

object SignInMessages {
    const val BLANK = "Enter your email and password"
    const val WRONG_CREDENTIALS = "Wrong email or password"
    const val TOO_MANY_ATTEMPTS = "Too many attempts"
    const val CODE_LENGTH = "Enter the 6-digit code"
    const val WRONG_CODE = "Wrong code"
    const val GENERIC = "Something went wrong, try again"
}

/** One implementation of the screen: its state, its logic, and its mapper onto [SignInUi]. */
class SignInVersion<S>(
    val title: String,
    val initialState: S,
    val logic: (AuthRepository) -> Logic<S, SignInIntent, SignInEffect>,
    val toUi: (S) -> SignInUi,
)
