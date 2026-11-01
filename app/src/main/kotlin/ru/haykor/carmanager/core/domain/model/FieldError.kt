package ru.haykor.carmanager.core.domain.model

sealed interface FieldError

sealed interface CarFieldError : FieldError
sealed interface RecordFieldError : FieldError

sealed interface CommonFieldError :
    CarFieldError,
    RecordFieldError
data object Required : CommonFieldError
data object Invalid : CommonFieldError
data class TooLong(val maxLength: Int) : CommonFieldError

data class MileageOutOfSequence(val min: Long?, val max: Long?, val recordMileage: Long) : RecordFieldError
