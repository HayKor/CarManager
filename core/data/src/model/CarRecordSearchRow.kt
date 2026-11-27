package ru.haykor.carmanager.core.data.model

import androidx.room3.ColumnInfo
import androidx.room3.Embedded
import ru.haykor.carmanager.core.data.local.entity.car.CarEntity
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceCategoryId
import ru.haykor.carmanager.core.domain.ServiceRecordId

data class CarRecordSearchRow(
    @Embedded(prefix = "car_") val car: CarEntity,
    @ColumnInfo(name = "record_id") val recordId: ServiceRecordId?,
    @ColumnInfo(name = "record_carId") val recordCarId: CarId?,
    @ColumnInfo(name = "record_atMileage") val atMileage: Long?,
    @ColumnInfo(name = "record_price") val price: Long?,
    @ColumnInfo(name = "record_categoryId") val categoryId: ServiceCategoryId?,
    @ColumnInfo(name = "record_note") val note: String?,
    @ColumnInfo(name = "record_photoUri") val photoUri: String?,
    @ColumnInfo(name = "record_date") val date: Long?,
    @ColumnInfo(name = "record_createdAt") val recordCreatedAt: Long?,
    @ColumnInfo(name = "record_updatedAt") val recordUpdatedAt: Long?,
    @ColumnInfo(name = "record_categoryName") val categoryName: String?
)
