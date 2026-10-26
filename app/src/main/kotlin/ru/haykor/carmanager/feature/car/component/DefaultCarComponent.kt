package ru.haykor.carmanager.feature.car.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceRecordId
import ru.haykor.carmanager.core.domain.repository.CarRepository
import ru.haykor.carmanager.core.domain.repository.ServiceRecordRepository
import ru.haykor.carmanager.core.domain.usecase.DeleteCarUseCase
import ru.haykor.carmanager.core.presentation.component.createComponentScope
import ru.haykor.carmanager.core.result.DataResult
import ru.haykor.carmanager.feature.car.component.CarMutation.*

class DefaultCarComponent(
    componentContext: ComponentContext,
    override val id: CarId,
    private val carRepository: CarRepository,
    private val recordRepository: ServiceRecordRepository,
    private val deleteCar: DeleteCarUseCase,
    private val onBack: () -> Unit,
    private val onOpenEdit: () -> Unit,
    private val onOpenRecord: (ServiceRecordId) -> Unit,
    private val onOpenCreateRecord: () -> Unit
) : CarComponent,
    ComponentContext by componentContext {

    private val scope = createComponentScope()

    override val state: Value<CarState>
        field = MutableValue<CarState>(CarState())

    private val _events = Channel<CarEvent>(Channel.BUFFERED)
    override val events = _events.receiveAsFlow()

    override fun accept(intent: CarIntent) = when (intent) {
        CarIntent.BackClicked -> onBack()

        CarIntent.EditCarClicked -> onOpenEdit()

        CarIntent.CreateRecordClicked -> onOpenCreateRecord()

        is CarIntent.RecordClicked -> onOpenRecord(intent.recordId)

        CarIntent.DeleteClicked -> {
            reduce(SetShowDialog(true))
        }

        CarIntent.DeleteDismissed -> {
            reduce(SetShowDialog(false))
        }

        CarIntent.DeleteConfirmed -> {
            delete()
        }
    }

    init {
        loadCarWithRecords()
    }

    private fun loadCarWithRecords() {
        scope.launch {
            merge(
                carRepository.observeCar(id).map { car ->
                    if (car == null) CarMutation.CarNotFound else CarMutation.CarLoaded(car)
                },
                recordRepository.observeByCar(id).map(CarMutation::RecordsLoaded)
            ).collect { mutation ->
                reduce(mutation)
            }
        }
    }

    private fun delete() {
        if (state.value.isDeleting) return

        reduce(CarMutation.SetIsDeleting(true))
        reduce(CarMutation.SetShowDialog(false))
        scope.launch {
            when (val result = deleteCar(id)) {
                is DataResult.Success -> onDelete()

                is DataResult.Failure -> {
                    reduce(CarMutation.SetIsDeleting(false))
                    _events.send(CarEvent.DeleteFailed(result.error))
                }
            }
        }
    }

    private fun onDelete() {
        onBack()
    }

    private fun reduce(mutation: CarMutation) {
        state.update { CarReducer.reduce(it, mutation) }
    }
}

@ContributesBinding(AppScope::class)
@Inject
class DefaultCarComponentFactory(
    private val carRepository: CarRepository,
    private val recordRepository: ServiceRecordRepository,
    private val deleteCar: DeleteCarUseCase
) : CarComponent.Factory {

    override fun create(
        ctx: ComponentContext,
        carId: CarId,
        onBack: () -> Unit,
        onOpenEdit: () -> Unit,
        onOpenRecord: (ServiceRecordId) -> Unit,
        onOpenCreateRecord: () -> Unit
    ): CarComponent = DefaultCarComponent(
        ctx,
        carId,
        carRepository,
        recordRepository,
        deleteCar,
        onBack,
        onOpenEdit,
        onOpenRecord,
        onOpenCreateRecord
    )
}
