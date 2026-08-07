package az.theternal.cmplayground.feature.taskdetail.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.core.mvi.asUiState
import az.theternal.cmplayground.core.state.map
import az.theternal.cmplayground.core.state.read
import az.theternal.cmplayground.feature.taskdetail.contract.TaskDetailActions
import az.theternal.cmplayground.feature.taskdetail.contract.TaskDetailPhase
import az.theternal.cmplayground.feature.taskdetail.contract.TaskDetailState
import az.theternal.cmplayground.feature.taskdetail.widget.TaskDetailHeader
import az.theternal.cmplayground.feature.taskdetail.widget.TaskDetailNoteCard
import az.theternal.cmplayground.feature.tasks.domain.TaskPriority
import org.jetbrains.compose.ui.tooling.preview.Preview

private val ScreenPadding = 16.dp
private val SectionSpacing = 16.dp

@Composable
fun TaskDetailScreenContent(
    state: State<TaskDetailState>,
    actions: TaskDetailActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(SectionSpacing),
    ) {
        TextButton(onClick = actions.onBackClick) {
            Text("← Back to tasks")
        }

        when (state.read { phase() }) {
            TaskDetailPhase.LOADING -> Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            TaskDetailPhase.ERROR -> Column(
                modifier = Modifier.fillMaxSize().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SectionSpacing, Alignment.CenterVertically),
            ) {
                Text(
                    text = state.read { errorMessage.orEmpty() },
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(onClick = actions.onRetryClick) { Text("Retry") }
            }

            TaskDetailPhase.CONTENT -> {
                TaskDetailHeader(
                    state = state.map { headerState() },
                    onDoneChange = actions.onDoneChange,
                )
                TaskDetailNoteCard(state = state.map { noteState() })
            }
        }
    }
}

@Preview
@Composable
private fun TaskDetailScreenContentPreview() {
    val state = TaskDetailState(
        isLoading = false,
        title = "Split the state class per component",
        note = "One projection per component, one component state class per widget.",
        priority = TaskPriority.HIGH,
    )

    TaskDetailScreenContent(state = state.asUiState(), actions = TaskDetailActions())
}
