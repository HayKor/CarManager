package ru.haykor.carmanager.core.domain.error

import ru.haykor.carmanager.core.result.AppError

sealed interface DataError : AppError {
    sealed interface Local : DataError {
        data object Unknown : Local
    }
}
