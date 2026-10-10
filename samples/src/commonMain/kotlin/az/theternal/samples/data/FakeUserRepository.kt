package az.theternal.samples.data

import kotlinx.coroutines.delay

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
