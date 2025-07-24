@file:OptIn(ExperimentalSharedTransitionApi::class)

package az.theternal.cmplayground.feature.product.presentation.details

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import az.theternal.cmplayground.feature.product.domain.entities.ProductEntity
import az.theternal.cmplayground.ui.modifiers.sharedBounds
import az.theternal.cmplayground.ui.modifiers.sharedElement
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsView(
    item: ProductEntity
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        modifier = Modifier.sharedBounds("title_${item.id}"),
                        text = item.title
                    )
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            AsyncImage(
                modifier = Modifier
                    .sharedElement("photo_${item.id}")
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp)),
                model = item.photoUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(16.dp))

            Text(
                modifier = Modifier
                    .sharedBounds("desc_${item.id}"),
                text = item.description,
            )
        }
    }
}