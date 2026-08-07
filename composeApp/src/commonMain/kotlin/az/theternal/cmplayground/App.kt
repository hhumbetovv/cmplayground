package az.theternal.cmplayground

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.core.debug.LocalRecompositionHighlight
import az.theternal.cmplayground.feature.taskdetail.screen.TaskDetailScreen
import az.theternal.cmplayground.feature.tasks.screen.TasksScreen
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

sealed interface PlaygroundRoute {
    data object Tasks : PlaygroundRoute
    data class TaskDetail(val taskId: String) : PlaygroundRoute
}

private val ToggleSpacing = 8.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    MaterialTheme {
        var route by remember { mutableStateOf<PlaygroundRoute>(PlaygroundRoute.Tasks) }
        var isHighlightEnabled by remember { mutableStateOf(false) }

        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val showMessage: (String) -> Unit = remember(scope, snackbarHostState) {
            { message ->
                scope.launch { snackbarHostState.showSnackbar(message) }
                Unit
            }
        }

        CompositionLocalProvider(LocalRecompositionHighlight provides isHighlightEnabled) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = { Text("State playground") },
                        actions = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(ToggleSpacing),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = ToggleSpacing),
                            ) {
                                Text("Recompositions")
                                Switch(
                                    checked = isHighlightEnabled,
                                    onCheckedChange = { isHighlightEnabled = it },
                                )
                            }
                        },
                    )
                },
            ) { innerPadding ->
                val contentModifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)

                when (val current = route) {
                    PlaygroundRoute.Tasks -> TasksScreen(
                        onOpenTask = { taskId -> route = PlaygroundRoute.TaskDetail(taskId) },
                        onMessage = showMessage,
                        modifier = contentModifier,
                    )

                    is PlaygroundRoute.TaskDetail -> TaskDetailScreen(
                        taskId = current.taskId,
                        onBackClick = { route = PlaygroundRoute.Tasks },
                        onMessage = showMessage,
                        modifier = contentModifier,
                    )
                }
            }
        }
    }
}
