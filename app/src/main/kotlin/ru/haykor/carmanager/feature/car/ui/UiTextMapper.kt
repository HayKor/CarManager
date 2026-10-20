package ru.haykor.carmanager.feature.car.ui

import ru.haykor.carmanager.R
import ru.haykor.carmanager.core.domain.error.DataError

fun DataError.toUiText() = when (this) {
    DataError.Local.Unknown -> R.string.error_unknown
}
