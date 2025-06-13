package az.theternal.cmplayground.feature.counter.components

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import az.theternal.cmplayground.feature.counter.CounterContract
import az.theternal.cmplayground.feature.counter.CounterProvider

@Composable
fun IncreaseButton() {

    val postIntent = CounterProvider.current.postIntent

    Button(
        onClick = {
            postIntent(CounterContract.Intent.Increase)
        },
    ) {
        Text("Increase")
    }
}