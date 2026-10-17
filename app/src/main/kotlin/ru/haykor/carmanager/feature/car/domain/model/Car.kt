package ru.haykor.carmanager.feature.car.domain.model

import ru.haykor.carmanager.core.domain.CarId

data class Car(
    val id: CarId,
    val brand: String,
    val model: String,
    val year: Int?,
    val vin: String?,
    val plate: String?,
    val mileage: Long,
    val photoUri: String?,
    val createdAt: Long,
    val updatedAt: Long
)
