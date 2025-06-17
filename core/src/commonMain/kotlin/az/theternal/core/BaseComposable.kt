package az.theternal.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import az.theternal.common.utils.Logger

@Composable
inline fun <reified ViewModel : BaseViewModel<Intent, State, Effect>, Intent : ViewIntent, State : ViewState, Effect : ViewEffect> BaseComposable(
    crossinline viewModelFactory: () -> ViewModel,
    localProvider: ProvidableCompositionLocal<Provider<Intent, State>>,
    crossinline onEffectUpdate: ((Effect) -> Unit),
    crossinline content: @Composable () -> Unit
) {
    val viewModelStoreOwner = object : ViewModelStoreOwner {
        init {
            Logger.d("ViewModel Owner Created")
        }
        override val viewModelStore: ViewModelStore
            get() {
                Logger.d("ViewModel Store Created")
                return ViewModelStore()
            }
    }

    val viewModel: ViewModel = viewModel<ViewModel>(
        viewModelStoreOwner = viewModelStoreOwner,
        key = ViewModel::class.simpleName
    ) {
        Logger.d("ViewModel Created")
        viewModelFactory()
    }

    LaunchedEffect(viewModel.uiEffect) {
        viewModel.uiEffect.collect { effect ->
            onEffectUpdate(effect)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Logger.d("ViewModel Owner Cleared")
            viewModelStoreOwner.viewModelStore.clear()
        }
    }

    CompositionLocalProvider(
        localProvider provides Provider(
            _state = viewModel.uiState,
            postIntent = { viewModel.postIntent(it) }
        )
    ) {
        content()
    }
}