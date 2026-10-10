package az.theternal.samples.data

interface AuthRepository {
    suspend fun signIn(
        email: String,
        password: String,
    ): SignInResult

    suspend fun verifyCode(
        challengeId: String,
        code: String,
    ): Boolean

    suspend fun resendCode(challengeId: String)
}
