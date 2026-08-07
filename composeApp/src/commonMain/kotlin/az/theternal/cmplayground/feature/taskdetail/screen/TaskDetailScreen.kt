package az.theternal.cmplayground.feature.taskdetail.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import az.theternal.cmplayground.core.mvi.collectUiEffects
import az.theternal.cmplayground.feature.taskdetail.TaskDetailViewModel
import az.theternal.cmplayground.feature.taskdetail.contract.TaskDetailActions
import az.theternal.cmplayground.feature.taskdetail.contract.TaskDetailEffect

/**
 * The non-Orbit seam, doing exactly what `TasksScreen` does: hand the state holder down, forward
 * effects, bind actions. Only the effect collector differs — Orbit ships its own, a plain `Flow`
 * does not.
 *
 * As in `TasksScreen`, no state is read here.
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
        state = viewModel.state,
        actions = actions,
        modifier = modifier,
    )
}
