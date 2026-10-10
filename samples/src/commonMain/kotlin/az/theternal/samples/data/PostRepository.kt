package az.theternal.samples.data

import kotlinx.coroutines.flow.Flow

interface PostRepository {
    suspend fun feed(): List<Post>

    /** Sets the like and answers the post's like count. */
    suspend fun setLiked(
        postId: String,
        liked: Boolean,
    ): Int

    /** The post's like count, live: other people like it too. Cold — it costs only while collected. */
    fun likes(postId: String): Flow<Int>

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
