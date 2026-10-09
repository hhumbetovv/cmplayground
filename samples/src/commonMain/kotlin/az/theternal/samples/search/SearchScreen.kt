package az.theternal.samples.search

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import az.theternal.mvi.CollectEffects
import az.theternal.mvi.ContentState
import az.theternal.mvi.collectAsState
import az.theternal.mvi.select
import az.theternal.samples.data.User
import az.theternal.samples.ui.ErrorText
import az.theternal.samples.ui.Hint
import az.theternal.samples.ui.SamplePage

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    snackbar: SnackbarHostState,
) {
    viewModel.CollectEffects { effect ->
        when (effect) {
            is SearchEffect.FollowFailed -> snackbar.showSnackbar("Could not follow ${effect.name}")
        }
    }
    SearchContent(state = viewModel.collectAsState(), postIntent = viewModel::postIntent)
}

@Composable
private fun SearchContent(
    state: State<SearchState>,
    postIntent: (SearchIntent) -> Unit,
) {
    SamplePage {
        Hint(
            "Level 2: type fast — only the last query is searched. Tap follow several times — only the last " +
                "wish is sent. \"error\" fails the search; following Ken Thompson fails and rolls back.",
        )
        val query by state.select { it.query }
        OutlinedTextField(
            value = query,
            onValueChange = { postIntent(SearchIntent.EditQuery(it)) },
            label = { Text("Search people") },
            modifier = Modifier.fillMaxWidth(),
        )
        Results(results = state.select { it.results }, postIntent = postIntent)
    }
}

@Composable
private fun Results(
    results: State<ContentState<List<User>>>,
    postIntent: (SearchIntent) -> Unit,
) {
    when (val current = results.value) {
        ContentState.Idle -> Hint("Results appear as you type.")
        is ContentState.Loading -> CircularProgressIndicator()
        is ContentState.Error -> {
            ErrorText(current.error.message.orEmpty())
            Button(onClick = { postIntent(SearchIntent.Retry) }) { Text("Retry") }
        }
        is ContentState.Success -> {
            if (current.data.isEmpty()) Hint("Nobody by that name.")
            current.data.forEach { user ->
                UserRow(user = user, onToggleFollow = { postIntent(SearchIntent.ToggleFollow(user.id)) })
            }
        }
    }
}

@Composable
private fun UserRow(
    user: User,
    onToggleFollow: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(user.name, modifier = Modifier.weight(1f))
        if (user.isFollowing) {
            OutlinedButton(onClick = onToggleFollow) { Text("Following") }
        } else {
            FilledTonalButton(onClick = onToggleFollow) { Text("Follow") }
        }
    }
}
