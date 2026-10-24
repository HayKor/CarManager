package ru.haykor.carmanager.feature.garage.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.haykor.carmanager.R
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.PlateCountry
import ru.haykor.carmanager.core.domain.model.Car
import ru.haykor.carmanager.core.presentation.ui.LicensePlate
import ru.haykor.carmanager.core.presentation.ui.formatMileage
import ru.haykor.carmanager.core.presentation.ui.title
import ru.haykor.carmanager.feature.garage.component.GarageComponent
import ru.haykor.carmanager.feature.garage.component.GarageIntent
import ru.haykor.carmanager.feature.garage.component.GarageState
import ru.haykor.carmanager.ui.theme.AppTheme

@Composable
fun GarageScreen(component: GarageComponent, modifier: Modifier = Modifier) {
    val state by component.state.subscribeAsState()

    GarageContent(
        state = state,
        onIntent = component::accept,
        modifier = modifier
    )
}

@Composable
internal fun GarageContent(state: GarageState, onIntent: (GarageIntent) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { onIntent(GarageIntent.AddCarClicked) }) {
                Icon(painterResource(R.drawable.plus), contentDescription = stringResource(R.string.garage_add_car))
            }
        },
        modifier = modifier
    ) { padding ->
        when (val content = state.content) {
            GarageState.Content.Loading -> Unit

            GarageState.Content.Empty -> GarageEmpty(
                onAddClick = { onIntent(GarageIntent.AddCarClicked) },
                modifier = Modifier.padding(padding)
            )

            is GarageState.Content.Cars -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = padding,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(content.items, key = { it.id.value }) { car ->
                    CarCard(
                        car = car,
                        onClick = { onIntent(GarageIntent.CarClicked(car.id)) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GarageEmpty(onAddClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.garage_empty_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onAddClick) { Text(stringResource(R.string.garage_add_car)) }
    }
}

@Composable
private fun CarCard(car: Car, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            CarAvatar()

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = car.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.mileage_km, formatMileage(car.mileage)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                car.plate?.let {
                    Spacer(Modifier.height(8.dp))
                    LicensePlate(plate = it)
                }
            }

            Icon(
                painterResource(R.drawable.keyboard_arrow_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CarAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(R.drawable.directions_car),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

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

@Preview(showBackground = true)
@Composable
private fun CarCardPreview() = AppTheme {
    CarCard(
        car = previewCar,
        onClick = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun GarageEmptyPreview() = AppTheme {
    GarageEmpty(onAddClick = {}, modifier = Modifier.fillMaxSize())
}
