package ru.haykor.carmanager.core.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import ru.haykor.carmanager.Res
import ru.haykor.carmanager.keyboard_arrow_right

val CmSegmentedGap: Dp = ListItemDefaults.SegmentedGap

private val SectionHeaderPadding =
    PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 4.dp)

@Composable
fun cmSegmentShape(index: Int, count: Int): Shape = ListItemDefaults.segmentedShapes(index = index, count = count).shape

/* A row that opens another page */
@Composable
fun CmNavRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    enabled: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null, // shown before the chevron
    showChevron: Boolean = true,
    content: @Composable () -> Unit
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.shapes(shape = shape),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        verticalAlignment = Alignment.CenterVertically,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        leadingContent = leadingContent,
        supportingContent = supportingContent,
        trailingContent =
            if (trailingContent != null || showChevron) {
                {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        trailingContent?.invoke()
                        if (showChevron) CmChevron()
                    }
                }
            } else {
                null
            },
        content = content
    )
}

@Composable
private fun CmChevron() = Icon(
    painter = painterResource(Res.drawable.keyboard_arrow_right),
    contentDescription = null,
    tint = MaterialTheme.colorScheme.onSurfaceVariant
)

@Composable
fun CmSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(SectionHeaderPadding)
    )
}
// TODO: CmSelectionRow, CmSwitchRow, CmSectionHeader when needed,
// ref: [https://github.com/Flow-Tube/Flow/blob/main/app/src/main/java/io/github/aedev/flow/ui/components/shared/FlowListRows.kt]
