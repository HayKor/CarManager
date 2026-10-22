package ru.haykor.carmanager.feature.car.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.repository.CarRepository
import ru.haykor.carmanager.core.presentation.component.createComponentScope

class DefaultCarComponent(
    componentContext: ComponentContext,
    private val repository: CarRepository,
    override val id: CarId,
    val onBack: () -> Unit,
    val onOpenEdit: (CarId) -> Unit
) : CarComponent,
    ComponentContext by componentContext {

    private val scope = createComponentScope()

    override val state: Value<CarState>
        field = MutableValue<CarState>(CarState.Loading)

    override fun accept(intent: CarIntent) = when (intent) {
        CarIntent.BackClicked -> onBack()
        CarIntent.EditCarClicked -> onOpenEdit(id)
    }

    init {
        loadCar()
    }

    private fun loadCar() {
        scope.launch {
            repository.observeCar(id)
                .onStart { reduce(CarMutation.LoadingStarted) }
                .collectLatest { car ->
                    if (car == null) {
                        reduce(CarMutation.NotFound)
                    } else {
                        reduce(CarMutation.CarLoaded(car))
                    }
                }
        }
    }

    private fun reduce(mutation: CarMutation) {
        state.update { CarReducer.reduce(it, mutation) }
    }
}

@ContributesBinding(AppScope::class)
@Inject
class DefaultCarComponentFactory(private val repository: CarRepository) : CarComponent.Factory {
    override fun create(
        ctx: ComponentContext,
        carId: CarId,
        onBack: () -> Unit,
        onOpenEdit: (CarId) -> Unit
    ): CarComponent = DefaultCarComponent(ctx, repository, carId, onBack, onOpenEdit)
}
