package ru.haykor.carmanager.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ru.haykor.carmanager.Res
import ru.haykor.carmanager.build
import ru.haykor.carmanager.core.designsystem.CmNavRow
import ru.haykor.carmanager.core.domain.model.ServiceRecordDetails
import ru.haykor.carmanager.mileage_km

@Composable
fun ServiceRecordRow(
    record: ServiceRecordDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape
) {
    val mileage = stringResource(Res.string.mileage_km, formatNumber(record.record.atMileage))
    val date = formatDate(record.record.date)
    val note = record.record.note?.takeIf { it.isNotBlank() }

    CmNavRow(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        leadingContent = { ServiceAvatar() },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("$date · $mileage")
                if (note != null) Text(note, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        trailingContent = record.record.price?.let { price ->
            { Text(formatNumber(price), style = MaterialTheme.typography.titleSmall, maxLines = 1) }
        }
    ) {
        Text(record.category.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ServiceAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(Res.drawable.build),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
