package az.theternal.samples.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import az.theternal.samples.createpost.CreatePostScreen
import az.theternal.samples.createpost.CreatePostViewModel
import az.theternal.samples.data.SampleDependencies
import az.theternal.samples.feed.FeedScreen
import az.theternal.samples.feed.FeedViewModel
import az.theternal.samples.report.ReportScreen
import az.theternal.samples.report.ReportViewModel
import az.theternal.samples.search.SearchScreen
import az.theternal.samples.search.SearchViewModel
import az.theternal.samples.signin.SignInScreen
import az.theternal.samples.signin.SignInVersion
import az.theternal.samples.signin.SignInVersions
import az.theternal.samples.signin.SignInViewModel

private enum class Sample(
    val title: String,
    val subtitle: String,
) {
    Report("Level 1 · Report a post", "Intents change the state directly"),
    Search("Level 2 · Search people", "Actions under policies: latest search, latest follow per user"),
    SignIn1("Migration · Sign in at level 1", "Flags, guards and hand-kept jobs — past its limit"),
    SignIn2("Migration · Sign in at level 2", "The same state, concurrency moved into policies"),
    SignIn3("Migration · Sign in at level 3", "Phases and transitions; no flags, no guards"),
    CreatePost("Level 4 · Create a post", "Four slices at three levels, one composition root"),
    Feed("Component ViewModels · Feed", "Each like button is its own ViewModel"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SamplesApp() {
    val dependencies = remember { SampleDependencies() }
    val snackbar = remember { SnackbarHostState() }
    var open by remember { mutableStateOf<Sample?>(null) }
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(open?.title ?: "MVI patterns") },
                    navigationIcon = { if (open != null) TextButton(onClick = { open = null }) { Text("Back") } },
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
                when (val sample = open) {
                    null -> Catalog(onOpen = { open = it })
                    else -> ScreenScope(key = sample) {
                        SampleScreen(sample, dependencies, snackbar, close = { open = null })
                    }
                }
            }
        }
    }
}

@Composable
private fun Catalog(onOpen: (Sample) -> Unit) {
    Sample.entries.forEach { sample ->
        ListItem(
            headlineContent = { Text(sample.title) },
            supportingContent = { Text(sample.subtitle) },
            modifier = Modifier.clickable { onOpen(sample) },
        )
        HorizontalDivider()
    }
}

@Composable
private fun SampleScreen(
    sample: Sample,
    dependencies: SampleDependencies,
    snackbar: SnackbarHostState,
    close: () -> Unit,
) {
    when (sample) {
        Sample.Report -> ReportScreen(viewModel { ReportViewModel("p1", dependencies.posts) }, snackbar, onDone = close)
        Sample.Search -> SearchScreen(viewModel { SearchViewModel(dependencies.users) }, snackbar)
        Sample.SignIn1 -> SignIn(SignInVersions.level1, dependencies, snackbar)
        Sample.SignIn2 -> SignIn(SignInVersions.level2, dependencies, snackbar)
        Sample.SignIn3 -> SignIn(SignInVersions.level3, dependencies, snackbar)
        Sample.CreatePost -> CreatePostScreen(viewModel { CreatePostViewModel(dependencies.posts) }, snackbar, onPublished = close)
        Sample.Feed -> FeedScreen(viewModel { FeedViewModel(dependencies.posts) }, dependencies.posts, snackbar)
    }
}

@Composable
private fun <S> SignIn(
    version: SignInVersion<S>,
    dependencies: SampleDependencies,
    snackbar: SnackbarHostState,
) {
    SignInScreen(viewModel { SignInViewModel(version, dependencies.auth) }, snackbar)
}

/**
 * A ViewModel store per opened sample, cleared when it closes — the playground's stand-in for a
 * navigation entry. Component ViewModels (the feed's likes) live in it too.
 */
@Composable
private fun ScreenScope(
    key: Any,
    content: @Composable () -> Unit,
) {
    val owner = remember(key) {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
}
