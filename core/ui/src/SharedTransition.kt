package ru.haykor.carmanager.core.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

@Composable
fun Modifier.sharedElement(key: Any): Modifier {
    val sts = LocalSharedTransitionScope.current ?: return this
    val avs = LocalAnimatedVisibilityScope.current ?: return this
    return with(sts) {
        this@sharedElement.sharedElement(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = avs
        )
    }
}

@Composable
fun Modifier.sharedBounds(key: Any): Modifier {
    val sts = LocalSharedTransitionScope.current ?: return this
    val avs = LocalAnimatedVisibilityScope.current ?: return this
    return with(sts) {
        this@sharedBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = avs,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
        )
    }
}

@Composable
fun Modifier.aboveSharedElements(zIndex: Float = 1f): Modifier {
    val sts = LocalSharedTransitionScope.current ?: return this
    return with(sts) { renderInSharedTransitionScopeOverlay(zIndexInOverlay = zIndex) }
}
