package az.theternal.cmplayground.feature.counter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.feature.counter.CounterContract.Effect
import az.theternal.cmplayground.feature.counter.CounterContract.Intent
import az.theternal.cmplayground.feature.counter.CounterContract.State
import az.theternal.cmplayground.feature.counter.components.CountText
import az.theternal.cmplayground.feature.counter.components.DecreaseButton
import az.theternal.cmplayground.feature.counter.components.IncreaseButton
import az.theternal.common.utils.Logger
import az.theternal.core.BaseComposable
import az.theternal.core.Provider
import az.theternal.core.select
import org.jetbrains.compose.ui.tooling.preview.Preview

val CounterProvider = compositionLocalOf { Provider<Intent, State>() }

@Composable
fun <Result> ProvidableCompositionLocal<Provider<Intent, State>>.select(
    initial: Result,
    transform: (State) -> Result
): Result {
    return current.state.select(initial,transform)
}

@Composable
fun CounterView(
    modifier: Modifier = Modifier,
) = BaseComposable(
    viewModelFactory = { CounterViewModel() },
    localProvider = CounterProvider,
    onEffectUpdate = { effect ->
        when (effect) {
            is Effect.ShowSnackbar -> {
                Logger.d(effect.text)
            }
        }
    }
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeContentPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(1f))

        CountText()

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.CenterHorizontally
            )
        ) {
            IncreaseButton()

            DecreaseButton()
        }

        Spacer(Modifier.weight(1f))
    }
}

@Preview
@Composable
fun CounterViewPreview() {
    MaterialTheme {
        CounterView()
    }
}