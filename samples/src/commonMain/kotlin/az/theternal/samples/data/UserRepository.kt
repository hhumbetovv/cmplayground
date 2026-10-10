package az.theternal.samples.data

interface UserRepository {
    suspend fun search(query: String): List<User>

    /** Sets, not toggles: sending the latest wish again is harmless, which is what lets a tap cancel the last. */
    suspend fun setFollowing(
        userId: String,
        following: Boolean,
    )
}
