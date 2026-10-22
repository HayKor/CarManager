package ru.haykor.carmanager.feature.car.component

import androidx.compose.runtime.Stable
import ru.haykor.carmanager.core.domain.model.Car

@Stable
sealed interface CarState {
    data object Loading : CarState
    data object NotFound : CarState
    data class Content(val car: Car) : CarState
}

sealed interface CarIntent {
    data object EditCarClicked : CarIntent
    data object BackClicked : CarIntent
}

sealed interface CarMutation {
    data object NotFound : CarMutation
    data object LoadingStarted : CarMutation
    data class CarLoaded(val car: Car) : CarMutation
}

object CarReducer {
    fun reduce(state: CarState, mutation: CarMutation): CarState = when (mutation) {
        is CarMutation.CarLoaded -> CarState.Content(mutation.car)
        CarMutation.NotFound -> CarState.NotFound
        CarMutation.LoadingStarted -> CarState.Loading
    }
}
