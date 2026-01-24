package az.theternal.cmplayground.feature.post.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import az.theternal.cmplayground.feature.post.feature.PostDetailsView
import az.theternal.cmplayground.feature.post.feature.PostListView

fun EntryProviderScope<NavKey>.postGraph() {
    entry <PostRoute.List> {
        PostListView()
    }

    entry<PostRoute.Details> { routeKey ->
        PostDetailsView(routeKey)
    }
}