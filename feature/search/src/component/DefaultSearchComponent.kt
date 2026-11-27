package ru.haykor.carmanager.feature.search.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceRecordId
import ru.haykor.carmanager.core.domain.usecase.SearchCarsRecordsUseCase
import ru.haykor.carmanager.core.presentation.component.createComponentScope
import ru.haykor.carmanager.core.result.DataResult
import kotlin.time.Duration.Companion.milliseconds

class DefaultSearchComponent(
    componentContext: ComponentContext,
    private val searchCarsRecords: SearchCarsRecordsUseCase,
    private val onOpenCar: (CarId) -> Unit,
    private val onOpenRecord: (ServiceRecordId) -> Unit
) : SearchComponent,
    ComponentContext by componentContext {

    private val scope = createComponentScope()

    private val _state = MutableValue(SearchState())
    override val state: Value<SearchState> = _state

    private val _events = Channel<SearchEvent>(Channel.BUFFERED)
    override val events: Flow<SearchEvent> = _events.receiveAsFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        observeQuery()
    }

    override fun accept(intent: SearchIntent) = when (intent) {
        is SearchIntent.QueryChanged -> {
            reduce(SearchMutation.QueryChanged(intent.query))
            queryFlow.update { intent.query }
        }

        SearchIntent.ClearQueryClicked -> {
            reduce(SearchMutation.QueryChanged(""))
            queryFlow.update { "" }
        }

        is SearchIntent.CarClicked -> onOpenCar(intent.id)

        is SearchIntent.RecordClicked -> onOpenRecord(intent.id)
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun observeQuery() {
        queryFlow
            .debounce(300.milliseconds)
            .distinctUntilChanged()
            .flatMapLatest { query -> searchCarsRecords(query) }
            .onEach { result ->
                when (result) {
                    is DataResult.Success -> reduce(SearchMutation.ResultsLoaded(result.data))

                    is DataResult.Failure -> {
                        _events.send(SearchEvent.Failed(result.error))
                        reduce(SearchMutation.LoadFailed)
                    }
                }
            }
            .launchIn(scope)
    }

    private fun reduce(mutation: SearchMutation) {
        _state.update { state -> SearchReducer.reduce(state, mutation) }
    }
}

@ContributesBinding(AppScope::class)
@Inject
class DefaultSearchComponentFactory(private val searchCarsRecords: SearchCarsRecordsUseCase) :
    SearchComponent.Factory {

    override fun create(
        ctx: ComponentContext,
        onOpenCar: (CarId) -> Unit,
        onOpenRecord: (ServiceRecordId) -> Unit
    ): SearchComponent = DefaultSearchComponent(
        componentContext = ctx,
        searchCarsRecords = searchCarsRecords,
        onOpenCar = onOpenCar,
        onOpenRecord = onOpenRecord
    )
}
