package ru.haykor.carmanager.core.domain.usecase

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceRecordDraftParser
import ru.haykor.carmanager.core.domain.ServiceRecordParseResult
import ru.haykor.carmanager.core.domain.error.DataError
import ru.haykor.carmanager.core.domain.model.FieldError
import ru.haykor.carmanager.core.domain.model.RecordField
import ru.haykor.carmanager.core.domain.model.ServiceRecordFormInput
import ru.haykor.carmanager.core.domain.repository.CarRepository
import ru.haykor.carmanager.core.domain.repository.ServiceRecordRepository
import ru.haykor.carmanager.core.result.AppError
import ru.haykor.carmanager.core.result.DataResult
import ru.haykor.carmanager.core.result.EmptyResult
import ru.haykor.carmanager.core.result.getOrReturn
import ru.haykor.carmanager.core.result.mapError
import ru.haykor.carmanager.feature.serviceEditor.component.EditorMode

enum class MileageResolution {
    ASK, // shows dialog if mismatch ([atMileage] < [car.mileage])
    UPDATE_CAR, // update car's mileage to match [atMileage]
    KEEP_CAR // leave it as is
}

@Inject
class SaveServiceRecordUseCase(
    private val records: ServiceRecordRepository,
    private val cars: CarRepository,
    private val parser: ServiceRecordDraftParser
) {
    suspend operator fun invoke(
        mode: EditorMode,
        input: ServiceRecordFormInput,
        carId: CarId,
        resolution: MileageResolution = MileageResolution.ASK
    ): EmptyResult<SaveRecordError> {
        val draft = when (val parsed = parser.parse(input)) {
            is ServiceRecordParseResult.Invalid ->
                return DataResult.Failure(SaveRecordError.ParseFailure(parsed.errors))

            is ServiceRecordParseResult.Valid -> parsed.draft
        }

        val carMileage = cars.observeCar(carId).first()?.mileage ?: return DataResult.Failure(
            SaveRecordError.Data(
                DataError.Local.Unknown
            )
        )

        val excludeId = (mode as? EditorMode.Edit)?.recordId
        val bounds = records.mileageBounds(carId, draft.date, excludeId)
            .getOrReturn { return DataResult.Failure(SaveRecordError.Data(it)) }

        val lower = bounds.lower
        val upper = bounds.upper
        val mileage = draft.atMileage
        if ((lower != null && mileage < lower) || (upper != null && mileage > upper)) {
            return DataResult.Failure(SaveRecordError.MileageOutOfSequence(lower, upper, mileage))
        }
        val isLatest = upper == null

        if (resolution == MileageResolution.ASK && isLatest && draft.atMileage < carMileage) {
            return DataResult.Failure(SaveRecordError.MileageMismatch(carMileage, draft.atMileage))
        }

        val newCarMileage: Long? = draft.atMileage.takeIf {
            isLatest && it != carMileage && (resolution == MileageResolution.UPDATE_CAR || it > carMileage)
        }
        return when (mode) {
            is EditorMode.Create -> records.createWithMileage(
                draft = draft,
                newCarMileage = newCarMileage
            )

            is EditorMode.Edit -> records.updateWithMileage(
                mode.recordId,
                draft,
                newCarMileage = newCarMileage
            )
        }.mapError(SaveRecordError::Data)
    }
}

sealed interface SaveRecordError : AppError {
    data class ParseFailure(val errors: Map<RecordField, FieldError>) : SaveRecordError
    data class MileageMismatch(val carMileage: Long, val recordMileage: Long) : SaveRecordError
    data class MileageOutOfSequence(val min: Long?, val max: Long?, val recordMileage: Long) : SaveRecordError
    data class Data(val dataError: DataError) : SaveRecordError
}
