package ru.haykor.carmanager.feature.garage.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import kotlinx.coroutines.launch
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.repository.CarRepository
import ru.haykor.carmanager.core.presentation.component.createComponentScope

class DefaultGarageComponent(
    componentContext: ComponentContext,
    private val carRepository: CarRepository,
    private val onOpenCar: (CarId) -> Unit,
    private val onAddCar: () -> Unit
) : GarageComponent,
    ComponentContext by componentContext {

    private val scope = createComponentScope()

    private val _state = MutableValue(GarageState())
    override val state: Value<GarageState> = _state

    init {
        load()
    }

    override fun accept(intent: GarageIntent) = when (intent) {
        is GarageIntent.CarClicked -> {
            onOpenCar(intent.id)
        }

        GarageIntent.AddCarClicked -> {
            onAddCar()
        }
    }

    private fun load() = scope.launch {
        reduce(GarageMutation.LoadingStarted)
        carRepository.observeCars().collect { cars ->
            reduce(GarageMutation.CarsLoaded(cars))
        }
    }

    private fun reduce(mutation: GarageMutation) {
        _state.update { state -> GarageReducer.reduce(state, mutation) }
    }
}
