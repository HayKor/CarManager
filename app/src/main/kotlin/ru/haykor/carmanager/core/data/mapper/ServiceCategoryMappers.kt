package ru.haykor.carmanager.core.data.mapper

import ru.haykor.carmanager.core.data.local.entity.service.ServiceCategoryEntity
import ru.haykor.carmanager.core.domain.model.ServiceCategory


fun ServiceCategoryEntity.toDomain() = ServiceCategory(
    id = id,
    name = name
)
