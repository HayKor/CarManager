package ru.haykor.carmanager.core.data.mapper

import ru.haykor.carmanager.core.data.local.entity.service.ServiceRecordEntity
import ru.haykor.carmanager.core.data.local.entity.service.ServiceRecordUpdate
import ru.haykor.carmanager.core.data.local.entity.service.ServiceRecordWithCategory
import ru.haykor.carmanager.core.domain.ServiceRecordId
import ru.haykor.carmanager.core.domain.model.ServiceRecord
import ru.haykor.carmanager.core.domain.model.ServiceRecordDetails
import ru.haykor.carmanager.core.domain.model.ServiceRecordDraft

fun ServiceRecordDraft.toEntity(id: ServiceRecordId, createdAt: Long, updatedAt: Long) = ServiceRecordEntity(
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

fun ServiceRecordDraft.toUpdate(id: ServiceRecordId, updatedAt: Long) = ServiceRecordUpdate(
    id = id,
    carId = carId,
    atMileage = atMileage,
    price = price,
    categoryId = categoryId,
    note = note,
    photoUri = photoUri,
    date = date,
    updatedAt = updatedAt
)

fun ServiceRecordEntity.toDomain() = ServiceRecord(
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

fun ServiceRecordWithCategory.toDetails() = ServiceRecordDetails(
    record = record.toDomain(),
    category = category.toDomain()
)
