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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class SignInLevel1Logic(
    private val auth: AuthRepository,
) : Logic<SignInLevel1State, SignInIntent, SignInEffect> {
    private typealias Scope = StoreScope<SignInLevel1State, SignInEffect>

    private var lockCountdown: Job? = null
    private var resendCountdown: Job? = null

    override fun Scope.onIntent(intent: SignInIntent) {
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

    private fun Scope.submit() {
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

    private fun Scope.onSignInAnswer(answer: SignInResult) {
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

    private fun Scope.lock() {
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

    private fun Scope.startResendCountdown() {
        resendCountdown?.cancel()
        setState { copy(resendSeconds = SignInRules.RESEND_SECONDS) }
        resendCountdown = launch {
            while (state.resendSeconds > 0) {
                delay(SignInRules.TICK_MILLIS)
                setState { copy(resendSeconds = resendSeconds - 1) }
            }
        }
    }

    private fun Scope.verify() {
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

    private fun Scope.resend() {
        if (!state.canEditCode || state.resendSeconds > 0) return
        val challengeId = state.challengeId ?: return
        startResendCountdown()
        launch { safeCall { auth.resendCode(challengeId) } }
    }

    private fun Scope.back() {
        if (!state.canEditCode) return
        resendCountdown?.cancel()
        setState {
            copy(step = SignInStep.Credentials, code = "", challengeId = null, resendSeconds = 0, error = null)
        }
    }

    private fun Scope.signedIn() {
        resendCountdown?.cancel()
        setState { copy(isSignedIn = true) }
        emit(SignInEffect.SignedIn)
    }
}
