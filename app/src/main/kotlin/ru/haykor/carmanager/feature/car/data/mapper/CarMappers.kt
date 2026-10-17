package ru.haykor.carmanager.feature.car.data.mapper

import ru.haykor.carmanager.feature.car.data.local.entity.CarEntity
import ru.haykor.carmanager.feature.car.domain.model.Car

fun Car.toEntity() = CarEntity(
    id = id,
    brand = brand,
    model = model,
    year = year,
    vin = vin,
    plate = plate,
    mileage = mileage,
    photoUri = photoUri,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CarEntity.toDomain() = Car(
    id = id,
    brand = brand,
    model = model,
    year = year,
    vin = vin,
    plate = plate,
    mileage = mileage,
    photoUri = photoUri,
    createdAt = createdAt,
    updatedAt = updatedAt
)
