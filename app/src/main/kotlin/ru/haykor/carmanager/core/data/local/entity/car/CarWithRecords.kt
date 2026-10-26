package ru.haykor.carmanager.core.data.local.entity.car

import androidx.room3.Embedded
import androidx.room3.Relation
import ru.haykor.carmanager.core.data.local.entity.service.ServiceRecordEntity
import ru.haykor.carmanager.core.data.local.entity.service.ServiceRecordWithCategory

data class CarWithRecords(
    @Embedded val car: CarEntity,
    @Relation(
        entity = ServiceRecordEntity::class,
        parentColumns = ["id"],
        entityColumns = ["carId"]
    ) val records: List<ServiceRecordWithCategory>
)
