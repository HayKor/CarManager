package ru.haykor.carmanager.feature.carEditor.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.doOnDestroy
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.haykor.carmanager.core.domain.CarDraftParser
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.CarParseResult
import ru.haykor.carmanager.core.domain.mapper.toFormInput
import ru.haykor.carmanager.core.domain.model.CarDraft
import ru.haykor.carmanager.core.domain.repository.CarRepository
import ru.haykor.carmanager.core.presentation.component.createComponentScope
import ru.haykor.carmanager.core.result.DataResult

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

    private val _events = Channel<CarEditorEvent>(Channel.BUFFERED)
    override val events = _events.receiveAsFlow()

    private var isExitHandled = false

    private val backCallback = BackCallback(isEnabled = false) {
        // if we're inside of this lambda, condition is already met
        accept(CarEditorIntent.BackClicked)
    }

    init {
        loadInitialForm()
        backHandler.register(backCallback)
        lifecycle.doOnDestroy { backHandler.unregister(backCallback) }
    }

    override fun accept(intent: CarEditorIntent) = when (intent) {
        is CarEditorIntent.FieldBlurred -> {
            reduce(CarEditorMutation.FieldBlurred(intent.field))
            refreshErrors()
        }

        is CarEditorIntent.FieldChanged -> {
            reduce(CarEditorMutation.FieldChanged(intent.field, intent.value))
            refreshErrors()
        }

        is CarEditorIntent.PlateCountryChanged -> {
            reduce(CarEditorMutation.PlateCountryChanged(intent.plateCountry))
            refreshErrors()
        }

        CarEditorIntent.SaveClicked -> {
            reduce(CarEditorMutation.AllFieldsTouched)
            trySaveCar()
        }

        CarEditorIntent.BackClicked -> onBackClicked()

        CarEditorIntent.DialogConfirmClicked -> {
            reduce(CarEditorMutation.SetExitDialogVisible(false))
            reduce(CarEditorMutation.AllFieldsTouched)
            trySaveCar()
        }

        CarEditorIntent.DialogIgnoreClicked -> {
            reduce(CarEditorMutation.SetExitDialogVisible(false))
            exit()
        }

        CarEditorIntent.DialogDismissed -> {
            reduce(CarEditorMutation.SetExitDialogVisible(false))
        }
    }

    private fun onBackClicked() {
        val form = _state.value as? CarEditorState.Form
        if (form != null && form.isDirty && !form.isSaving) {
            reduce(CarEditorMutation.SetExitDialogVisible(true))
        } else {
            exit()
        }
    }

    /**
     * Leaves the screen at most once: the user may leave while a save is in flight, and the save
     * must not pop the next screen afterwards. Also does nothing if the component is already gone.
     */
    private fun exit() {
        if (isExitHandled || lifecycle.state == Lifecycle.State.DESTROYED) return
        isExitHandled = true
        onBack()
    }

    private fun loadInitialForm() = scope.launch {
        if (id == null) { // empty, so we create
            reduce(CarEditorMutation.FormInputNew)
            return@launch
        }

        carRepository.observeCar(id).first()?.let { car ->
            reduce(CarEditorMutation.FormInputLoaded(car.toFormInput()))
        } ?: reduce(CarEditorMutation.SetNotFound)
    }

    private fun trySaveCar() {
        val form = (_state.value as? CarEditorState.Form) ?: return
        if (form.isSaving) return

        when (val result = parser.parse(form.currentInput)) {
            is CarParseResult.Invalid -> reduce(CarEditorMutation.ErrorsUpdated(result.errors))

            is CarParseResult.Valid -> {
                reduce(CarEditorMutation.SetSaving(true))
                scope.launch {
                    saveOrUpdateCar(result.draft)
                }
            }
        }
    }

    private suspend fun saveOrUpdateCar(draft: CarDraft) {
        if (id != null) {
            when (val result = carRepository.update(id, draft)) {
                is DataResult.Failure -> {
                    reduce(CarEditorMutation.SetSaving(false))
                    _events.send(CarEditorEvent.SaveFailed(result.error))
                }

                is DataResult.Success -> {
                    exit()
                }
            }
        } else {
            when (val result = carRepository.create(draft)) {
                is DataResult.Failure -> {
                    reduce(CarEditorMutation.SetSaving(false))
                    _events.send(CarEditorEvent.SaveFailed(result.error))
                }

                is DataResult.Success -> {
                    exit()
                }
            }
        }
    }

    private fun reduce(mutation: CarEditorMutation) {
        _state.update { state -> CarEditorReducer.reduce(state, mutation) }
        updateBackCallback()
    }

    private fun refreshErrors() {
        val form = (_state.value as? CarEditorState.Form) ?: return
        val all = parser.validate(form.currentInput)
        reduce(CarEditorMutation.ErrorsUpdated(all.filterKeys { it in form.touched }))
    }

    private fun updateBackCallback() {
        val form = _state.value as? CarEditorState.Form
        // While saving, back leaves the screen and cancels the save, so no dialog is needed
        backCallback.isEnabled = form != null && form.isDirty && !form.isSaving
    }
}

@ContributesBinding(AppScope::class)
@Inject
class DefaultCarEditorComponentFactory(private val carRepository: CarRepository, private val parser: CarDraftParser) :
    CarEditorComponent.Factory {

    override fun create(ctx: ComponentContext, id: CarId?, onBack: () -> Unit) = DefaultCarEditorComponent(
        componentContext = ctx,
        id = id,
        carRepository = carRepository,
        parser = parser,
        onBack = onBack
    )
}
