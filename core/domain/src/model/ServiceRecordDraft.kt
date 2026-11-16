package ru.haykor.carmanager.core.domain.model

import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceCategoryId

data class ServiceRecordDraft(
    val carId: CarId,
    val atMileage: Long,
    val price: Long?,
    val categoryId: ServiceCategoryId,
    val note: String?,
    val photoUri: String?,
    val date: Long
)
