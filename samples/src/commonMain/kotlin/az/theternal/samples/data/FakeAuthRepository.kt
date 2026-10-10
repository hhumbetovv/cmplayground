package az.theternal.samples.data

import kotlinx.coroutines.delay

/**
 * In-memory backends with latency, so every sample shows its loading and its races. Each has one
 * deterministic way to fail, to show the error paths: the hints are on the screens.
 */
class FakeAuthRepository(
    private val latency: Long = 800,
) : AuthRepository {
    override suspend fun signIn(
        email: String,
        password: String,
    ): SignInResult {
        delay(latency)
        return when {
            password != PASSWORD -> SignInResult.WrongCredentials
            email == PLAIN_ACCOUNT -> SignInResult.Success
            email == TWO_FACTOR_ACCOUNT -> SignInResult.CodeRequired(challengeId = "challenge-1")
            else -> SignInResult.WrongCredentials
        }
    }

    override suspend fun verifyCode(
        challengeId: String,
        code: String,
    ): Boolean {
        delay(latency)
        return code == CODE
    }

    override suspend fun resendCode(challengeId: String) {
        delay(latency)
    }

    companion object {
        const val PLAIN_ACCOUNT = "ada@example.com"
        const val TWO_FACTOR_ACCOUNT = "alan@example.com"
        const val PASSWORD = "password"
        const val CODE = "123456"
    }
}
