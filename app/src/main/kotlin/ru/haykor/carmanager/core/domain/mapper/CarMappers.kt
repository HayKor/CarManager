package ru.haykor.carmanager.core.domain.mapper

import ru.haykor.carmanager.core.domain.PlateCountry
import ru.haykor.carmanager.core.domain.model.Car
import ru.haykor.carmanager.core.domain.model.CarFormInput

fun Car.toFormInput() = CarFormInput(
    brand = brand,
    model = model,
    year = year.toString(),
    mileage = mileage.toString(),
    vin = vin.orEmpty(),
    plate = plate.orEmpty(),
    plateCountry = plateCountry ?: PlateCountry.RU,
    photoUri = photoUri
)
