package ru.haykor.carmanager.core.domain.model

import ru.haykor.carmanager.core.domain.PlateCountry

enum class Field {
    BRAND,
    MODEL,
    YEAR,
    MILEAGE,
    NOTE,
    VIN,
    PLATE
}

data class CarFormInput(
    val brand: String = "",
    val model: String = "",
    val year: String = "",
    val note: String = "",
    val mileage: String = "",
    val vin: String = "",
    val plate: String = "",
    val plateCountry: PlateCountry = PlateCountry.RU,
    val photoUri: String? = null
)

sealed interface FieldError {
    data object Required : FieldError
    data object Invalid : FieldError
    data class TooLong(val maxLength: Int) : FieldError
}
