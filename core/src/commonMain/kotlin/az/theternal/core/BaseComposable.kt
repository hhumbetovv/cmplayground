package az.theternal.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ProvidedValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collect

@Composable
inline fun <reified ViewModel : BaseViewModel<Intent, State, Effect>, Intent : ViewIntent, State : ViewState, Effect : ViewEffect> BaseComposable(
    crossinline viewModelFactory: () -> ViewModel,
    localProvider: ProvidableCompositionLocal<Provider<Intent, State>>,
    crossinline onEffectUpdate: ((Effect) -> Unit),
    crossinline content: @Composable () -> Unit
) {
    val viewModel: ViewModel = viewModel<ViewModel> { viewModelFactory() }

    LaunchedEffect(viewModel.uiEffect) {
        viewModel.uiEffect.collect { effect ->
            onEffectUpdate(effect)
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