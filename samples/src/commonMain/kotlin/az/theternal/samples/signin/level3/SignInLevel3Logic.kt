package az.theternal.samples.signin.level3

import az.theternal.mvi.Logic
import az.theternal.mvi.machineLogic
import az.theternal.mvi.safeCall
import az.theternal.samples.data.AuthRepository
import az.theternal.samples.signin.SignInEffect
import az.theternal.samples.signin.SignInIntent
import az.theternal.samples.signin.SignInRules
import dev.klitsie.statemachine.StateMachine
import kotlinx.coroutines.delay

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
            onEvent<SignInEvent.Submit> { editing, _ -> editing.submitted() }
        }

        state<SignInPhase.Submitting> {
            sideEffect(key = { Unit }) { submitting ->
                send(SignInEvent.SignInAnswered(safeCall { auth.signIn(submitting.email, submitting.password) }))
            }
            onEvent<SignInEvent.SignInAnswered> { submitting, event ->
                submitting.answered(event.result).also { next -> if (next == SignInPhase.SignedIn) trigger(SignInEffect.SignedIn) }
            }
        }

        state<SignInPhase.Locked> {
            sideEffect(key = { Unit }) { tickForever() }
            onEvent<SignInEvent.Tick> { locked, _ -> locked.ticked() }
        }

        nestedState<SignInPhase.Code> {
            // Restarts when a new countdown begins, stops at 0; one ticker whatever the phase inside.
            sideEffect(key = { it.resendSeconds > 0 }) { code -> if (code.resendSeconds > 0) tickForever() }
            sideEffect(key = { it.resends }) { code ->
                if (code.resends > 0) safeCall { auth.resendCode(code.challengeId) }
            }
            onEvent<SignInEvent.Tick> { code, _ -> code.ticked() }

            state<SignInPhase.EnteringCode> {
                onEvent<SignInEvent.CodeEdited> { entering, event -> entering.copy(code = event.value, error = null) }
                onEvent<SignInEvent.Verify> { entering, _ -> entering.verified() }
                onEvent<SignInEvent.Resend> { entering, _ -> entering.resent() }
                onEvent<SignInEvent.Back> { entering, _ -> SignInPhase.Editing(entering.email, entering.password) }
            }

            state<SignInPhase.Verifying> {
                sideEffect(key = { Unit }) { verifying ->
                    send(SignInEvent.CodeAnswered(safeCall { auth.verifyCode(verifying.challengeId, verifying.code) }))
                }
                onEvent<SignInEvent.CodeAnswered> { verifying, event ->
                    verifying.answered(event.result).also { next -> if (next == SignInPhase.SignedIn) trigger(SignInEffect.SignedIn) }
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
