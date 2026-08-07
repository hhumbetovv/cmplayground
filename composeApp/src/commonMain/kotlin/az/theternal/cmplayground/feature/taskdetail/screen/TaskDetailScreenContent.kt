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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.core.state.preview
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
    state: TaskDetailState,
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

        when (state.phase.value) {
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
                Text(text = state.error.value, style = MaterialTheme.typography.bodyLarge)
                Button(onClick = actions.onRetryClick) { Text("Retry") }
            }

            TaskDetailPhase.CONTENT -> {
                TaskDetailHeader(state = state.header, onDoneChange = actions.onDoneChange)
                TaskDetailNoteCard(state = state.noteCard)
            }
        }
    }
}

@Preview
@Composable
private fun TaskDetailScreenContentPreview() {
    val state = remember { TaskDetailState() }.preview {
        state.isLoading.set(false)
        state.title.set("Split the state class per component")
        state.note.set("One field per thing that changes, one component state per component.")
        state.priority.set(TaskPriority.HIGH)
    }

    TaskDetailScreenContent(state = state, actions = TaskDetailActions())
}
