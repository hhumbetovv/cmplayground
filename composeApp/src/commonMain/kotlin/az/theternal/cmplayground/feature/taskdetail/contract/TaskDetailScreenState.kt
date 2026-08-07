package az.theternal.cmplayground.feature.taskdetail.contract

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import az.theternal.cmplayground.core.state.derive
import az.theternal.cmplayground.feature.taskdetail.widget.TaskDetailHeaderState
import az.theternal.cmplayground.feature.taskdetail.widget.TaskDetailNoteState

/**
 * The same two kinds of field as on the tasks screen, for the same reasons: the header carries
 * `State` because its parts move independently — ticking the task changes `isDone` and `isUpdating`
 * while the title stands still — and the note card is a plain value read whole.
 */
@Stable
data class TaskDetailScreenState(
    val phase: State<TaskDetailPhase>,
    val error: State<String>,
    val header: TaskDetailHeaderState,
    val noteCard: State<TaskDetailNoteState>,
)

/**
 * Built once, in two layers, exactly like `rememberTasksScreenState` — and from a ViewModel that has
 * no Orbit in it, which is the point of this screen.
 */
@Composable
fun rememberTaskDetailScreenState(source: State<TaskDetailState>): TaskDetailScreenState =
    remember(source) {
        // layer 1
        val errorMessage = source.derive { errorMessage }
        val note = source.derive { note }

        // layer 2
        TaskDetailScreenState(
            phase = source.derive { phase() },
            error = errorMessage.derive { orEmpty() },
            header = TaskDetailHeaderState(
                title = source.derive { title },
                priority = source.derive { priority },
                isDone = source.derive { isDone },
                isUpdating = source.derive { isUpdating },
            ),
            noteCard = note.derive { TaskDetailNoteState(note = this) },
        )
    }
