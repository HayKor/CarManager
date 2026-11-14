package ru.haykor.carmanager.feature.car.ui

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ru.haykor.carmanager.Res
import ru.haykor.carmanager.build
import ru.haykor.carmanager.cancel
import ru.haykor.carmanager.car_info_title
import ru.haykor.carmanager.car_not_found
import ru.haykor.carmanager.car_not_found_description
import ru.haykor.carmanager.car_section_details
import ru.haykor.carmanager.car_section_service_history
import ru.haykor.carmanager.car_service_history_empty
import ru.haykor.carmanager.chevron_left
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.PlateCountry
import ru.haykor.carmanager.core.domain.ServiceCategoryId
import ru.haykor.carmanager.core.domain.ServiceRecordId
import ru.haykor.carmanager.core.domain.model.Car
import ru.haykor.carmanager.core.domain.model.ServiceCategory
import ru.haykor.carmanager.core.domain.model.ServiceRecord
import ru.haykor.carmanager.core.domain.model.ServiceRecordDetails
import ru.haykor.carmanager.core.presentation.coroutines.ObserveAsEvents
import ru.haykor.carmanager.core.presentation.ui.LicensePlate
import ru.haykor.carmanager.core.presentation.ui.formatMileage
import ru.haykor.carmanager.core.presentation.ui.rememberAnimatedCardShape
import ru.haykor.carmanager.core.presentation.ui.title
import ru.haykor.carmanager.core.presentation.ui.toUiText
import ru.haykor.carmanager.delete
import ru.haykor.carmanager.delete_car_text
import ru.haykor.carmanager.delete_car_title
import ru.haykor.carmanager.description_edit
import ru.haykor.carmanager.edit
import ru.haykor.carmanager.feature.car.component.CarComponent
import ru.haykor.carmanager.feature.car.component.CarEvent
import ru.haykor.carmanager.feature.car.component.CarIntent
import ru.haykor.carmanager.feature.car.component.CarSection
import ru.haykor.carmanager.feature.car.component.CarState
import ru.haykor.carmanager.feature.car.component.RecordsSection
import ru.haykor.carmanager.field_mileage
import ru.haykor.carmanager.field_vin
import ru.haykor.carmanager.go_back
import ru.haykor.carmanager.keyboard_arrow_right
import ru.haykor.carmanager.mileage_km
import ru.haykor.carmanager.more_vert
import ru.haykor.carmanager.not_specified
import ru.haykor.carmanager.notes
import ru.haykor.carmanager.pin
import ru.haykor.carmanager.plus
import ru.haykor.carmanager.show_less
import ru.haykor.carmanager.show_more
import ru.haykor.carmanager.ui.theme.AppTheme

// region composables
@Composable
fun CarScreen(component: CarComponent, modifier: Modifier = Modifier) {
    val state by component.state.subscribeAsState()

    val context = LocalContext.current
    ObserveAsEvents(component.events) { event ->
        when (event) {
            is CarEvent.DeleteFailed -> {
                Toast.makeText(context, getString(event.error.toUiText()), Toast.LENGTH_LONG).show()
            }
        }
    }

    CarContent(
        state = state,
        onIntent = component::accept,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CarContent(state: CarState, onIntent: (CarIntent) -> Unit, modifier: Modifier = Modifier) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text =
                            (state.carSection as? CarSection.Loaded)?.car?.title
                                ?: stringResource(Res.string.car_info_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onIntent(CarIntent.BackClicked) }) {
                        Icon(
                            painter = painterResource(Res.drawable.chevron_left),
                            contentDescription = stringResource(Res.string.go_back)
                        )
                    }
                },
                actions = {
                    if (state.carSection is CarSection.Loaded) {
                        IconButton(onClick = { onIntent(CarIntent.EditCarClicked) }) {
                            Icon(
                                painter = painterResource(Res.drawable.edit),
                                contentDescription = stringResource(Res.string.description_edit)
                            )
                        }
                        var expanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                painter = painterResource(Res.drawable.more_vert),
                                contentDescription = "Show more options"
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    expanded = false
                                    onIntent(CarIntent.DeleteClicked)
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(Res.drawable.delete),
                                        tint = MaterialTheme.colorScheme.error,
                                        contentDescription = null
                                    )
                                },
                                text = {
                                    Text(
                                        text = stringResource(Res.string.delete),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onIntent(CarIntent.CreateRecordClicked) },
                text = { Text("Record") },
                icon = {
                    Icon(
                        painterResource(Res.drawable.plus),
                        contentDescription = null
                    )
                }
            )
        },
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        when (val carSection = state.carSection) {
            // Room answers in milliseconds, a spinner would only flicker
            CarSection.Loading -> Unit

            CarSection.NotFound -> CarNotFound(
                onBackClick = { onIntent(CarIntent.BackClicked) },
                modifier = Modifier.padding(innerPadding)
            )

            is CarSection.Loaded -> CarDetails(
                car = carSection.car,
                recordsSection = state.recordsSection,
                isShowDialog = state.isShowDialog,
                onIntent = onIntent,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun CarDetails(
    car: Car,
    recordsSection: RecordsSection,
    isShowDialog: Boolean,
    onIntent: (CarIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    DeleteDialog(
        isShow = isShowDialog,
        onDismiss = { onIntent(CarIntent.DeleteDismissed) },
        onConfirm = { onIntent(CarIntent.DeleteConfirmed) }
    )
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        item(key = "summary") { CarSummaryCard(car) }

        car.note?.let { note ->
            item(key = "note") { CarNoteCard(note) }
        }

        car.vin?.let {
            item(key = "details_header") { SectionHeader(stringResource(Res.string.car_section_details)) }
            item(key = "details") { CarDetailsCard(car) }
        }

        recordsDetails(
            recordsSection = recordsSection,
            onRecordClick = { onIntent(CarIntent.RecordClicked(it)) }
        )
    }
}

fun LazyListScope.recordsDetails(recordsSection: RecordsSection, onRecordClick: (ServiceRecordId) -> Unit) {
    item(key = "history_header") { SectionHeader(stringResource(Res.string.car_section_service_history)) }

    when (recordsSection) {
        is RecordsSection.Loaded -> {
            if (recordsSection.records.isEmpty()) {
                item(key = "history_empty") { ServiceHistoryEmpty() }
            } else {
                itemsIndexed(recordsSection.records, key = { _, record -> record.record.id.value }) { index, record ->

                    val isFirst = index == 0
                    val isLast = index == recordsSection.records.lastIndex

                    val shape = rememberAnimatedCardShape(
                        isFirst = isFirst,
                        isLast = isLast,
                        smallRadius = 8.dp,
                        largeRadius = 32.dp
                    )

                    ServiceRecordCard(
                        record = record,
                        shape = shape,
                        onClick = { onRecordClick(record.record.id) },
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }

        RecordsSection.Loading -> {
            // TODO: shimmer ?
        }
    }
}

@Composable
private fun ServiceRecordCard(
    record: ServiceRecordDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.elevatedShape
) {
    Card(
        onClick = onClick,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            ServiceAvatar()

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = record.category.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(Res.string.mileage_km, formatMileage(record.record.atMileage)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                record.record.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                painter = painterResource(Res.drawable.keyboard_arrow_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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

@Composable
private fun DeleteDialog(isShow: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    if (isShow) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(stringResource(Res.string.delete_car_title))
            },
            text = {
                Text(stringResource(Res.string.delete_car_text))
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirm
                ) {
                    Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.cancel))
                }
            },
            modifier = modifier
        )
    }
}

@Composable
private fun CarSummaryCard(car: Car) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomEnd = 8.dp, bottomStart = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            car.plate?.let { LicensePlate(it) }

            Column {
                Text(
                    text = stringResource(Res.string.field_mileage),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = stringResource(Res.string.mileage_km, formatMileage(car.mileage)),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun CarDetailsCard(car: Car, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        ListItem(
            leadingContent = {
                Icon(
                    painter = painterResource(Res.drawable.pin),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            overlineContent = { Text(stringResource(Res.string.field_vin)) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        ) {
            Text(car.vin ?: stringResource(Res.string.not_specified))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CarNoteCard(note: String, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    var overflows by remember { mutableStateOf(false) }

    Card(
        onClick = { expanded = !expanded },
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomEnd = 32.dp, bottomStart = 32.dp),
        enabled = overflows || expanded,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            disabledContentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .animateContentSize()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialShapes.Cookie6Sided.toShape())
                        .background(MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(
                        painterResource(Res.drawable.notes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Note",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text(
                text = note,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { overflows = it.hasVisualOverflow }
            )
            if (overflows || expanded) {
                Text(
                    text = stringResource(if (expanded) Res.string.show_less else Res.string.show_more),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
    )
}

@Composable
private fun ServiceHistoryEmpty() {
    Text(
        text = stringResource(Res.string.car_service_history_empty),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    )
}

@Composable
private fun CarNotFound(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.car_not_found),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.car_not_found_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onBackClick) {
            Text(stringResource(Res.string.go_back))
        }
    }
}

// endregion

// region Previews

private val previewCar = Car(
    id = CarId.random(),
    brand = "Nissan",
    model = "Qashqai",
    year = 2010,
    note = "My family car",
    vin = "SJNFAAJ10U1234567",
    plateCountry = PlateCountry.RU,
    plate = "С123ОМ77",
    mileage = 128_400,
    photoUri = null,
    createdAt = 0L,
    updatedAt = 0L
)

private val previewRecord = ServiceRecordDetails(
    record = ServiceRecord(
        id = ServiceRecordId.random(),
        carId = previewCar.id,
        atMileage = 120_000,
        price = 1500,
        categoryId = ServiceCategoryId.random(),
        note = "Replaced oil and oil filter",
        photoUri = null,
        date = 0L,
        createdAt = 0L,
        updatedAt = 0L
    ),
    category = ServiceCategory(
        id = ServiceCategoryId.random(),
        name = "Oil Change"
    )
)

@Preview(name = "Content", showBackground = true)
@Preview(name = "Content · dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CarContentPreview() = AppTheme {
    CarContent(
        state = CarState(
            carSection = CarSection.Loaded(previewCar),
            recordsSection = RecordsSection.Loaded(listOf(previewRecord))
        ),
        onIntent = {}
    )
}

@Preview(name = "Delete dialog", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CarContentDeleteDialogPreview() = AppTheme {
    CarContent(state = CarState(carSection = CarSection.Loaded(previewCar), isShowDialog = true), onIntent = {})
}

@Preview(name = "Optional fields missing", showBackground = true)
@Composable
private fun CarContentMinimalPreview() = AppTheme {
    CarContent(
        state = CarState(carSection = CarSection.Loaded(previewCar.copy(vin = null, plate = null))),
        onIntent = {}
    )
}

@Preview(name = "Not found", showBackground = true)
@Composable
private fun CarNotFoundPreview() = AppTheme {
    CarContent(state = CarState(carSection = CarSection.NotFound), onIntent = {})
}

// endregion
