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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ru.haykor.carmanager.Res
import ru.haykor.carmanager.core.designsystem.CmNavRow
import ru.haykor.carmanager.core.designsystem.LicensePlate
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.PlateCountry
import ru.haykor.carmanager.core.domain.model.Car
import ru.haykor.carmanager.core.ui.theme.AppTheme
import ru.haykor.carmanager.directions_car
import ru.haykor.carmanager.mileage_km

@Composable
fun CarCard(car: Car, onClick: () -> Unit, modifier: Modifier = Modifier, shape: Shape = RectangleShape) {
    CmNavRow(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        leadingContent = { CarAvatar() },
        supportingContent = {
            Column {
                Text(stringResource(Res.string.mileage_km, formatNumber(car.mileage)))
                car.plate?.let {
                    Spacer(Modifier.height(8.dp))
                    LicensePlate(plate = it)
                }
            }
        }
    ) {
        Text(
            text = car.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
            painterResource(Res.drawable.directions_car),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

// region preview
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
