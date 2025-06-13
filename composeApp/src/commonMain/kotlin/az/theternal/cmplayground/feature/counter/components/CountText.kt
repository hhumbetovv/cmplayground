package az.theternal.cmplayground.feature.counter.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.theternal.cmplayground.feature.counter.CounterProvider
import az.theternal.cmplayground.feature.counter.select
import az.theternal.core.select
import kotlinx.coroutines.flow.map

@Composable
fun CountText() {

    val count = CounterProvider.select { it.count }

    Text(
        count.toString(),
        fontSize = 72.sp
    )

}