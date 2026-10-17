package ru.haykor.carmanager.feature.garage.component

import androidx.compose.runtime.Stable
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.model.Car
import ru.haykor.carmanager.feature.garage.component.GarageState.Content

@Stable
data class GarageState(val query: String = "", val content: Content = Content.Loading) {
    sealed interface Content {
        data object Loading : Content
        data object Empty : Content
        data class Cars(val items: List<Car>) : Content
    }
}

sealed interface GarageIntent {
    data object AddCarClicked : GarageIntent
    data class CarClicked(val id: CarId) : GarageIntent
}

sealed interface GarageMutation {
    data object LoadingStarted : GarageMutation
    data class CarsLoaded(val items: List<Car>) : GarageMutation
}

object GarageReducer {
    fun reduce(state: GarageState, mutation: GarageMutation): GarageState = when (mutation) {
        is GarageMutation.CarsLoaded -> {
            state.copy(content = if (mutation.items.isEmpty()) Content.Empty else Content.Cars(mutation.items))
        }

        GarageMutation.LoadingStarted -> {
            state.copy(content = Content.Loading)
        }
    }
}
