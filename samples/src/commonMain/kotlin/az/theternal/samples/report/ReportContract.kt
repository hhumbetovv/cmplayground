package az.theternal.samples.report

import az.theternal.mvi.ViewEffect
import az.theternal.mvi.ViewIntent
import az.theternal.mvi.ViewState
import az.theternal.samples.data.ReportReason

data class ReportState(
    val reason: ReportReason? = null,
    val comment: String = "",
    val isSending: Boolean = false,
) : ViewState {
    val canSend: Boolean
        get() = reason != null && (reason != ReportReason.Other || comment.isNotBlank()) && !isSending
}

sealed interface ReportIntent : ViewIntent {
    data class SelectReason(
        val reason: ReportReason,
    ) : ReportIntent

    data class EditComment(
        val text: String,
    ) : ReportIntent

    data object Send : ReportIntent
}

sealed interface ReportEffect : ViewEffect {
    data object Sent : ReportEffect

    data class Failed(
        val message: String,
    ) : ReportEffect
}
