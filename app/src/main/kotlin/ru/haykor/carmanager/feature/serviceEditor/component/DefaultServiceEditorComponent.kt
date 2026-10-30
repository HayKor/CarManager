package ru.haykor.carmanager.feature.serviceEditor.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch
import ru.haykor.carmanager.core.domain.usecase.GetInitialServiceEditorDataUseCase
import ru.haykor.carmanager.core.domain.usecase.MileageResolution
import ru.haykor.carmanager.core.domain.usecase.SaveRecordError
import ru.haykor.carmanager.core.domain.usecase.SaveServiceRecordUseCase
import ru.haykor.carmanager.core.presentation.component.createComponentScope
import ru.haykor.carmanager.core.result.DataResult
import ru.haykor.carmanager.feature.serviceEditor.component.ServiceEditorMutation.AllFieldsTouched
import ru.haykor.carmanager.feature.serviceEditor.component.ServiceEditorMutation.CategoryPicked
import ru.haykor.carmanager.feature.serviceEditor.component.ServiceEditorMutation.DatePicked
import ru.haykor.carmanager.feature.serviceEditor.component.ServiceEditorMutation.ErrorsUpdated
import ru.haykor.carmanager.feature.serviceEditor.component.ServiceEditorMutation.FieldBlurred
import ru.haykor.carmanager.feature.serviceEditor.component.ServiceEditorMutation.FieldChanged
import ru.haykor.carmanager.feature.serviceEditor.component.ServiceEditorMutation.SetIsSaving
import ru.haykor.carmanager.feature.serviceEditor.component.ServiceEditorMutation.SetShowConfirmation

class DefaultServiceEditorComponent(
    componentContext: ComponentContext,
    override val editorMode: EditorMode,
    private val getInitialServiceEditorData: GetInitialServiceEditorDataUseCase,
    private val saveServiceRecord: SaveServiceRecordUseCase,
    private val onBack: () -> Unit
) : ServiceEditorComponent,
    ComponentContext by componentContext {

    private val scope = createComponentScope()

    override val state: Value<ServiceEditorState>
        field = MutableValue<ServiceEditorState>(ServiceEditorState.Loading)

    override fun accept(intent: ServiceEditorIntent) = when (intent) {
        ServiceEditorIntent.BackClicked -> onBack()

        is ServiceEditorIntent.CategoryPicked -> reduce(CategoryPicked(intent.categoryId))

        is ServiceEditorIntent.DatePicked -> reduce(DatePicked(intent.date))

        is ServiceEditorIntent.FieldBlurred -> reduce(FieldBlurred(intent.field))

        is ServiceEditorIntent.FieldChanged -> reduce(FieldChanged(intent.field, intent.value))

        ServiceEditorIntent.SaveClicked -> {
            reduce(AllFieldsTouched)
            trySaveRecord()
        }

        ServiceEditorIntent.ConfirmSaveClicked -> {
            reduce(AllFieldsTouched)
            reduce(SetShowConfirmation(false))
            trySaveRecord(resolution = MileageResolution.UPDATE_CAR)
        }

        ServiceEditorIntent.DismissSaveClicked -> {
            reduce(SetShowConfirmation(false))
        }
    }

    init {
        loadInitialForm()
    }

    private fun loadInitialForm() {
        scope.launch {
            when (val result = getInitialServiceEditorData(editorMode)) {
                null -> reduce(ServiceEditorMutation.SetNotFound)

                else -> {
                    reduce(
                        ServiceEditorMutation.InitialFormLoaded(
                            input = result.input,
                            car = result.car,
                            categories = result.categories
                        )
                    )
                }
            }
        }
    }

    private fun trySaveRecord(resolution: MileageResolution = MileageResolution.ASK) {
        val form = (state.value as? ServiceEditorState.Form) ?: return
        if (form.isSaving) return

        reduce(SetIsSaving(true))
        scope.launch {
            when (val result = saveServiceRecord(editorMode, form.input, form.input.carId, resolution)) {
                is DataResult.Failure -> {
                    reduce(SetIsSaving(false))
                    when (val error = result.error) {
                        is SaveRecordError.Data -> {
                            // TODO:
                        }

                        is SaveRecordError.MileageMismatch -> {
                            reduce(SetShowConfirmation(true))
                        }

                        is SaveRecordError.ParseFailure -> {
                            reduce(ErrorsUpdated(error.errors))
                        }

                        is SaveRecordError.MileageOutOfSequence -> {
                            // TODO:
                        }
                    }
                }

                is DataResult.Success -> {
                    onBack()
                }
            }
        }
    }

    private fun reduce(mutation: ServiceEditorMutation) {
        state.update { ServiceEditorReducer.reduce(it, mutation) }
    }
}

@Inject
@ContributesBinding(AppScope::class)
class DefaultServiceEditorComponentFactory(
    private val getInitialServiceEditorData: GetInitialServiceEditorDataUseCase,
    private val saveServiceRecord: SaveServiceRecordUseCase
) : ServiceEditorComponent.Factory {

    override fun create(ctx: ComponentContext, mode: EditorMode, onBack: () -> Unit): ServiceEditorComponent =
        DefaultServiceEditorComponent(
            componentContext = ctx,
            editorMode = mode,
            getInitialServiceEditorData = getInitialServiceEditorData,
            saveServiceRecord = saveServiceRecord,
            onBack = onBack
        )
}
