package ru.haykor.carmanager.feature.garage.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.repository.CarRepository
import ru.haykor.carmanager.core.domain.usecase.SearchCarsUseCase
import ru.haykor.carmanager.core.presentation.component.createComponentScope

class DefaultGarageComponent(
    componentContext: ComponentContext,
    private val carRepository: CarRepository,
    private val searchCars: SearchCarsUseCase,
    private val onOpenCar: (CarId) -> Unit,
    private val onAddCar: () -> Unit
) : GarageComponent,
    ComponentContext by componentContext {

    private val scope = createComponentScope()

    private val _state = MutableValue(GarageState())
    override val state: Value<GarageState> = _state

    private val queryFlow = MutableStateFlow("")

    init {
        observeCarsSearch()
    }

    override fun accept(intent: GarageIntent) = when (intent) {
        is GarageIntent.CarClicked -> {
            onOpenCar(intent.id)
        }

        GarageIntent.AddCarClicked -> {
            onAddCar()
        }

        is GarageIntent.QueryChanged -> {
            reduce(GarageMutation.QueryChanged(intent.query))
            queryFlow.update { intent.query }
        }

        GarageIntent.ClearQueryClicked -> {
            reduce(GarageMutation.QueryChanged(""))
            queryFlow.update { "" }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun observeCarsSearch() {
        queryFlow
            .debounce(300.milliseconds)
            .distinctUntilChanged()
            .flatMapLatest { query ->
                searchCars(query)
            }
            .onEach { cars ->
                reduce(GarageMutation.CarsLoaded(cars))
            }
            .launchIn(scope)
    }

    private fun reduce(mutation: GarageMutation) {
        _state.update { state -> GarageReducer.reduce(state, mutation) }
    }
}

@ContributesBinding(AppScope::class)
@Inject
class DefaultGarageComponentFactory(
    private val carRepository: CarRepository,
    private val searchCars: SearchCarsUseCase
) : GarageComponent.Factory {
    override fun create(ctx: ComponentContext, onOpenCar: (CarId) -> Unit, onAddCar: () -> Unit): GarageComponent =
        DefaultGarageComponent(ctx, carRepository, searchCars, onOpenCar, onAddCar)
}
