package ru.haykor.carmanager.core.domain

import dev.zacsweers.metro.Inject
import ru.haykor.carmanager.core.domain.model.FieldError
import ru.haykor.carmanager.core.domain.model.RecordField
import ru.haykor.carmanager.core.domain.model.ServiceRecordDraft
import ru.haykor.carmanager.core.domain.model.ServiceRecordFormInput

@Inject
class ServiceRecordDraftParser {

    fun validate(input: ServiceRecordFormInput): Map<RecordField, FieldError> {
        val normalized = input.normalized()

        val validators = mapOf(
            RecordField.AT_MILEAGE to (required() + matches { it.toLongOrNull() in 0..MAX_MILEAGE }),
            RecordField.PRICE to optional(matches { it.toLongOrNull() != null && it.toLong() >= 0 }),
            RecordField.NOTE to optional(maxLength(NOTE_MAX_LENGTH))
        )

        return buildMap {
            for ((field, validator) in validators) {
                validator.validate(normalized.valueOf(field))?.let { put(field, it) }
            }
        }
    }

    fun parse(input: ServiceRecordFormInput): ServiceRecordParseResult {
        val normalized = input.normalized()
        val errors = validate(normalized)
        if (errors.isNotEmpty()) return ServiceRecordParseResult.Invalid(errors)

        return ServiceRecordParseResult.Valid(
            ServiceRecordDraft(
                carId = normalized.carId,
                atMileage = normalized.atMileage.toLong(),
                price = normalized.price.toLongOrNull(),
                categoryId = normalized.categoryId,
                note = normalized.note.ifEmpty { null },
                photoUri = normalized.photoUri,
                date = normalized.date
            )
        )
    }

    // Validate and parse the same normalized values so the saved draft matches validation.
    private fun ServiceRecordFormInput.normalized() = copy(
        atMileage = atMileage.trim(),
        price = price.trim(),
        note = note.trim()
    )

    private fun ServiceRecordFormInput.valueOf(field: RecordField): String = when (field) {
        RecordField.AT_MILEAGE -> atMileage
        RecordField.PRICE -> price
        RecordField.NOTE -> note
    }

    private companion object {
        const val MAX_MILEAGE = 9_999_999L
        const val NOTE_MAX_LENGTH = 500
    }
}

sealed interface ServiceRecordParseResult {
    data class Valid(val draft: ServiceRecordDraft) : ServiceRecordParseResult
    data class Invalid(val errors: Map<RecordField, FieldError>) : ServiceRecordParseResult
}
