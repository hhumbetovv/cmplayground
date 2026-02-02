package az.theternal.cmplayground.feature.notifier

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import az.theternal.cmplayground.feature.notifier.NotifierContract.*
import az.theternal.core.framework.intent_consumer.postIntent

@Composable
fun ColumnScope.NotifierView(
    viewModel: NotifierViewModel = viewModel { NotifierViewModel() }
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = {
                viewModel.postIntent(Intent.Notify)
            },
        ) {
            Text("Notify")
        }
    }
}