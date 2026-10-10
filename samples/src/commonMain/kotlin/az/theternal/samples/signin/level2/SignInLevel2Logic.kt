package az.theternal.samples.signin.level2

import az.theternal.mvi.ActionLogic
import az.theternal.mvi.Policy
import az.theternal.mvi.StoreScope
import az.theternal.mvi.safeCall
import az.theternal.mvi.setState
import az.theternal.samples.data.AuthRepository
import az.theternal.samples.data.SignInResult
import az.theternal.samples.signin.SignInEffect
import az.theternal.samples.signin.SignInIntent
import az.theternal.samples.signin.SignInMessages
import az.theternal.samples.signin.SignInRules
import az.theternal.samples.signin.SignInStep
import kotlinx.coroutines.delay

class SignInLevel2Logic(
    private val auth: AuthRepository,
) : ActionLogic<SignInLevel2State, SignInIntent, SignInLevel2Action, SignInEffect>() {
    private typealias Scope = StoreScope<SignInLevel2State, SignInEffect>

    override fun Scope.toAction(intent: SignInIntent): SignInLevel2Action? {
        return when (intent) {
            is SignInIntent.EditEmail -> {
                if (state.canEditCredentials) setState { copy(email = intent.value, error = null) }
                null
            }
            is SignInIntent.EditPassword -> {
                if (state.canEditCredentials) setState { copy(password = intent.value, error = null) }
                null
            }
            is SignInIntent.EditCode -> {
                if (state.canEditCode) setState { copy(code = intent.value, error = null) }
                null
            }
            SignInIntent.Submit -> signIn()
            SignInIntent.Verify -> verify()
            SignInIntent.Resend -> {
                val challengeId = state.challengeId
                if (!state.canEditCode || state.resendSeconds > 0 || challengeId == null) return null
                SignInLevel2Action.Resend(challengeId)
            }
            SignInIntent.Back -> {
                if (!state.canEditCode) return null
                setState { copy(step = SignInStep.Credentials, code = "", challengeId = null, error = null) }
                SignInLevel2Action.Countdown(SignInLevel2Action.Countdown.Kind.Resend, seconds = 0)
            }
        }
    }

    /** The parameter-gathering step: validation, and the values the request will be sent with. */
    private fun Scope.signIn(): SignInLevel2Action? {
        if (!state.canEditCredentials) return null
        val email = state.email.trim()
        if (email.isEmpty() || state.password.isEmpty()) {
            setState { copy(error = SignInMessages.BLANK) }
            return null
        }
        setState { copy(isSubmitting = true, error = null) }
        return SignInLevel2Action.SignIn(email = email, password = state.password)
    }

    private fun Scope.verify(): SignInLevel2Action? {
        val challengeId = state.challengeId
        if (!state.canEditCode || challengeId == null) return null
        if (state.code.length != SignInRules.CODE_LENGTH) {
            setState { copy(error = SignInMessages.CODE_LENGTH) }
            return null
        }
        setState { copy(isVerifying = true, error = null) }
        return SignInLevel2Action.Verify(challengeId = challengeId, code = state.code)
    }

    override fun policy(action: SignInLevel2Action): Policy {
        return when (action) {
            is SignInLevel2Action.Countdown -> Policy.Latest
            is SignInLevel2Action.SignIn, is SignInLevel2Action.Verify, is SignInLevel2Action.Resend -> Policy.Drop
        }
    }

    override fun key(action: SignInLevel2Action): Any {
        return if (action is SignInLevel2Action.Countdown) action.kind else action::class
    }

    override suspend fun Scope.process(action: SignInLevel2Action) {
        when (action) {
            is SignInLevel2Action.SignIn -> {
                val result = safeCall { auth.signIn(action.email, action.password) }
                setState { copy(isSubmitting = false) }
                result
                    .onSuccess { answer -> onSignInAnswer(answer) }
                    .onFailure { setState { copy(error = SignInMessages.GENERIC) } }
            }
            is SignInLevel2Action.Verify -> {
                val result = safeCall { auth.verifyCode(action.challengeId, action.code) }
                setState { copy(isVerifying = false) }
                result
                    .onSuccess { accepted ->
                        if (accepted) signedIn() else setState { copy(error = SignInMessages.WRONG_CODE) }
                    }
                    .onFailure { setState { copy(error = SignInMessages.GENERIC) } }
            }
            is SignInLevel2Action.Resend -> {
                dispatch(SignInLevel2Action.Countdown(SignInLevel2Action.Countdown.Kind.Resend, SignInRules.RESEND_SECONDS))
                safeCall { auth.resendCode(action.challengeId) }
            }
            is SignInLevel2Action.Countdown -> countDown(action)
        }
    }

    private fun Scope.onSignInAnswer(answer: SignInResult) {
        when (answer) {
            SignInResult.Success -> signedIn()
            is SignInResult.CodeRequired -> {
                setState {
                    copy(
                        step = SignInStep.Code,
                        challengeId = answer.challengeId,
                        code = "",
                        failedAttempts = 0,
                        resendSeconds = SignInRules.RESEND_SECONDS,
                    )
                }
                dispatch(SignInLevel2Action.Countdown(SignInLevel2Action.Countdown.Kind.Resend, SignInRules.RESEND_SECONDS))
            }
            SignInResult.WrongCredentials -> {
                val attempts = state.failedAttempts + 1
                if (attempts >= SignInRules.MAX_ATTEMPTS) {
                    setState {
                        copy(
                            failedAttempts = 0,
                            lockedSeconds = SignInRules.LOCK_SECONDS,
                            error = SignInMessages.TOO_MANY_ATTEMPTS,
                        )
                    }
                    dispatch(SignInLevel2Action.Countdown(SignInLevel2Action.Countdown.Kind.Lock, SignInRules.LOCK_SECONDS))
                } else {
                    setState { copy(failedAttempts = attempts, error = SignInMessages.WRONG_CREDENTIALS) }
                }
            }
        }
    }

    private suspend fun Scope.countDown(action: SignInLevel2Action.Countdown) {
        fun SignInLevel2State.withSeconds(seconds: Int) = when (action.kind) {
            SignInLevel2Action.Countdown.Kind.Lock -> copy(lockedSeconds = seconds)
            SignInLevel2Action.Countdown.Kind.Resend -> copy(resendSeconds = seconds)
        }
        setState { withSeconds(action.seconds) }
        for (left in action.seconds - 1 downTo 0) {
            delay(SignInRules.TICK_MILLIS)
            setState { withSeconds(left) }
        }
        if (action.kind == SignInLevel2Action.Countdown.Kind.Lock && action.seconds > 0) {
            setState { copy(error = null) }
        }
    }

    private fun Scope.signedIn() {
        dispatch(SignInLevel2Action.Countdown(SignInLevel2Action.Countdown.Kind.Resend, seconds = 0))
        setState { copy(isSignedIn = true) }
        emit(SignInEffect.SignedIn)
    }
}
