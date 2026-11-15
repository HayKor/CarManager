package ru.haykor.carmanager.core.data.local.entity.service

import androidx.room3.Embedded
import androidx.room3.Relation

data class ServiceRecordWithCategory(
    @Embedded val record: ServiceRecordEntity,
    @Relation(parentColumns = ["categoryId"], entityColumns = ["id"])
    val category: ServiceCategoryEntity
)
