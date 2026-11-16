package ru.haykor.carmanager.core.ui

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import ru.haykor.carmanager.core.domain.model.Car
import java.text.NumberFormat
import kotlin.time.Clock
import kotlin.time.Instant

/** Grouped by the current locale: 120000 → "120 000" / "120,000". */
fun formatNumber(mileage: Long): String = NumberFormat.getIntegerInstance().format(mileage)

private val MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

/** "12 Mar" for the current year, "12 Mar 2025" otherwise. */
fun formatDate(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val date = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone).date
    val currentYear = Clock.System.now().toLocalDateTime(timeZone).year
    val base = "${date.day} ${MONTHS[date.month.number - 1]}"
    return if (date.year == currentYear) base else "$base ${date.year}"
}

val Car.title: String
    get() = "$brand $model, $year"
