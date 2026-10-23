package ru.haykor.carmanager.feature.car.component

import androidx.compose.runtime.Stable
import ru.haykor.carmanager.core.domain.error.DataError
import ru.haykor.carmanager.core.domain.model.Car
import ru.haykor.carmanager.feature.car.component.CarState.Content
import ru.haykor.carmanager.feature.car.component.CarState.Loading
import ru.haykor.carmanager.feature.car.component.CarState.NotFound

@Stable
sealed interface CarState {
    data object Loading : CarState
    data object NotFound : CarState
    data class Content(val car: Car, val isShowDialog: Boolean = false, val isDeleting: Boolean = false) : CarState
}

sealed interface CarIntent {
    data object EditCarClicked : CarIntent
    data object BackClicked : CarIntent

    data object DeleteClicked : CarIntent
    data object DeleteDismissed : CarIntent
    data object DeleteConfirmed : CarIntent
}

sealed interface CarEvent {
    data class DeleteFailed(val error: DataError) : CarEvent
}

sealed interface CarMutation {
    data object NotFound : CarMutation
    data object LoadingStarted : CarMutation
    data class CarLoaded(val car: Car) : CarMutation

    data class SetShowDialog(val value: Boolean) : CarMutation
    data class SetIsDeleting(val value: Boolean) : CarMutation
}

object CarReducer {
    fun reduce(state: CarState, mutation: CarMutation): CarState = when (mutation) {
        is CarMutation.CarLoaded -> Content(mutation.car)

        CarMutation.NotFound -> {
            if (state is Content && state.isDeleting) state else NotFound
        }

        CarMutation.LoadingStarted -> Loading

        is CarMutation.SetIsDeleting -> {
            (state as? Content)?.copy(isDeleting = mutation.value) ?: state
        }

        is CarMutation.SetShowDialog -> {
            (state as? Content)?.copy(isShowDialog = mutation.value) ?: state
        }
    }
}
