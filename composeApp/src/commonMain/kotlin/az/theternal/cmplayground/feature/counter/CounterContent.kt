package az.theternal.cmplayground.feature.counter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.feature.counter.CounterContract.*
import az.theternal.cmplayground.feature.counter.components.CountText
import az.theternal.cmplayground.feature.counter.components.DecreaseButton
import az.theternal.cmplayground.feature.counter.components.IncreaseButton
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ColumnScope.CounterContent(
    state: State,
    postIntent: (Intent) -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .safeContentPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(1f))

        CountText(state.count)

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.CenterHorizontally
            )
        ) {
            IncreaseButton(
                onClick = { postIntent(Intent.Increase) }
            )

            DecreaseButton(
                onClick = { postIntent(Intent.Decrease) }
            )
        }

        Spacer(Modifier.weight(1f))
    }
}

@Preview
@Composable
fun CounterViewPreview() {
    MaterialTheme {
        Column {
            CounterContent(
                state = State(),
                postIntent = {},
            )
        }
    }
}