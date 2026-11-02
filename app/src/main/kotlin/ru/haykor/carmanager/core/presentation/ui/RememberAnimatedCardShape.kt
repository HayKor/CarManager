package ru.haykor.carmanager.core.presentation.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.haykor.carmanager.core.common.ComprehensionDebt

@ComprehensionDebt
@Composable
fun rememberAnimatedCardShape(
    isFirst: Boolean,
    isLast: Boolean,
    smallRadius: Dp = 8.dp,
    largeRadius: Dp = 32.dp
): Shape {
    val targetTop = if (isFirst) largeRadius else smallRadius
    val targetBottom = if (isLast) largeRadius else smallRadius

    val animatedTop by animateDpAsState(
        targetValue = targetTop,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "TopEndCorner"
    )
    val animatedBottom by animateDpAsState(
        targetValue = targetBottom,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "BottomStartCorner"
    )

    return RoundedCornerShape(
        topStart = animatedTop,
        topEnd = animatedTop,
        bottomEnd = animatedBottom,
        bottomStart = animatedBottom
    )
}
