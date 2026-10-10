package az.theternal.samples.createpost.poll

data class PollState(
    val isOn: Boolean = false,
    val options: List<String> = List(MIN_OPTIONS) { "" },
) {
    val canAdd: Boolean get() = options.size < MAX_OPTIONS
    val canRemove: Boolean get() = options.size > MIN_OPTIONS

    companion object {
        const val MIN_OPTIONS = 2
        const val MAX_OPTIONS = 4
    }
}

sealed interface PollIntent {
    data object Toggle : PollIntent

    data class EditOption(
        val index: Int,
        val text: String,
    ) : PollIntent

    data object AddOption : PollIntent

    data class RemoveOption(
        val index: Int,
    ) : PollIntent
}
