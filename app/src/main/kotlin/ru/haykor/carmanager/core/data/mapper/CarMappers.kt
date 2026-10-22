package ru.haykor.carmanager.core.data.mapper

import ru.haykor.carmanager.core.data.local.entity.car.CarEntity
import ru.haykor.carmanager.core.data.local.entity.car.CarUpdate
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.model.Car
import ru.haykor.carmanager.core.domain.model.CarDraft

fun CarEntity.toDomain() = Car(
    id = id,
    brand = brand,
    model = model,
    year = year,
    vin = vin,
    plateCountry = plateCountry,
    plate = plate,
    mileage = mileage,
    photoUri = photoUri,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CarDraft.toEntity(id: CarId, createdAt: Long, updatedAt: Long) = CarEntity(
    id = id,
    brand = brand,
    model = model,
    year = year,
    vin = vin,
    plateCountry = plateCountry,
    plate = plate,
    mileage = mileage,
    photoUri = photoUri,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CarDraft.toUpdate(id: CarId, now: Long) = CarUpdate(
    id = id,
    brand = brand,
    model = model,
    year = year,
    vin = vin,
    plateCountry = plateCountry,
    plate = plate,
    mileage = mileage,
    photoUri = photoUri,
    updatedAt = now
)
