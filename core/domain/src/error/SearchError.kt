package ru.haykor.carmanager.core.domain.error

import ru.haykor.carmanager.core.result.AppError

sealed interface SearchError : AppError {
    data class Data(val dataError: DataError) : SearchError
}
