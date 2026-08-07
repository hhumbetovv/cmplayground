package az.theternal.cmplayground.feature.taskdetail.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import az.theternal.cmplayground.core.mvi.collectUiEffects
import az.theternal.cmplayground.feature.taskdetail.TaskDetailViewModel
import az.theternal.cmplayground.feature.taskdetail.contract.TaskDetailActions
import az.theternal.cmplayground.feature.taskdetail.contract.TaskDetailEffect
import az.theternal.cmplayground.feature.taskdetail.contract.rememberTaskDetailScreenState

/**
 * The non-Orbit seam, doing exactly what `TasksScreen` does: turn the holder's state into a
 * `State<S>`, forward its effects, bind its actions. Only the two calls differ —
 * `collectAsStateWithLifecycle()` instead of Orbit's `collectAsState()`, and a plain effect flow
 * instead of Orbit side effects.
 *
 * As in `TasksScreen`, the state is not read here.
 */
@Composable
fun TaskDetailScreen(
    taskId: String,
    onBackClick: () -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: TaskDetailViewModel = viewModel(key = taskId) { TaskDetailViewModel(taskId) }

    viewModel.effects.collectUiEffects { effect ->
        when (effect) {
            is TaskDetailEffect.ShowMessage -> onMessage(effect.message)
        }
    }

    val actions = remember(viewModel, onBackClick) {
        TaskDetailActions(
            onRetryClick = viewModel::load,
            onDoneChange = viewModel::changeDone,
            onBackClick = onBackClick,
        )
    }

    TaskDetailScreenContent(
        state = rememberTaskDetailScreenState(viewModel.state.collectAsStateWithLifecycle()),
        actions = actions,
        modifier = modifier,
    )
}
