package ru.haykor.carmanager.feature.car.domain.model

data class CarDraft(
    val brand: String,
    val model: String,
    val year: Int?,
    val vin: String?,
    val plate: String?,
    val mileage: Long,
    val photoUri: String?
)
