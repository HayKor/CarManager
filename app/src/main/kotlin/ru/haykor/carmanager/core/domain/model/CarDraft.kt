package ru.haykor.carmanager.core.domain.model

import ru.haykor.carmanager.core.domain.PlateCountry

data class CarDraft(
    val brand: String,
    val model: String,
    val year: Int,
    val vin: String?,
    val plateCountry: PlateCountry?,
    val plate: String?,
    val mileage: Long,
    val photoUri: String?
)
