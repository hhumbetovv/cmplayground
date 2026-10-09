package az.theternal.samples.feed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import az.theternal.mvi.CollectEffects
import az.theternal.mvi.ContentState
import az.theternal.mvi.collectAsState
import az.theternal.mvi.dataOrNull
import az.theternal.samples.data.Post
import az.theternal.samples.data.PostRepository
import az.theternal.samples.ui.ErrorText
import az.theternal.samples.ui.Hint
import az.theternal.samples.ui.SamplePage

@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    posts: PostRepository,
    snackbar: SnackbarHostState,
) {
    val state by viewModel.collectAsState()
    SamplePage {
        Hint(
            "Component ViewModels: every like button is its own ViewModel, keyed by the post. Tap a heart many " +
                "times — one request. Grace Hopper's post refuses likes and rolls back.",
        )
        Button(onClick = { viewModel.postIntent(FeedIntent.Refresh) }) { Text("Refresh") }
        val feed = state.posts
        if (feed is ContentState.Loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (feed is ContentState.Error) ErrorText(feed.error.message.orEmpty())
        if (feed is ContentState.Loading && feed.previous == null) CircularProgressIndicator()
        feed.dataOrNull()?.forEach { post -> PostCard(post = post, posts = posts, snackbar = snackbar) }
    }
}

@Composable
private fun PostCard(
    post: Post,
    posts: PostRepository,
    snackbar: SnackbarHostState,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(post.author, style = MaterialTheme.typography.titleSmall)
            Text(post.text)
            Row(verticalAlignment = Alignment.CenterVertically) {
                LikeButton(post = post, posts = posts, snackbar = snackbar)
            }
        }
    }
}

@Composable
private fun LikeButton(
    post: Post,
    posts: PostRepository,
    snackbar: SnackbarHostState,
) {
    val like = viewModel(key = "like-${post.id}") { LikeViewModel(post, posts) }
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
