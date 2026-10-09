package az.theternal.samples.data

import kotlinx.coroutines.flow.Flow

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

interface UserRepository {
    suspend fun search(query: String): List<User>

    /** Sets, not toggles: sending the latest wish again is harmless, which is what lets a tap cancel the last. */
    suspend fun setFollowing(
        userId: String,
        following: Boolean,
    )
}

interface PostRepository {
    suspend fun feed(): List<Post>

    /** Sets the like and answers the post's like count. */
    suspend fun setLiked(
        postId: String,
        liked: Boolean,
    ): Int

    suspend fun report(
        postId: String,
        reason: ReportReason,
        comment: String,
    )

    suspend fun searchTags(query: String): List<String>

    /** Uploads the draft's media, reporting progress from 0 to 1. */
    fun upload(draft: PostDraft): Flow<Float>

    /** Publishes the draft and answers the new post's id. */
    suspend fun publish(draft: PostDraft): String
}
