package ru.haykor.carmanager.core.domain.fixtures

import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceCategoryId
import ru.haykor.carmanager.core.domain.ServiceRecordId
import ru.haykor.carmanager.core.domain.model.ServiceRecord


fun aRecord(
    id: ServiceRecordId = ServiceRecordId.random(),
    carId: CarId = CarId.random(),
    atMileage: Long = 140_000,
    price: Long = 5_600,
    categoryId: ServiceCategoryId = ServiceCategoryId.random(),
    note: String? = "Some note",
    photoUri: String? = null,
    date: Long = 0L,
    createdAt: Long = 0L,
    updatedAt: Long = 0L
) = ServiceRecord(
    id = id,
    carId = carId,
    atMileage = atMileage,
    price = price,
    categoryId = categoryId,
    note = note,
    photoUri = photoUri,
    date = date,
    createdAt = createdAt,
    updatedAt = updatedAt
)
