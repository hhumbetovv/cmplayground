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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import az.theternal.cmplayground.core.debug.trackRecompositions
import az.theternal.cmplayground.core.mvi.ComponentState
import az.theternal.cmplayground.core.state.rememberTextInput

/**
 * `State` fields, because the two change at different times: `query` on every keystroke,
 * `isClearVisible` only when the text becomes empty or stops being empty.
 */
@Stable
data class TasksSearchFieldState(
    val query: State<String>,
    val isClearVisible: State<Boolean>,
) : ComponentState

/**
 * The parameters never change — they are `State` references fixed at construction — so nothing
 * outside this field can recompose it. Typing recomposes it because it reads `query` itself, and
 * `isClearVisible` is read inside the trailing icon's own scope, so the icon appearing and
 * disappearing repaints the icon and not the field.
 *
 * The rendered value comes from [rememberTextInput], not straight from the reduced state — see its
 * documentation for why a text field is the one place where a local write comes first.
 */
@Composable
fun TasksSearchField(
    state: TasksSearchFieldState,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val input = rememberTextInput(state.query.value, onQueryChange)

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
            if (state.isClearVisible.value) {
                IconButton(onClick = onClearClick) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                }
            }
        },
    )
}
