package ru.haykor.carmanager.core.data.local.entity.service

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import ru.haykor.carmanager.core.data.local.entity.car.CarEntity
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceCategoryId
import ru.haykor.carmanager.core.domain.ServiceRecordId

@Entity(
    tableName = "service_records",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        ),

        ForeignKey(
            entity = ServiceCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("carId"), Index("categoryId")]
)
data class ServiceRecordEntity(
    @PrimaryKey val id: ServiceRecordId,
    val carId: CarId,
    val atMileage: Long,
    val price: Long?,
    val categoryId: ServiceCategoryId,
    val note: String?,
    val photoUri: String?,
    val date: Long,
    val createdAt: Long,
    val updatedAt: Long
)
