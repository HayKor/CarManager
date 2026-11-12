package ru.haykor.carmanager.core.domain.fixtures

import ru.haykor.carmanager.core.domain.model.CarField
import ru.haykor.carmanager.core.domain.model.FieldError

val allRequiredErrors = mapOf(
    CarField.BRAND to FieldError.Required,
    CarField.MODEL to FieldError.Required,
    CarField.YEAR to FieldError.Required,
    CarField.MILEAGE to FieldError.Required
)
