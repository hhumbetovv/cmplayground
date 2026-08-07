package az.theternal.cmplayground.feature.tasks.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import az.theternal.cmplayground.feature.tasks.TasksViewModel
import az.theternal.cmplayground.feature.tasks.contract.TasksEffect
import az.theternal.cmplayground.feature.tasks.contract.rememberTasksActions
import org.orbitmvi.orbit.compose.collectSideEffect

/**
 * The seam. Orbit's own API, called directly, with no wrapper in between.
 *
 * There is no state collection here: the state holder is handed over as it is, and its fields are
 * read where they are used. Nothing in this function reads state, so it composes once for the life
 * of the screen — no `collectAsState`, and therefore no chance of the whole screen recomposing per
 * keystroke.
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
        state = viewModel.state,
        actions = rememberTasksActions(viewModel::dispatch),
        modifier = modifier,
    )
}
