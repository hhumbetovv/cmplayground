package az.theternal.samples.data

data class User(
    val id: String,
    val name: String,
    val isFollowing: Boolean,
)

data class Post(
    val id: String,
    val author: String,
    val text: String,
    val likes: Int,
    val isLiked: Boolean,
)

enum class ReportReason(
    val label: String,
) {
    Spam("Spam"),
    Harassment("Harassment"),
    FalseInformation("False information"),
    Other("Something else"),
}

/** Everything a post is published with, gathered from the create-post screen's slices. */
data class PostDraft(
    val text: String,
    val pollOptions: List<String>,
    val tags: List<String>,
)

sealed interface SignInResult {
    data object Success : SignInResult

    data class CodeRequired(
        val challengeId: String,
    ) : SignInResult

    data object WrongCredentials : SignInResult
}
