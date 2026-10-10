package az.theternal.samples.signin

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
