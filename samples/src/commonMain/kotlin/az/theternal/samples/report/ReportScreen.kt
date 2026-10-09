package az.theternal.samples.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import az.theternal.mvi.CollectEffects
import az.theternal.mvi.collectAsState
import az.theternal.mvi.select
import az.theternal.samples.data.ReportReason
import az.theternal.samples.ui.Hint
import az.theternal.samples.ui.SamplePage
import az.theternal.samples.ui.Section

@Composable
fun ReportScreen(
    viewModel: ReportViewModel,
    snackbar: SnackbarHostState,
    onDone: () -> Unit,
) {
    viewModel.CollectEffects { effect ->
        when (effect) {
            ReportEffect.Sent -> {
                snackbar.showSnackbar("Report sent")
                onDone()
            }
            is ReportEffect.Failed -> snackbar.showSnackbar(effect.message)
        }
    }
    ReportContent(state = viewModel.collectAsState(), postIntent = viewModel::postIntent)
}

@Composable
private fun ReportContent(
    state: State<ReportState>,
    postIntent: (ReportIntent) -> Unit,
) {
    SamplePage {
        Hint("Level 1: intents change the state directly. A comment containing \"fail\" fails to send.")
        ReportForm(state = state, postIntent = postIntent)
    }
}

/** The form alone: drawn by this screen, and by the feed's report sheet, where the same logic is a slot. */
@Composable
fun ReportForm(
    state: State<ReportState>,
    postIntent: (ReportIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Reasons(selected = state.select { it.reason }, onSelect = { postIntent(ReportIntent.SelectReason(it)) })
        val comment by state.select { it.comment }
        OutlinedTextField(
            value = comment,
            onValueChange = { postIntent(ReportIntent.EditComment(it)) },
            label = { Text("Comment") },
            modifier = Modifier.fillMaxWidth(),
        )
        val canSend by state.select { it.canSend }
        val isSending by state.select { it.isSending }
        Button(onClick = { postIntent(ReportIntent.Send) }, enabled = canSend) {
            Text(if (isSending) "Sending…" else "Send report")
        }
    }
}

@Composable
private fun Reasons(
    selected: State<ReportReason?>,
    onSelect: (ReportReason) -> Unit,
) {
    val current by selected
    Section("Why are you reporting this post?") {
        ReportReason.entries.forEach { reason ->
            Row(
                modifier = Modifier.fillMaxWidth().selectable(selected = reason == current, onClick = { onSelect(reason) }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = reason == current, onClick = { onSelect(reason) })
                Text(reason.label)
            }
        }
    }
}
