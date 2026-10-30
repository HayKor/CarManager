package ru.haykor.carmanager.core.domain.model

sealed interface FieldError {
    data object Required : FieldError
    data object Invalid : FieldError
    data class TooLong(val maxLength: Int) : FieldError
}
