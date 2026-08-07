package az.theternal.cmplayground.feature.tasks.widget

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import az.theternal.cmplayground.core.debug.trackRecompositions
import az.theternal.cmplayground.core.mvi.ComponentState
import az.theternal.cmplayground.core.state.read
import az.theternal.cmplayground.core.state.rememberTextInput

@Immutable
data class TasksSearchFieldState(
    val query: String,
    val isClearVisible: Boolean,
) : ComponentState

/**
 * Typing recomposes this field and nothing else on the screen: every sibling reads its own slice,
 * and none of those slices change when `query` does.
 *
 * The rendered value comes from [rememberTextInput], not straight from the reduced state — see its
 * documentation for why a text field is the one place where a local write comes first.
 */
@Composable
fun TasksSearchField(
    state: State<TasksSearchFieldState>,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val input = rememberTextInput(state.read { query }, onQueryChange)

    OutlinedTextField(
        value = input.value,
        onValueChange = input::onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .trackRecompositions(),
        singleLine = true,
        label = { Text("Search tasks") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (state.read { isClearVisible }) {
                IconButton(onClick = onClearClick) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                }
            }
        },
    )
}
