package az.theternal.cmplayground.feature.product.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.feature.product.domain.entities.dummyProducts
import az.theternal.cmplayground.feature.product.presentation.list.components.ProductListItem
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ProductListView(
    onNavigateDetails: (String) -> Unit
) {
    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                count = dummyProducts.size,
                key = { index -> dummyProducts[index].id },
            ) { index ->
                val item = dummyProducts[index]
                ProductListItem(
                    item = item,
                    onTap = { onNavigateDetails(item.id) }
                )
            }
        }
    }
}

@Preview
@Composable
private fun ProductListPreview() {
    ProductListView(
        onNavigateDetails = {}
    )
}