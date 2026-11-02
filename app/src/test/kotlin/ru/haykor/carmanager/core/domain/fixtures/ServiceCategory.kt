package ru.haykor.carmanager.core.domain.fixtures

import ru.haykor.carmanager.core.domain.ServiceCategoryId
import ru.haykor.carmanager.core.domain.model.ServiceCategory

fun aCategory(id: ServiceCategoryId = ServiceCategoryId.random(), name: String = "Brake test") = ServiceCategory(
    id = id,
    name = name
)
