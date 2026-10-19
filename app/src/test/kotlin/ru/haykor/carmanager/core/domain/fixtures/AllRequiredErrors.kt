package ru.haykor.carmanager.core.domain.fixtures

import ru.haykor.carmanager.core.domain.model.Field
import ru.haykor.carmanager.core.domain.model.FieldError

val allRequiredErrors = mapOf(
    Field.BRAND to FieldError.Required,
    Field.MODEL to FieldError.Required,
    Field.YEAR to FieldError.Required,
    Field.MILEAGE to FieldError.Required
)
