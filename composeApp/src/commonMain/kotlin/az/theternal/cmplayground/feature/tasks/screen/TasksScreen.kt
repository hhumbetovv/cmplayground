package az.theternal.cmplayground.feature.tasks.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import az.theternal.cmplayground.feature.tasks.TasksViewModel
import az.theternal.cmplayground.feature.tasks.contract.TasksEffect
import az.theternal.cmplayground.feature.tasks.contract.rememberTasksActions
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

/**
 * The seam. Orbit's own API, called directly, with no wrapper in between.
 *
 * The state is **not** read here, and that is the load-bearing detail:
 * `val uiState by viewModel.collectAsState()` would subscribe this composable to every state
 * change, so the whole screen would re-run on each keystroke and every projection below it would
 * start from an already invalidated scope. Kept as a `State` and handed down, this function
 * composes once.
 */
@Composable
fun TasksScreen(
    onOpenTask: (String) -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TasksViewModel = viewModel { TasksViewModel() },
) {
    viewModel.collectSideEffect { effect ->
        when (effect) {
            is TasksEffect.OpenTask -> onOpenTask(effect.id)
            is TasksEffect.ShowMessage -> onMessage(effect.message)
        }
    }

    TasksScreenContent(
        state = viewModel.collectAsState(),
        actions = rememberTasksActions(viewModel::dispatch),
        modifier = modifier,
    )
}
