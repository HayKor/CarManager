package ru.haykor.carmanager.core.designsystem

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.Modifier

/**
 * Common enter/exit/placement animation for rows inside a lazy list.
 * Call only from an item/header content lambda of a lazy list.
 */
fun LazyItemScope.cmAnimateItem(modifier: Modifier = Modifier): Modifier = modifier.animateItem(
    fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
    fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow),
    placementSpec = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
)
