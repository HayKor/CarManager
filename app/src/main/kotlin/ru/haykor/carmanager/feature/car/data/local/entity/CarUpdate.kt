package ru.haykor.carmanager.feature.car.data.local.entity

import ru.haykor.carmanager.core.domain.CarId

data class CarUpdate(
    val id: CarId,
    val brand: String,
    val model: String,
    val year: Int?,
    val vin: String?,
    val plate: String?,
    val mileage: Long,
    val photoUri: String?,
    val updatedAt: Long
)
