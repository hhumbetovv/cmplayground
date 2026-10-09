package az.theternal.samples.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
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
    SignIn3("Migration · Sign in at level 3", "Phases and transitions in a state-machine library"),
    SignIn3Plain("Migration · Sign in at level 3, no library", "The same phases; work bound with whileIn and runOn"),
    CreatePost("Level 4 · Create a post", "Four slices at three levels; the draft survives process death"),
    Feed("Component ViewModels · Feed", "A ViewModel per card, live while on screen; a report sheet as a slot"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SamplesApp() {
    val dependencies = remember { SampleDependencies() }
    val snackbar = remember { SnackbarHostState() }
    val screens = viewModel { SampleScreens() }
    var openName by rememberSaveable { mutableStateOf<String?>(null) }
    val open = openName?.let(Sample::valueOf)
    val close = {
        openName?.let(screens::close)
        openName = null
    }
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(open?.title ?: "MVI patterns") },
                    navigationIcon = { if (open != null) TextButton(onClick = close) { Text("Back") } },
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (open) {
                    null -> Catalog(onOpen = { openName = it.name })
                    else -> ScreenScope(screens = screens, key = open.name) { savedState ->
                        SampleScreen(open, dependencies, savedState, snackbar, close = close)
                    }
                }
            }
        }
    }
}

@Composable
private fun Catalog(onOpen: (Sample) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Sample.entries.forEach { sample ->
            ListItem(
                headlineContent = { Text(sample.title) },
                supportingContent = { Text(sample.subtitle) },
                modifier = Modifier.clickable { onOpen(sample) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun SampleScreen(
    sample: Sample,
    dependencies: SampleDependencies,
    savedState: SavedStateHandle,
    snackbar: SnackbarHostState,
    close: () -> Unit,
) {
    when (sample) {
        Sample.Report -> ReportScreen(viewModel { ReportViewModel("p1", dependencies.posts) }.store, snackbar, onDone = close)
        Sample.Search -> SearchScreen(viewModel { SearchViewModel(dependencies.users) }.store, snackbar)
        Sample.SignIn1 -> SignIn(SignInVersions.level1, dependencies, snackbar)
        Sample.SignIn2 -> SignIn(SignInVersions.level2, dependencies, snackbar)
        Sample.SignIn3 -> SignIn(SignInVersions.level3, dependencies, snackbar)
        Sample.SignIn3Plain -> SignIn(SignInVersions.level3Plain, dependencies, snackbar)
        Sample.CreatePost -> CreatePostScreen(
            viewModel { CreatePostViewModel(dependencies.posts, savedState) }.store,
            snackbar,
            onPublished = close,
        )
        Sample.Feed -> FeedScreen(viewModel { FeedViewModel(dependencies.posts) }.store, dependencies.posts, snackbar)
    }
}

@Composable
private fun <S> SignIn(
    version: SignInVersion<S>,
    dependencies: SampleDependencies,
    snackbar: SnackbarHostState,
) {
    SignInScreen(viewModel { SignInViewModel(version, dependencies.auth) }.store, snackbar)
}

/**
 * The open samples' ViewModel stores and saved state — the playground's stand-in for navigation entries.
 * Kept in the app's ViewModel store, so a sample's ViewModels survive a rotation; cleared when the sample
 * is closed. Component ViewModels (the feed's likes) live in a sample's store too.
 */
private class SampleScreens : ViewModel() {
    class Screen(
        val owner: ViewModelStoreOwner,
        val savedState: SavedStateHandle,
    )

    private val screens = mutableMapOf<String, Screen>()

    fun screen(
        key: String,
        restore: () -> SavedStateHandle,
    ): Screen {
        return screens.getOrPut(key) {
            val store = ViewModelStore()
            Screen(owner = object : ViewModelStoreOwner {
                override val viewModelStore = store
            }, savedState = restore())
        }
    }

    fun close(key: String) {
        screens.remove(key)?.owner?.viewModelStore?.clear()
    }

    override fun onCleared() {
        screens.values.forEach { screen -> screen.owner.viewModelStore.clear() }
        screens.clear()
    }
}

/**
 * Hosts a sample: its ViewModel store, and a [SavedStateHandle] that is written into the composition's
 * saved state, so that what a keeper saves comes back after the process was killed. (To try it on
 * Android, turn on "Don't keep activities" and leave the app.)
 */
@Composable
private fun ScreenScope(
    screens: SampleScreens,
    key: String,
    content: @Composable (SavedStateHandle) -> Unit,
) {
    val saved = rememberSaveable(key, saver = SavedHandleSaver) { SavedHandle() }
    val screen = screens.screen(key) { SavedStateHandle(saved.restored) }
    saved.live = screen.savedState
    CompositionLocalProvider(LocalViewModelStoreOwner provides screen.owner) { content(screen.savedState) }
}

/** The bridge between a sample's handle and the composition's saved state: string entries only. */
private class SavedHandle(
    val restored: Map<String, Any?> = emptyMap(),
) {
    var live: SavedStateHandle? = null
}

private val SavedHandleSaver = Saver<SavedHandle, ArrayList<String>>(
    save = { saved ->
        val handle = saved.live ?: return@Saver null
        ArrayList<String>().apply {
            handle.keys().forEach { key ->
                handle.get<Any?>(key)?.let { value ->
                    if (value is String) {
                        add(key)
                        add(value)
                    }
                }
            }
        }
    },
    restore = { entries -> SavedHandle(entries.chunked(2).associate { (key, value) -> key to value }) },
)
