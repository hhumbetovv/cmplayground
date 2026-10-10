package az.theternal.samples.report

import az.theternal.samples.data.ReportReason

data class ReportState(
    val reason: ReportReason? = null,
    val comment: String = "",
    val isSending: Boolean = false,
) {
    val canSend: Boolean
        get() = reason != null && (reason != ReportReason.Other || comment.isNotBlank()) && !isSending
}

sealed interface ReportIntent {
    data class SelectReason(
        val reason: ReportReason,
    ) : ReportIntent

    data class EditComment(
        val text: String,
    ) : ReportIntent

    data object Send : ReportIntent
}

sealed interface ReportEffect {
    data object Sent : ReportEffect

    data class Failed(
        val message: String,
    ) : ReportEffect
}
