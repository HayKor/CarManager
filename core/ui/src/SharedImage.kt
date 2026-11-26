package ru.haykor.carmanager.core.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

@Composable
fun Modifier.sharedImage(key: Any): Modifier {
    val sts = LocalSharedTransitionScope.current ?: return this
    val avs = LocalAnimatedVisibilityScope.current ?: return this
    return with(sts) {
        this@sharedImage.sharedElement(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = avs
        )
    }
}
