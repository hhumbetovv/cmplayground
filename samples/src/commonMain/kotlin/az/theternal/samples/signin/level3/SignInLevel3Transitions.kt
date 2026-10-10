package az.theternal.samples.signin.level3

import az.theternal.samples.data.SignInResult
import az.theternal.samples.signin.SignInMessages
import az.theternal.samples.signin.SignInRules

// The transitions, as pure functions: the rules of the screen, with no coroutines in them. SignInLevel3Logic wires
// them to intents and starts a phase's work when the screen enters it.

internal fun SignInPhase.Editing.submitted(): SignInPhase {
    return if (email.isBlank() || password.isEmpty()) {
        copy(error = SignInMessages.BLANK)
    } else {
        SignInPhase.Submitting(email.trim(), password, failedAttempts)
    }
}

internal fun SignInPhase.Submitting.answered(result: Result<SignInResult>): SignInPhase {
    val answer = result.getOrElse { return SignInPhase.Editing(email, password, failedAttempts, SignInMessages.GENERIC) }
    return when (answer) {
        SignInResult.Success -> SignInPhase.SignedIn
        is SignInResult.CodeRequired -> SignInPhase.EnteringCode(email = email, password = password, challengeId = answer.challengeId)
        SignInResult.WrongCredentials -> {
            val attempts = failedAttempts + 1
            if (attempts >= SignInRules.MAX_ATTEMPTS) {
                SignInPhase.Locked(email, password, SignInRules.LOCK_SECONDS)
            } else {
                SignInPhase.Editing(email, password, attempts, SignInMessages.WRONG_CREDENTIALS)
            }
        }
    }
}

internal fun SignInPhase.Locked.ticked(): SignInPhase {
    return if (secondsLeft <= 1) SignInPhase.Editing(email, password) else copy(secondsLeft = secondsLeft - 1)
}

internal fun SignInPhase.Code.ticked(): SignInPhase.Code {
    val seconds = maxOf(0, resendSeconds - 1)
    return when (this) {
        is SignInPhase.EnteringCode -> copy(resendSeconds = seconds)
        is SignInPhase.Verifying -> copy(resendSeconds = seconds)
    }
}

internal fun SignInPhase.EnteringCode.verified(): SignInPhase {
    return if (code.length != SignInRules.CODE_LENGTH) {
        copy(error = SignInMessages.CODE_LENGTH)
    } else {
        SignInPhase.Verifying(
            email = email,
            password = password,
            challengeId = challengeId,
            code = code,
            resendSeconds = resendSeconds,
            resends = resends,
        )
    }
}

/** Unchanged while the countdown runs: a new code can be asked for only after it. */
internal fun SignInPhase.EnteringCode.resent(): SignInPhase.EnteringCode {
    return if (resendSeconds > 0) this else copy(resendSeconds = SignInRules.RESEND_SECONDS, resends = resends + 1)
}

internal fun SignInPhase.Verifying.answered(result: Result<Boolean>): SignInPhase {
    return if (result.getOrNull() == true) {
        SignInPhase.SignedIn
    } else {
        SignInPhase.EnteringCode(
            email = email,
            password = password,
            challengeId = challengeId,
            code = code,
            resendSeconds = resendSeconds,
            resends = resends,
            error = if (result.isFailure) SignInMessages.GENERIC else SignInMessages.WRONG_CODE,
        )
    }
}
