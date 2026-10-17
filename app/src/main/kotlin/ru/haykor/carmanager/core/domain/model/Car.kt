package ru.haykor.carmanager.core.domain.model

import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.PlateCountry

data class Car(
    val id: CarId,
    val brand: String,
    val model: String,
    val year: Int?,
    val vin: String?,
    val plateCountry: PlateCountry?,
    val plate: String?,
    val mileage: Long,
    val photoUri: String?,
    val createdAt: Long,
    val updatedAt: Long
)
