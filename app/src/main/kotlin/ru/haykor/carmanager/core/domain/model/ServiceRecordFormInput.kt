package ru.haykor.carmanager.core.domain.model

import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceCategoryId

enum class RecordField {
    AT_MILEAGE,
    NOTE,
    PRICE
}

data class ServiceRecordFormInput(
    val carId: CarId,
    val atMileage: String = "",
    val price: String = "",
    val categoryId: ServiceCategoryId,
    val note: String = "",
    val photoUri: String? = null,
    val date: Long
)
