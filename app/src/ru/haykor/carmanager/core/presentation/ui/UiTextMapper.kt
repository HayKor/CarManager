package ru.haykor.carmanager.core.presentation.ui

import org.jetbrains.compose.resources.StringResource
import ru.haykor.carmanager.Res
import ru.haykor.carmanager.core.domain.error.DataError
import ru.haykor.carmanager.error_unknown

fun DataError.toUiText(): StringResource = when (this) {
    DataError.Local.Unknown -> Res.string.error_unknown
}
