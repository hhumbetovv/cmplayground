package az.theternal.cmplayground.feature.tasks.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.core.debug.trackRecompositions
import az.theternal.cmplayground.core.mvi.ComponentState
import az.theternal.cmplayground.core.state.derive
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * Carries the list as ids plus a lookup, not as a list of items.
 *
 * That split is what decouples the two kinds of change. `ids` changes when the list's shape
 * changes — a page appended, a task created or deleted, a filter applied. `tasksById` changes
 * whenever any single item changes. Because [TaskList] reads only the first and each row derives
 * only its own entry from the second, an item edit never reaches the list.
 */
@Stable
data class TaskListState(
    val ids: State<ImmutableList<String>>,
    val tasksById: State<ImmutableMap<String, TaskItemState>>,
    val canLoadMore: State<Boolean>,
    val isLoadingMore: State<Boolean>,
) : ComponentState

private val ItemSpacing = 12.dp
private val ListPadding = 16.dp
private val FooterPadding = 24.dp
private const val LOAD_MORE_THRESHOLD = 3

/**
 * `ids` is the only thing this function reads, so it recomposes when the list's shape changes and
 * at no other time. Editing a task rewrites one map entry, which reaches the row that derived it
 * and stops there; `canLoadMore` is read inside the paging flow and `isLoadingMore` inside the
 * footer's own scope, so neither can invalidate the list either.
 */
@Composable
fun TaskList(
    state: TaskListState,
    onTaskClick: (String) -> Unit,
    onTaskCheckedChange: (String, Boolean) -> Unit,
    onTaskDeleteClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val ids = state.ids.value

    LaunchedEffect(listState, state) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: return@snapshotFlow false
            state.canLoadMore.value &&
                info.totalItemsCount > 0 &&
                lastVisible >= info.totalItemsCount - LOAD_MORE_THRESHOLD
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .trackRecompositions(),
        state = listState,
        contentPadding = PaddingValues(ListPadding),
        verticalArrangement = Arrangement.spacedBy(ItemSpacing),
    ) {
        items(items = ids, key = { it }) { id ->
            TaskRow(
                state = remember(id) { state.tasksById.derive { this[id] } },
                onClick = { onTaskClick(id) },
                onCheckedChange = { isDone -> onTaskCheckedChange(id, isDone) },
                onDeleteClick = { onTaskDeleteClick(id) },
            )
        }

        item(key = "load-more-footer") {
            TaskListFooter(state.isLoadingMore)
        }
    }
}

@Composable
private fun TaskListFooter(isLoadingMore: State<Boolean>) {
    if (!isLoadingMore.value) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(FooterPadding),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
