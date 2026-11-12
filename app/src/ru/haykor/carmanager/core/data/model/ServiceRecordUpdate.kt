package ru.haykor.carmanager.core.data.model

import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceCategoryId
import ru.haykor.carmanager.core.domain.ServiceRecordId

data class ServiceRecordUpdate(
    val id: ServiceRecordId,
    val carId: CarId,
    val atMileage: Long,
    val price: Long?,
    val categoryId: ServiceCategoryId,
    val note: String?,
    val photoUri: String?,
    val date: Long,
    val updatedAt: Long
)
