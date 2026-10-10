package az.theternal.samples.signin.level3

import az.theternal.samples.signin.SignInMessages
import az.theternal.samples.signin.SignInStep
import az.theternal.samples.signin.SignInUi

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
