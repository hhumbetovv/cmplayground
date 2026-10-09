package az.theternal.samples.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

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

class FakeUserRepository(
    private val latency: Long = 600,
) : UserRepository {
    private val following = mutableSetOf("u2")
    private val names = listOf(
        "Ada Lovelace", "Alan Turing", "Grace Hopper", "Barbara Liskov", "Donald Knuth",
        "Edsger Dijkstra", "Margaret Hamilton", "Ken Thompson", "Linus Torvalds", "Radia Perlman",
    )

    override suspend fun search(query: String): List<User> {
        delay(latency)
        if (query.equals(FAILING_QUERY, ignoreCase = true)) error("The search is down")
        return names.withIndex()
            .filter { (_, name) -> name.contains(query, ignoreCase = true) }
            .map { (index, name) -> User(id = "u$index", name = name, isFollowing = "u$index" in following) }
    }

    override suspend fun setFollowing(
        userId: String,
        following: Boolean,
    ) {
        delay(latency)
        if (userId == FAILING_USER) error("Cannot follow this user")
        if (following) this.following += userId else this.following -= userId
    }

    companion object {
        const val FAILING_QUERY = "error"

        /** Ken Thompson: following him always fails, to show the rollback. */
        const val FAILING_USER = "u7"
    }
}

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

/** What the samples are built from; the playground has no DI. */
class SampleDependencies(
    val auth: AuthRepository = FakeAuthRepository(),
    val users: UserRepository = FakeUserRepository(),
    val posts: PostRepository = FakePostRepository(),
)
