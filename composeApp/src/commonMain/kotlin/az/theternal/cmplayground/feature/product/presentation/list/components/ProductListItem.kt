package az.theternal.cmplayground.feature.product.presentation.list.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.feature.product.domain.entities.ProductEntity
import coil3.compose.SubcomposeAsyncImage
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun ProductListItem(
    modifier: Modifier = Modifier,
    item: ProductEntity,
    onTap: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SubcomposeAsyncImage(
            model = item.photoUrl,
            contentDescription = item.title,
            loading = {
                CircularProgressIndicator(Modifier.requiredSize(42.dp))
            },
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(72.dp)
                .clip(
                    shape = RoundedCornerShape(12.dp)
                )
        )

        Spacer(Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(item.title)

            Text(
                item.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}