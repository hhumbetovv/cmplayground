package az.theternal.samples.signin.level1

import az.theternal.samples.signin.SignInUi

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
