package ru.haykor.carmanager.core.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.haykor.carmanager.core.domain.model.Car
import java.text.NumberFormat

/** Grouped by the current locale: 120000 → "120 000" / "120,000". */
@Composable
fun formatMileage(mileage: Long): String = remember(mileage) {
    NumberFormat.getIntegerInstance().format(mileage)
}

val Car.title: String
    get() = "$brand $model"
