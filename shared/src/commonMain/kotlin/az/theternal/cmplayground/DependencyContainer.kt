package az.theternal.cmplayground

import androidx.compose.runtime.ProvidedValue
import az.theternal.post.api.presentation.renderer.LocalPostCardRenderer
import az.theternal.post.impl.presentation.ren.PostCardRendererImpl

class Dependencies {
    val providedValues: Set<ProvidedValue<*>> = setOf(
        LocalPostCardRenderer provides PostCardRendererImpl()
    )
}