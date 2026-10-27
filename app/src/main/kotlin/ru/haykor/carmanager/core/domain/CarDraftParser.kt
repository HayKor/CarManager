package ru.haykor.carmanager.core.domain

import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ru.haykor.carmanager.core.domain.model.CarDraft
import ru.haykor.carmanager.core.domain.model.CarField
import ru.haykor.carmanager.core.domain.model.CarFormInput
import ru.haykor.carmanager.core.domain.model.FieldError

@Inject
class CarDraftParser(private val plateFormats: PlateFormats, private val clock: Clock) {

    fun validate(input: CarFormInput): Map<CarField, FieldError> {
        val normalized = input.normalized()
        val plateFormat = plateFormats.of(normalized.plateCountry)

        val validators = mapOf(
            CarField.BRAND to (required() + maxLength(BRAND_MAX_LENGTH)),
            CarField.MODEL to (required() + maxLength(MODEL_MAX_LENGTH)),
            CarField.YEAR to (required() + matches { it.toIntOrNull() in MIN_YEAR..maxYear() }),
            CarField.MILEAGE to (required() + matches { it.toLongOrNull() in 0..MAX_MILEAGE }),
            CarField.NOTE to optional(maxLength(NOTE_MAX_LENGTH)),
            CarField.VIN to optional(matches(VIN_REGEX::matches)),
            CarField.PLATE to optional(matches(plateFormat::isValid))
        )

        return buildMap {
            for ((field, validator) in validators) {
                validator.validate(normalized.valueOf(field))?.let { put(field, it) }
            }
        }
    }

    fun parse(input: CarFormInput): CarParseResult {
        val errors = validate(input)
        if (errors.isNotEmpty()) return CarParseResult.Invalid(errors)

        val normalized = input.normalized()
        return CarParseResult.Valid(
            CarDraft(
                brand = normalized.brand,
                model = normalized.model,
                year = normalized.year.toInt(),
                vin = normalized.vin.ifEmpty { null },
                plateCountry = normalized.plateCountry,
                plate = normalized.plate.ifEmpty { null },
                mileage = normalized.mileage.toLong(),
                note = normalized.note.ifEmpty { null },
                photoUri = normalized.photoUri
            )
        )
    }

    // Validation and parsing both read from here, so the saved value is exactly the validated one
    private fun CarFormInput.normalized() = copy(
        brand = brand.trim(),
        model = model.trim(),
        year = year.trim(),
        mileage = mileage.trim(),
        note = note.trim(),
        vin = vin.trim().uppercase(),
        plate = plateFormats.of(plateCountry).normalize(plate)
    )

    private fun CarFormInput.valueOf(field: CarField): String = when (field) {
        CarField.BRAND -> brand
        CarField.MODEL -> model
        CarField.YEAR -> year
        CarField.NOTE -> note
        CarField.MILEAGE -> mileage
        CarField.VIN -> vin
        CarField.PLATE -> plate
    }

    // Next year's models go on sale in autumn
    private fun maxYear(): Int = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).year + 1

    private companion object {
        const val BRAND_MAX_LENGTH = 30
        const val MODEL_MAX_LENGTH = 40
        const val NOTE_MAX_LENGTH = 50
        const val MIN_YEAR = 1900
        const val MAX_MILEAGE = 9_999_999L

        // 17 chars, I/O/Q are never used
        val VIN_REGEX = Regex("^[A-HJ-NPR-Z0-9]{17}$")
    }
}

sealed interface CarParseResult {
    data class Valid(val draft: CarDraft) : CarParseResult
    data class Invalid(val errors: Map<CarField, FieldError>) : CarParseResult
}
