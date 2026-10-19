package ru.haykor.carmanager.feature.car.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.haykor.carmanager.core.domain.CarDraftParser
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.CarParseResult
import ru.haykor.carmanager.core.domain.mapper.toFormInput
import ru.haykor.carmanager.core.domain.model.CarDraft
import ru.haykor.carmanager.core.domain.repository.CarRepository
import ru.haykor.carmanager.core.presentation.component.createComponentScope
import ru.haykor.carmanager.feature.car.component.CarEditorMutation.FieldBlurred
import ru.haykor.carmanager.feature.car.component.CarEditorMutation.FieldChanged
import ru.haykor.carmanager.feature.car.component.CarEditorMutation.FormInputLoaded
import ru.haykor.carmanager.feature.car.component.CarEditorMutation.PlateCountryChanged
import ru.haykor.carmanager.feature.car.component.CarEditorMutation.SetNotFound

class DefaultCarEditorComponent(
    componentContext: ComponentContext,
    private val id: CarId?,
    private val carRepository: CarRepository,
    private val parser: CarDraftParser,
    private val onBack: () -> Unit
) : CarEditorComponent,
    ComponentContext by componentContext {

    private val scope = createComponentScope()

    private val _state = MutableValue<CarEditorState>(CarEditorState.Loading)
    override val state: Value<CarEditorState> = _state

    init {
        loadInitialForm()
    }

    override fun accept(intent: CarEditorIntent) = when (intent) {
        is CarEditorIntent.FieldBlurred -> {
            reduce(FieldBlurred(intent.field))
            refreshErrors()
        }

        is CarEditorIntent.FieldChanged -> {
            reduce(FieldChanged(intent.field, intent.value))
            refreshErrors()
        }

        is CarEditorIntent.PlateCountryChanged -> {
            reduce(PlateCountryChanged(intent.plateCountry))
            refreshErrors()
        }

        CarEditorIntent.SaveClicked -> {
            reduce(CarEditorMutation.AllFieldsTouched)
            trySaveCar()
        }

        CarEditorIntent.BackClicked -> {
            onBack()
        }
    }

    private fun loadInitialForm() = scope.launch {
        if (id == null) { // empty, so we create
            reduce(CarEditorMutation.FormInputNew)
            return@launch
        }

        carRepository.observeCar(id).first()?.let { car ->
            reduce(FormInputLoaded(car.toFormInput()))
        } ?: reduce(SetNotFound)
    }

    private fun trySaveCar() {
        val form = (_state.value as? CarEditorState.Form) ?: return
        if (form.isSaving) return

        when (val result = parser.parse(form.input)) {
            is CarParseResult.Invalid -> reduce(CarEditorMutation.ErrorsUpdated(result.errors))

            is CarParseResult.Valid -> {
                reduce(CarEditorMutation.SetSaving(true))
                scope.launch {
                    try {
                        saveOrUpdateCar(result.draft)
                        onBack()
                    } finally {
                        reduce(CarEditorMutation.SetSaving(false))
                    }
                }
            }
        }
    }

    private suspend fun saveOrUpdateCar(draft: CarDraft) {
        if (id != null) {
            carRepository.update(id, draft)
        } else {
            carRepository.create(draft)
        }
    }

    private fun reduce(mutation: CarEditorMutation) {
        _state.update { state -> CarEditorReducer.reduce(state, mutation) }
    }

    private fun refreshErrors() {
        val form = (_state.value as? CarEditorState.Form) ?: return
        val all = parser.validate(form.input)
        reduce(CarEditorMutation.ErrorsUpdated(all.filterKeys { it in form.touched }))
    }
}
