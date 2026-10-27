package ru.haykor.carmanager.feature.serviceEditor.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch
import ru.haykor.carmanager.core.domain.repository.CarRepository
import ru.haykor.carmanager.core.domain.repository.ServiceRecordRepository
import ru.haykor.carmanager.core.domain.usecase.GetInitialServiceEditorDataUseCase
import ru.haykor.carmanager.core.presentation.component.createComponentScope
import kotlin.time.Clock

class DefaultServiceEditorComponent(
    componentContext: ComponentContext,
    override val editorMode: EditorMode,
    private val carRepository: CarRepository,
    private val recordRepository: ServiceRecordRepository,
    private val getInitialServiceEditorData: GetInitialServiceEditorDataUseCase,
    private val clock: Clock,
    private val onBack: () -> Unit
) : ServiceEditorComponent,
    ComponentContext by componentContext {

    private val scope = createComponentScope()

    override val state: Value<ServiceEditorState>
        field = MutableValue<ServiceEditorState>(ServiceEditorState.Loading)

    override fun accept(intent: ServiceEditorIntent) = when (intent) {
        ServiceEditorIntent.BackClicked -> onBack()
        is ServiceEditorIntent.CategoryPicked -> reduce(ServiceEditorMutation.CategoryPicked(intent.categoryId))
        is ServiceEditorIntent.DatePicked -> reduce(ServiceEditorMutation.DatePicked(intent.date))
        is ServiceEditorIntent.FieldBlurred -> reduce(ServiceEditorMutation.FieldBlurred(intent.field))
        is ServiceEditorIntent.FieldChanged -> reduce(ServiceEditorMutation.FieldChanged(intent.field, intent.value))
        ServiceEditorIntent.SaveClicked -> Unit
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

    private fun reduce(mutation: ServiceEditorMutation) {
        state.update { ServiceEditorReducer.reduce(it, mutation) }
    }
}

@Inject
@ContributesBinding(AppScope::class)
class DefaultServiceEditorComponentFactory(
    private val carRepository: CarRepository,
    private val recordRepository: ServiceRecordRepository,
    private val getInitialServiceEditorData: GetInitialServiceEditorDataUseCase,
    private val clock: Clock
) : ServiceEditorComponent.Factory {

    override fun create(ctx: ComponentContext, mode: EditorMode, onBack: () -> Unit): ServiceEditorComponent =
        DefaultServiceEditorComponent(
            componentContext = ctx,
            editorMode = mode,
            carRepository = carRepository,
            recordRepository = recordRepository,
            getInitialServiceEditorData = getInitialServiceEditorData,
            clock = clock,
            onBack = onBack
        )
}
