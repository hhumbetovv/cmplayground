package az.theternal.post.impl.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.theternal.post.api.presentation.model.PostUiModel

@Composable
fun PostCard(
    post: PostUiModel,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.Black.copy(alpha = 0.5f),
                shape = shape
            )
            .background(
                color = Color.White,
                shape = shape,
            )
            .padding(
                horizontal = 12.dp,
                vertical = 8.dp
            )
    ) {
        BasicText(
            text = post.title,
            style = TextStyle(
                fontSize = 20.sp
            )
        )

        Spacer(modifier = Modifier.height(8.dp))
        
        BasicText(
            text = post.content
        )
    }
}

@Composable
@Preview
private fun DefaultPreview() {
    PostCard(
        post = PostUiModel(
            title = "Post Title",
            content = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata"
        ),
    )
}