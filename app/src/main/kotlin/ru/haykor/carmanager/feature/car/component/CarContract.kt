package ru.haykor.carmanager.feature.car.component

import androidx.compose.runtime.Immutable
import ru.haykor.carmanager.core.domain.ServiceRecordId
import ru.haykor.carmanager.core.domain.error.DataError
import ru.haykor.carmanager.core.domain.model.Car
import ru.haykor.carmanager.core.domain.model.ServiceRecordDetails

@Immutable
data class CarState(
    val carSection: CarSection = CarSection.Loading,
    val recordsSection: RecordsSection = RecordsSection.Loading,
    val isShowDialog: Boolean = false,
    val isDeleting: Boolean = false
)

sealed interface CarSection {
    data object Loading : CarSection
    data object NotFound : CarSection
    data class Loaded(val car: Car) : CarSection
}

sealed interface RecordsSection {
    data object Loading : RecordsSection
    data class Loaded(val records: List<ServiceRecordDetails>) : RecordsSection
}

sealed interface CarIntent {
    data object EditCarClicked : CarIntent
    data class EditServiceClicked(val recordId: ServiceRecordId) : CarIntent
    data object BackClicked : CarIntent

    data object DeleteClicked : CarIntent
    data object DeleteDismissed : CarIntent
    data object DeleteConfirmed : CarIntent
}

sealed interface CarEvent {
    data class DeleteFailed(val error: DataError) : CarEvent
}

sealed interface CarMutation {
    data object CarNotFound : CarMutation
    data class CarLoaded(val car: Car) : CarMutation
    data class RecordsLoaded(val records: List<ServiceRecordDetails>) : CarMutation

    data class SetShowDialog(val value: Boolean) : CarMutation
    data class SetIsDeleting(val value: Boolean) : CarMutation
}

object CarReducer {
    fun reduce(state: CarState, mutation: CarMutation): CarState = when (mutation) {
        is CarMutation.CarLoaded -> state.copy(carSection = CarSection.Loaded(mutation.car))

        is CarMutation.RecordsLoaded -> state.copy(recordsSection = RecordsSection.Loaded(mutation.records))

        CarMutation.CarNotFound -> {
            if (state.isDeleting) state else state.copy(carSection = CarSection.NotFound)
        }

        is CarMutation.SetIsDeleting -> {
            state.copy(isDeleting = mutation.value)
        }

        is CarMutation.SetShowDialog -> {
            state.copy(isShowDialog = mutation.value)
        }
    }
}
