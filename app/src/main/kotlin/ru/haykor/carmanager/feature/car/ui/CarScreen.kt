package ru.haykor.carmanager.feature.car.ui

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.haykor.carmanager.R
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.PlateCountry
import ru.haykor.carmanager.core.domain.model.Car
import ru.haykor.carmanager.core.presentation.coroutines.ObserveAsEvents
import ru.haykor.carmanager.core.presentation.ui.LicensePlate
import ru.haykor.carmanager.core.presentation.ui.formatMileage
import ru.haykor.carmanager.core.presentation.ui.title
import ru.haykor.carmanager.core.presentation.ui.toUiText
import ru.haykor.carmanager.feature.car.component.CarComponent
import ru.haykor.carmanager.feature.car.component.CarEvent
import ru.haykor.carmanager.feature.car.component.CarIntent
import ru.haykor.carmanager.feature.car.component.CarState
import ru.haykor.carmanager.ui.theme.AppTheme

@Composable
fun CarScreen(component: CarComponent, modifier: Modifier = Modifier) {
    val state by component.state.subscribeAsState()

    val context = LocalContext.current
    ObserveAsEvents(component.events) { event ->
        when (event) {
            is CarEvent.DeleteFailed -> {
                Toast.makeText(context, event.error.toUiText(), Toast.LENGTH_LONG).show()
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
            LargeTopAppBar(
                title = {
                    Text(
                        text = (state as? CarState.Content)?.car?.title ?: stringResource(R.string.car_info_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onIntent(CarIntent.BackClicked) }) {
                        Icon(
                            painter = painterResource(R.drawable.chevron_left),
                            contentDescription = stringResource(R.string.go_back)
                        )
                    }
                },
                actions = {
                    if (state is CarState.Content) {
                        IconButton(onClick = { onIntent(CarIntent.EditCarClicked) }) {
                            Icon(
                                painter = painterResource(R.drawable.edit),
                                contentDescription = stringResource(R.string.description_edit)
                            )
                        }
                        var expanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
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
                                        painter = painterResource(R.drawable.delete),
                                        tint = MaterialTheme.colorScheme.error,
                                        contentDescription = null
                                    )
                                },
                                text = {
                                    Text(
                                        text = stringResource(R.string.delete),
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
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        when (state) {
            // Room answers in milliseconds, a spinner would only flicker
            CarState.Loading -> Unit

            CarState.NotFound -> CarNotFound(
                onBackClick = { onIntent(CarIntent.BackClicked) },
                modifier = Modifier.padding(innerPadding)
            )

            is CarState.Content -> CarDetails(
                state = state,
                onIntent = onIntent,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

/** A lazy list from the start: service records will be appended below the car info. */
@Composable
private fun CarDetails(state: CarState.Content, onIntent: (CarIntent) -> Unit, modifier: Modifier = Modifier) {
    DeleteDialog(
        isShow = state.isShowDialog,
        onDismiss = { onIntent(CarIntent.DeleteDismissed) },
        onConfirm = { onIntent(CarIntent.DeleteConfirmed) }
    )
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "summary") { CarSummaryCard(state.car) }

        item(key = "details_header") { SectionHeader(stringResource(R.string.car_section_details)) }
        item(key = "details") { CarDetailsCard(state.car) }

        item(key = "history_header") { SectionHeader(stringResource(R.string.car_section_service_history)) }
        item(key = "history_empty") { ServiceHistoryEmpty() }
    }
}

@Composable
private fun DeleteDialog(isShow: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    if (isShow) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(stringResource(R.string.delete_car_title))
            },
            text = {
                Text(stringResource(R.string.delete_car_text))
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirm
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = car.year.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                car.plate?.let { LicensePlate(it) }
            }

            Column {
                Text(
                    text = stringResource(R.string.field_mileage),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = stringResource(R.string.mileage_km, formatMileage(car.mileage)),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun CarDetailsCard(car: Car) {
    val notSpecified = stringResource(R.string.not_specified)

    Card(modifier = Modifier.fillMaxWidth()) {
        DetailRow(stringResource(R.string.field_brand), car.brand)
        RowDivider()
        DetailRow(stringResource(R.string.field_model), car.model)
        RowDivider()
        DetailRow(stringResource(R.string.field_year), car.year.toString())
        RowDivider()
        DetailRow(stringResource(R.string.field_plate), car.plate ?: notSpecified, isMissing = car.plate == null)
        RowDivider()
        DetailRow(
            label = stringResource(R.string.field_vin),
            value = car.vin ?: notSpecified,
            isMissing = car.vin == null,
            monospace = car.vin != null
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String, isMissing: Boolean = false, monospace: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = if (monospace) FontFamily.Monospace else null,
            color = if (isMissing) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun ServiceHistoryEmpty() {
    Text(
        text = stringResource(R.string.car_service_history_empty),
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
            text = stringResource(R.string.car_not_found),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.car_not_found_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onBackClick) {
            Text(stringResource(R.string.go_back))
        }
    }
}

// region Previews

private val previewCar = Car(
    id = CarId.random(),
    brand = "Nissan",
    model = "Qashqai",
    year = 2010,
    vin = "SJNFAAJ10U1234567",
    plateCountry = PlateCountry.RU,
    plate = "С123ОМ77",
    mileage = 128_400,
    photoUri = null,
    createdAt = 0L,
    updatedAt = 0L
)

@Preview(name = "Content", showBackground = true)
@Preview(name = "Content · dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CarContentPreview() = AppTheme {
    CarContent(state = CarState.Content(previewCar), onIntent = {})
}

@Preview(name = "Delete dialog", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CarContentDeleteDialogPreview() = AppTheme {
    CarContent(state = CarState.Content(previewCar, isShowDialog = true), onIntent = {})
}

@Preview(name = "Optional fields missing", showBackground = true)
@Composable
private fun CarContentMinimalPreview() = AppTheme {
    CarContent(
        state = CarState.Content(previewCar.copy(vin = null, plate = null)),
        onIntent = {}
    )
}

@Preview(name = "Not found", showBackground = true)
@Composable
private fun CarNotFoundPreview() = AppTheme {
    CarContent(state = CarState.NotFound, onIntent = {})
}

// endregion
