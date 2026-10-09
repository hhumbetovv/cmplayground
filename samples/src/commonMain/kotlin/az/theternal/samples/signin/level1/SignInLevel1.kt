package az.theternal.samples.signin.level1

import az.theternal.mvi.Logic
import az.theternal.mvi.StoreScope
import az.theternal.mvi.launch
import az.theternal.mvi.safeCall
import az.theternal.mvi.setState
import az.theternal.samples.data.AuthRepository
import az.theternal.samples.data.SignInResult
import az.theternal.samples.signin.SignInEffect
import az.theternal.samples.signin.SignInIntent
import az.theternal.samples.signin.SignInMessages
import az.theternal.samples.signin.SignInRules
import az.theternal.samples.signin.SignInStep
import az.theternal.samples.signin.SignInUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

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

fun SignInLevel1State.toUi() = SignInUi(
    step = step,
    email = email,
    password = password,
    code = code,
    error = error,
    isBusy = isSubmitting || isVerifying || isSignedIn,
    canEditCredentials = canEditCredentials,
    lockedSeconds = lockedSeconds,
    resendSeconds = resendSeconds,
)

class SignInLevel1Logic(
    private val auth: AuthRepository,
) : Logic<SignInLevel1State, SignInIntent, SignInEffect> {
    private var lockCountdown: Job? = null
    private var resendCountdown: Job? = null

    override fun StoreScope<SignInLevel1State, SignInEffect>.onIntent(intent: SignInIntent) {
        when (intent) {
            is SignInIntent.EditEmail -> if (state.canEditCredentials) setState { copy(email = intent.value, error = null) }
            is SignInIntent.EditPassword -> if (state.canEditCredentials) setState { copy(password = intent.value, error = null) }
            SignInIntent.Submit -> submit()
            is SignInIntent.EditCode -> if (state.canEditCode) setState { copy(code = intent.value, error = null) }
            SignInIntent.Verify -> verify()
            SignInIntent.Resend -> resend()
            SignInIntent.Back -> back()
        }
    }

    private fun StoreScope<SignInLevel1State, SignInEffect>.submit() {
        if (!state.canEditCredentials) return
        val email = state.email.trim()
        val password = state.password
        if (email.isEmpty() || password.isEmpty()) {
            setState { copy(error = SignInMessages.BLANK) }
            return
        }
        setState { copy(isSubmitting = true, error = null) }
        launch {
            val result = safeCall { auth.signIn(email, password) }
            setState { copy(isSubmitting = false) }
            result
                .onSuccess { answer -> onSignInAnswer(answer) }
                .onFailure { setState { copy(error = SignInMessages.GENERIC) } }
        }
    }

    private fun StoreScope<SignInLevel1State, SignInEffect>.onSignInAnswer(answer: SignInResult) {
        when (answer) {
            SignInResult.Success -> signedIn()
            is SignInResult.CodeRequired -> {
                setState {
                    copy(step = SignInStep.Code, challengeId = answer.challengeId, code = "", failedAttempts = 0)
                }
                startResendCountdown()
            }
            SignInResult.WrongCredentials -> {
                val attempts = state.failedAttempts + 1
                if (attempts >= SignInRules.MAX_ATTEMPTS) {
                    lock()
                } else {
                    setState { copy(failedAttempts = attempts, error = SignInMessages.WRONG_CREDENTIALS) }
                }
            }
        }
    }

    private fun StoreScope<SignInLevel1State, SignInEffect>.lock() {
        setState {
            copy(failedAttempts = 0, lockedSeconds = SignInRules.LOCK_SECONDS, error = SignInMessages.TOO_MANY_ATTEMPTS)
        }
        lockCountdown?.cancel()
        lockCountdown = launch {
            while (state.lockedSeconds > 0) {
                delay(SignInRules.TICK_MILLIS)
                setState { copy(lockedSeconds = lockedSeconds - 1) }
            }
            setState { copy(error = null) }
        }
    }

    private fun StoreScope<SignInLevel1State, SignInEffect>.startResendCountdown() {
        resendCountdown?.cancel()
        setState { copy(resendSeconds = SignInRules.RESEND_SECONDS) }
        resendCountdown = launch {
            while (state.resendSeconds > 0) {
                delay(SignInRules.TICK_MILLIS)
                setState { copy(resendSeconds = resendSeconds - 1) }
            }
        }
    }

    private fun StoreScope<SignInLevel1State, SignInEffect>.verify() {
        if (!state.canEditCode) return
        val challengeId = state.challengeId ?: return
        val code = state.code
        if (code.length != SignInRules.CODE_LENGTH) {
            setState { copy(error = SignInMessages.CODE_LENGTH) }
            return
        }
        setState { copy(isVerifying = true, error = null) }
        launch {
            val result = safeCall { auth.verifyCode(challengeId, code) }
            setState { copy(isVerifying = false) }
            result
                .onSuccess { accepted ->
                    if (accepted) signedIn() else setState { copy(error = SignInMessages.WRONG_CODE) }
                }
                .onFailure { setState { copy(error = SignInMessages.GENERIC) } }
        }
    }

    private fun StoreScope<SignInLevel1State, SignInEffect>.resend() {
        if (!state.canEditCode || state.resendSeconds > 0) return
        val challengeId = state.challengeId ?: return
        startResendCountdown()
        launch { safeCall { auth.resendCode(challengeId) } }
    }

    private fun StoreScope<SignInLevel1State, SignInEffect>.back() {
        if (!state.canEditCode) return
        resendCountdown?.cancel()
        setState {
            copy(step = SignInStep.Credentials, code = "", challengeId = null, resendSeconds = 0, error = null)
        }
    }

    private fun StoreScope<SignInLevel1State, SignInEffect>.signedIn() {
        resendCountdown?.cancel()
        setState { copy(isSignedIn = true) }
        emit(SignInEffect.SignedIn)
    }
}
