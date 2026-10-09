package az.theternal.samples.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import az.theternal.mvi.CollectEffects
import az.theternal.mvi.ContentState
import az.theternal.mvi.ItemScope
import az.theternal.mvi.collectAsState
import az.theternal.mvi.dataOrNull
import az.theternal.mvi.rememberItemStores
import az.theternal.mvi.select
import az.theternal.samples.data.Post
import az.theternal.samples.data.PostRepository
import az.theternal.samples.report.ReportForm
import az.theternal.samples.report.ReportState
import az.theternal.samples.ui.ErrorText
import az.theternal.samples.ui.Hint

@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    posts: PostRepository,
    snackbar: SnackbarHostState,
) {
    viewModel.CollectEffects { effect ->
        when (effect) {
            is FeedEffect.Message -> snackbar.showSnackbar(effect.text)
        }
    }
    val state = viewModel.collectAsState()
    val feed by state.select { it.posts }
    val items = feed.dataOrNull().orEmpty()
    // One ViewModel store per post: a card's like lives while the post is in the feed.
    val itemStores = rememberItemStores(keys = items.map(Post::id))
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Hint(
                    "Each like is its own ViewModel, in its card's own store: it survives scrolling away and is " +
                        "cleared when a refresh drops the post. Counts tick live only while a card is on screen. " +
                        "Grace Hopper's post refuses likes and rolls back. Report opens the report screen's logic " +
                        "in a sheet; a comment containing \"fail\" fails.",
                )
                Button(onClick = { viewModel.postIntent(FeedIntent.Refresh) }) { Text("Refresh") }
                if (feed is ContentState.Loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                (feed as? ContentState.Error)?.let { ErrorText(it.error.message.orEmpty()) }
                if (feed is ContentState.Loading && items.isEmpty()) CircularProgressIndicator()
            }
        }
        items(items, key = Post::id) { post ->
            ItemScope(itemStores, post.id) {
                PostCard(
                    post = post,
                    posts = posts,
                    snackbar = snackbar,
                    onReport = { viewModel.postIntent(FeedIntent.OpenReport(post.id)) },
                )
            }
        }
    }
    val isReporting by state.select { it.report != null }
    if (isReporting) {
        ReportSheet(
            state = state.select { it.report ?: ReportState() },
            postIntent = viewModel::postIntent,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportSheet(
    state: State<ReportState>,
    postIntent: (FeedIntent) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = { postIntent(FeedIntent.CloseReport) }) {
        Column(modifier = Modifier.padding(16.dp)) {
            ReportForm(state = state, postIntent = { postIntent(FeedIntent.Report(it)) })
        }
    }
}

@Composable
private fun PostCard(
    post: Post,
    posts: PostRepository,
    snackbar: SnackbarHostState,
    onReport: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(post.author, style = MaterialTheme.typography.titleSmall)
            Text(post.text)
            Row(verticalAlignment = Alignment.CenterVertically) {
                LikeButton(post = post, posts = posts, snackbar = snackbar)
                TextButton(onClick = onReport) { Text("Report") }
            }
        }
    }
}

/** Inside the card's [ItemScope], so `viewModel { }` is the card's own. */
@Composable
private fun LikeButton(
    post: Post,
    posts: PostRepository,
    snackbar: SnackbarHostState,
) {
    val like = viewModel { LikeViewModel(post, posts) }
    like.CollectEffects { effect ->
        when (effect) {
            LikeEffect.Failed -> snackbar.showSnackbar("Could not like ${post.author}'s post")
        }
    }
    val state by like.collectAsState()
    TextButton(onClick = { like.postIntent(LikeIntent.Toggle) }) {
        Text("${if (state.isLiked) "♥" else "♡"} ${state.count}")
    }
}
