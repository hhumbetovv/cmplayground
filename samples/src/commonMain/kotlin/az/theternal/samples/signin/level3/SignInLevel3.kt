package az.theternal.samples.signin.level3

import az.theternal.mvi.Logic
import az.theternal.mvi.machineLogic
import az.theternal.mvi.safeCall
import az.theternal.samples.data.AuthRepository
import az.theternal.samples.data.SignInResult
import az.theternal.samples.signin.SignInEffect
import az.theternal.samples.signin.SignInIntent
import az.theternal.samples.signin.SignInMessages
import az.theternal.samples.signin.SignInRules
import az.theternal.samples.signin.SignInStep
import az.theternal.samples.signin.SignInUi
import dev.klitsie.statemachine.StateMachine
import kotlinx.coroutines.delay

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

fun SignInPhase.toUi(): SignInUi {
    return when (this) {
        is SignInPhase.Editing -> SignInUi(email = email, password = password, error = error)
        is SignInPhase.Submitting -> SignInUi(email = email, password = password, isBusy = true, canEditCredentials = false)
        is SignInPhase.Locked -> SignInUi(
            email = email,
            password = password,
            error = SignInMessages.TOO_MANY_ATTEMPTS,
            canEditCredentials = false,
            lockedSeconds = secondsLeft,
        )
        is SignInPhase.EnteringCode -> SignInUi(
            step = SignInStep.Code,
            email = email,
            password = password,
            code = code,
            error = error,
            canEditCredentials = false,
            resendSeconds = resendSeconds,
        )
        is SignInPhase.Verifying -> SignInUi(
            step = SignInStep.Code,
            email = email,
            password = password,
            code = code,
            isBusy = true,
            canEditCredentials = false,
            resendSeconds = resendSeconds,
        )
        SignInPhase.SignedIn -> SignInUi(isBusy = true, canEditCredentials = false)
    }
}

fun signInLevel3Logic(auth: AuthRepository): Logic<SignInPhase, SignInIntent, SignInEffect> {
    return machineLogic<SignInPhase, SignInIntent, SignInEvent, SignInEffect>(
        toEvent = { intent ->
            when (intent) {
                is SignInIntent.EditEmail -> SignInEvent.EmailEdited(intent.value)
                is SignInIntent.EditPassword -> SignInEvent.PasswordEdited(intent.value)
                SignInIntent.Submit -> SignInEvent.Submit
                is SignInIntent.EditCode -> SignInEvent.CodeEdited(intent.value)
                SignInIntent.Verify -> SignInEvent.Verify
                SignInIntent.Resend -> SignInEvent.Resend
                SignInIntent.Back -> SignInEvent.Back
            }
        },
    ) {
        state<SignInPhase.Editing> {
            onEvent<SignInEvent.EmailEdited> { editing, event -> editing.copy(email = event.value, error = null) }
            onEvent<SignInEvent.PasswordEdited> { editing, event -> editing.copy(password = event.value, error = null) }
            onEvent<SignInEvent.Submit> { editing, _ ->
                if (editing.email.isBlank() || editing.password.isEmpty()) {
                    editing.copy(error = SignInMessages.BLANK)
                } else {
                    SignInPhase.Submitting(editing.email.trim(), editing.password, editing.failedAttempts)
                }
            }
        }

        state<SignInPhase.Submitting> {
            sideEffect(key = { Unit }) { submitting ->
                send(SignInEvent.SignInAnswered(safeCall { auth.signIn(submitting.email, submitting.password) }))
            }
            onEvent<SignInEvent.SignInAnswered> { submitting, event ->
                val answer = event.result.getOrElse {
                    return@onEvent SignInPhase.Editing(
                        submitting.email,
                        submitting.password,
                        submitting.failedAttempts,
                        SignInMessages.GENERIC,
                    )
                }
                when (answer) {
                    SignInResult.Success -> {
                        trigger(SignInEffect.SignedIn)
                        SignInPhase.SignedIn
                    }
                    is SignInResult.CodeRequired -> SignInPhase.EnteringCode(
                        email = submitting.email,
                        password = submitting.password,
                        challengeId = answer.challengeId,
                    )
                    SignInResult.WrongCredentials -> {
                        val attempts = submitting.failedAttempts + 1
                        if (attempts >= SignInRules.MAX_ATTEMPTS) {
                            SignInPhase.Locked(submitting.email, submitting.password, SignInRules.LOCK_SECONDS)
                        } else {
                            SignInPhase.Editing(
                                submitting.email,
                                submitting.password,
                                attempts,
                                SignInMessages.WRONG_CREDENTIALS,
                            )
                        }
                    }
                }
            }
        }

        state<SignInPhase.Locked> {
            sideEffect(key = { Unit }) { tickForever() }
            onEvent<SignInEvent.Tick> { locked, _ ->
                if (locked.secondsLeft <= 1) {
                    SignInPhase.Editing(locked.email, locked.password)
                } else {
                    locked.copy(secondsLeft = locked.secondsLeft - 1)
                }
            }
        }

        nestedState<SignInPhase.Code> {
            // Restarts when a new countdown begins, stops at 0; one ticker whatever the phase inside.
            sideEffect(key = { it.resendSeconds > 0 }) { code -> if (code.resendSeconds > 0) tickForever() }
            sideEffect(key = { it.resends }) { code ->
                if (code.resends > 0) safeCall { auth.resendCode(code.challengeId) }
            }
            onEvent<SignInEvent.Tick> { code, _ -> code.withResendSeconds(maxOf(0, code.resendSeconds - 1)) }

            state<SignInPhase.EnteringCode> {
                onEvent<SignInEvent.CodeEdited> { entering, event -> entering.copy(code = event.value, error = null) }
                onEvent<SignInEvent.Verify> { entering, _ ->
                    if (entering.code.length != SignInRules.CODE_LENGTH) {
                        entering.copy(error = SignInMessages.CODE_LENGTH)
                    } else {
                        SignInPhase.Verifying(
                            email = entering.email,
                            password = entering.password,
                            challengeId = entering.challengeId,
                            code = entering.code,
                            resendSeconds = entering.resendSeconds,
                            resends = entering.resends,
                        )
                    }
                }
                onEvent<SignInEvent.Resend> { entering, _ ->
                    if (entering.resendSeconds > 0) {
                        entering
                    } else {
                        entering.copy(resendSeconds = SignInRules.RESEND_SECONDS, resends = entering.resends + 1)
                    }
                }
                onEvent<SignInEvent.Back> { entering, _ -> SignInPhase.Editing(entering.email, entering.password) }
            }

            state<SignInPhase.Verifying> {
                sideEffect(key = { Unit }) { verifying ->
                    send(SignInEvent.CodeAnswered(safeCall { auth.verifyCode(verifying.challengeId, verifying.code) }))
                }
                onEvent<SignInEvent.CodeAnswered> { verifying, event ->
                    if (event.result.getOrNull() == true) {
                        trigger(SignInEffect.SignedIn)
                        SignInPhase.SignedIn
                    } else {
                        SignInPhase.EnteringCode(
                            email = verifying.email,
                            password = verifying.password,
                            challengeId = verifying.challengeId,
                            code = verifying.code,
                            resendSeconds = verifying.resendSeconds,
                            resends = verifying.resends,
                            error = if (event.result.isFailure) SignInMessages.GENERIC else SignInMessages.WRONG_CODE,
                        )
                    }
                }
            }
        }
    }
}

private suspend fun StateMachine<SignInPhase, *, SignInEvent>.tickForever() {
    while (true) {
        delay(SignInRules.TICK_MILLIS)
        send(SignInEvent.Tick)
    }
}

private fun SignInPhase.Code.withResendSeconds(seconds: Int): SignInPhase.Code {
    return when (this) {
        is SignInPhase.EnteringCode -> copy(resendSeconds = seconds)
        is SignInPhase.Verifying -> copy(resendSeconds = seconds)
    }
}
