package az.theternal.samples.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakePostRepository(
    private val latency: Long = 700,
) : PostRepository {
    private val posts = mutableListOf(
        Post("p1", "Ada Lovelace", "The engine might compose elaborate pieces of music.", likes = 12, isLiked = false),
        Post("p2", "Alan Turing", "We can only see a short distance ahead.", likes = 40, isLiked = true),
        Post("p3", "Grace Hopper", "It is easier to ask forgiveness than to get permission.", likes = 7, isLiked = false),
        Post("p4", "Barbara Liskov", "Abstraction is the key to managing complexity.", likes = 3, isLiked = false),
        Post("p5", "Edsger Dijkstra", "Simplicity is prerequisite for reliability.", likes = 51, isLiked = false),
        Post("p6", "Donald Knuth", "Premature optimization is the root of all evil.", likes = 88, isLiked = true),
        Post("p7", "Margaret Hamilton", "There was no choice but to be pioneers.", likes = 23, isLiked = false),
        Post("p8", "Tony Hoare", "I call it my billion-dollar mistake.", likes = 64, isLiked = false),
        Post("p9", "Frances Allen", "Compilers are about making things possible.", likes = 9, isLiked = false),
        Post("p10", "John McCarthy", "He who refuses to do arithmetic is doomed to talk nonsense.", likes = 17, isLiked = false),
    )
    private val tags = listOf(
        "kotlin", "compose", "multiplatform", "android", "ios", "architecture", "mvi", "testing",
        "coroutines", "design", "statemachine", "kmp",
    )

    override suspend fun feed(): List<Post> {
        delay(latency)
        return posts.toList()
    }

    override suspend fun setLiked(
        postId: String,
        liked: Boolean,
    ): Int {
        delay(latency)
        if (postId == FAILING_POST) error("Likes are off for this post")
        val index = posts.indexOfFirst { it.id == postId }
        val post = posts[index]
        val updated = if (post.isLiked == liked) post else post.copy(isLiked = liked, likes = post.likes + if (liked) 1 else -1)
        posts[index] = updated
        return updated.likes
    }

    /** Someone else likes the post every few seconds, for as long as it is watched. */
    override fun likes(postId: String): Flow<Int> = flow {
        while (true) {
            delay(LIVE_LIKE_MILLIS)
            val index = posts.indexOfFirst { it.id == postId }
            val post = posts[index].let { it.copy(likes = it.likes + 1) }
            posts[index] = post
            emit(post.likes)
        }
    }

    override suspend fun report(
        postId: String,
        reason: ReportReason,
        comment: String,
    ) {
        delay(latency)
        if (comment.contains(FAILING_TEXT, ignoreCase = true)) error("The report could not be sent")
    }

    override suspend fun searchTags(query: String): List<String> {
        delay(latency / 2)
        return tags.filter { it.startsWith(query, ignoreCase = true) }
    }

    override fun upload(draft: PostDraft): Flow<Float> = flow {
        for (step in 1..UPLOAD_STEPS) {
            delay(latency / 2)
            emit(step / UPLOAD_STEPS.toFloat())
        }
    }

    override suspend fun publish(draft: PostDraft): String {
        delay(latency)
        if (draft.text.contains(FAILING_TEXT, ignoreCase = true)) error("The post could not be published")
        val id = "p${posts.size + 1}"
        posts.add(0, Post(id, "You", draft.text, likes = 0, isLiked = false))
        return id
    }

    companion object {
        /** A comment or a post containing it fails, to show the error paths. */
        const val FAILING_TEXT = "fail"

        /** Grace Hopper's post: liking it always fails, to show the rollback. */
        const val FAILING_POST = "p3"
        const val LIVE_LIKE_MILLIS = 4_000L
        private const val UPLOAD_STEPS = 5
    }
}
