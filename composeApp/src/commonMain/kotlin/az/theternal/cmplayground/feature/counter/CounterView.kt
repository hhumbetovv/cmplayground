package az.theternal.cmplayground.feature.counter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import az.theternal.cmplayground.feature.counter.CounterContract.Effect
import az.theternal.common.utils.Logger
import az.theternal.core.framework.delegates.OnEffectUpdate
import az.theternal.core.framework.delegates.collectAsState

@Composable
fun CounterView(
    viewModel: CounterViewModel = viewModel { CounterViewModel() }
) = with(viewModel) {

    OnEffectUpdate { effect ->
        when(effect) {
            is Effect.ShowSnackbar -> {
                Logger.d(effect.text)
            }
        }
    }

    val state by collectAsState()

    CounterContent(
        state = state,
        postIntent = { viewModel.postIntent(it) }
    )
}