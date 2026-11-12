package ru.haykor.carmanager.feature.garage.component

import androidx.compose.runtime.Stable
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.model.Car
import ru.haykor.carmanager.feature.garage.component.GarageState.Content

@Stable
data class GarageState(val query: String = "", val content: Content = Content.Loading) {
    sealed interface Content {
        data object Loading : Content
        data object EmptyGarage : Content
        data class EmptySearch(val query: String) : Content
        data class Cars(val items: List<Car>) : Content
    }
}

sealed interface GarageIntent {
    data object AddCarClicked : GarageIntent
    data class CarClicked(val id: CarId) : GarageIntent
    data class QueryChanged(val query: String) : GarageIntent
    data object ClearQueryClicked : GarageIntent
}

sealed interface GarageMutation {
    data object LoadingStarted : GarageMutation
    data class CarsLoaded(val items: List<Car>) : GarageMutation
    data class QueryChanged(val query: String) : GarageMutation
}

object GarageReducer {
    fun reduce(state: GarageState, mutation: GarageMutation): GarageState = when (mutation) {
        is GarageMutation.CarsLoaded -> {
            val content = when {
                mutation.items.isNotEmpty() -> Content.Cars(mutation.items)
                state.query.isBlank() -> Content.EmptyGarage
                else -> Content.EmptySearch(state.query.trim())
            }
            state.copy(content = content)
        }

        GarageMutation.LoadingStarted -> {
            state.copy(content = Content.Loading)
        }

        is GarageMutation.QueryChanged -> {
            state.copy(query = mutation.query)
        }
    }
}
