package ru.haykor.carmanager.core.domain.fixtures

import ru.haykor.carmanager.core.domain.model.CarField
import ru.haykor.carmanager.core.domain.model.CarFieldError
import ru.haykor.carmanager.core.domain.model.Required

val allRequiredErrors: Map<CarField, CarFieldError> = mapOf(
    CarField.BRAND to Required,
    CarField.MODEL to Required,
    CarField.YEAR to Required,
    CarField.MILEAGE to Required
)
