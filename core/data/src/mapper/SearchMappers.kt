package ru.haykor.carmanager.core.data.mapper

import ru.haykor.carmanager.core.data.local.entity.car.CarWithRecords
import ru.haykor.carmanager.core.data.local.entity.service.ServiceRecordWithCategory
import ru.haykor.carmanager.core.data.model.CarRecordSearchRow
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.model.Car
import ru.haykor.carmanager.core.domain.model.CarSearchResult
import ru.haykor.carmanager.core.domain.model.ServiceCategory
import ru.haykor.carmanager.core.domain.model.ServiceRecord
import ru.haykor.carmanager.core.domain.model.ServiceRecordDetails

fun CarRecordSearchRow.toRecordDetails(): ServiceRecordDetails? {
    val recordId = recordId ?: return null
    val categoryId = categoryId!!
    return ServiceRecordDetails(
        record = ServiceRecord(
            id = recordId,
            carId = recordCarId!!,
            atMileage = atMileage!!,
            price = price,
            categoryId = categoryId,
            note = note,
            photoUri = photoUri,
            date = date!!,
            createdAt = recordCreatedAt!!,
            updatedAt = recordUpdatedAt!!
        ),
        category = ServiceCategory(id = categoryId, name = categoryName.orEmpty())
    )
}

/** Groups rows by car in SQL order; rows without a record leave the car with an empty list. */
fun List<CarRecordSearchRow>.toDomainGroups(): List<CarSearchResult> {
    val cars = LinkedHashMap<CarId, Car>()
    val records = LinkedHashMap<CarId, MutableList<ServiceRecordDetails>>()
    forEach { row ->
        cars.getOrPut(row.car.id) { row.car.toDomain() }
        val carRecords = records.getOrPut(row.car.id) { mutableListOf() }
        row.toRecordDetails()?.let(carRecords::add)
    }
    return cars.map { (id, car) -> CarSearchResult(car = car, records = records.getValue(id)) }
}

/** Maps the full record history of a car, newest record first. */
fun CarWithRecords.toDomainGroup() = CarSearchResult(
    car = car.toDomain(),
    records = records.map(ServiceRecordWithCategory::toDetails).sortedByDescending { it.record.date }
)
