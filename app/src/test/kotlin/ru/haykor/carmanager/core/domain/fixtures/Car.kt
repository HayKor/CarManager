package ru.haykor.carmanager.core.domain.fixtures

import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.PlateCountry
import ru.haykor.carmanager.core.domain.model.Car

fun aCar(
    id: CarId = CarId.random(),
    brand: String = "Nissan",
    model: String = "Qashqai",
    year: Int = 2010,
    vin: String? = null,
    plateCountry: PlateCountry? = null,
    plate: String? = null,
    mileage: Long = 100_000,
    photoUri: String? = null,
    createdAt: Long = 0,
    updatedAt: Long = 0
) = Car(
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
