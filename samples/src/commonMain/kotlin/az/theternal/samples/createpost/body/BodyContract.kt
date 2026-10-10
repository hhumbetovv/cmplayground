package az.theternal.samples.createpost.body

data class BodyState(
    val text: String = "",
) {
    val remaining: Int get() = MAX_LENGTH - text.length

    companion object {
        const val MAX_LENGTH = 280
    }
}

sealed interface BodyIntent {
    data class Edit(
        val text: String,
    ) : BodyIntent
}
