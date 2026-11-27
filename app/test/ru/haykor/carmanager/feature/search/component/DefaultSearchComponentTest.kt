package ru.haykor.carmanager.feature.search.component

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceRecordId
import ru.haykor.carmanager.core.domain.error.DataError
import ru.haykor.carmanager.core.domain.error.SearchError
import ru.haykor.carmanager.core.domain.fixtures.FakeSearchRepository
import ru.haykor.carmanager.core.domain.usecase.SearchCarsRecordsUseCase
import ru.haykor.carmanager.feature.search.component.SearchState.Content
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultSearchComponentTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = FakeSearchRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial blank query loads history`() = runTest(dispatcher) {
        val component = createComponent()

        advanceTimeBy(301)
        runCurrent()

        assertEquals(listOf(emptyList<String>()), repository.searches)
        assertIs<Content.Results>(component.state.value.content)
    }

    @Test
    fun `query change searches after debounce`() = runTest(dispatcher) {
        val component = createComponent()
        advanceTimeBy(301)
        runCurrent()

        component.accept(SearchIntent.QueryChanged("Cam, oil!"))
        advanceTimeBy(299)
        runCurrent()

        assertEquals(listOf(emptyList<String>()), repository.searches)

        advanceTimeBy(2)
        runCurrent()

        assertEquals(listOf("Cam", "oil"), repository.searches.last())
        assertEquals("Cam, oil!", component.state.value.query)
    }

    @Test
    fun `clear query loads history`() = runTest(dispatcher) {
        val component = createComponent()
        advanceTimeBy(301)
        runCurrent()

        component.accept(SearchIntent.QueryChanged("Cam"))
        advanceTimeBy(301)
        runCurrent()

        component.accept(SearchIntent.ClearQueryClicked)
        advanceTimeBy(301)
        runCurrent()

        assertEquals(emptyList<String>(), repository.searches.last())
        assertEquals("", component.state.value.query)
    }

    @Test
    fun `same query searched once`() = runTest(dispatcher) {
        val component = createComponent()
        advanceTimeBy(301)
        runCurrent()

        component.accept(SearchIntent.QueryChanged("Cam"))
        advanceTimeBy(301)
        runCurrent()
        component.accept(SearchIntent.QueryChanged("Cam"))
        advanceTimeBy(301)
        runCurrent()

        assertEquals(listOf(emptyList<String>(), listOf("Cam")), repository.searches)
    }

    @Test
    fun `car and record clicks call callbacks`() {
        val openedCars = mutableListOf<CarId>()
        val openedRecords = mutableListOf<ServiceRecordId>()
        val component = createComponent(
            onOpenCar = openedCars::add,
            onOpenRecord = openedRecords::add
        )
        val carId = CarId("car")
        val recordId = ServiceRecordId("record")

        component.accept(SearchIntent.CarClicked(carId))
        component.accept(SearchIntent.RecordClicked(recordId))

        assertEquals(listOf(carId), openedCars)
        assertEquals(listOf(recordId), openedRecords)
    }

    @Test
    fun `failure emits event and settles`() = runTest(dispatcher) {
        repository.error = DataError.Local.Unknown
        val component = createComponent()
        var event: SearchEvent? = null
        backgroundScope.launch { event = component.events.first() }

        advanceTimeBy(301)
        runCurrent()

        assertEquals(SearchEvent.Failed(SearchError.Data(DataError.Local.Unknown)), event)
        assertEquals(Content.Results(emptyList()), component.state.value.content)
    }

    private fun createComponent(onOpenCar: (CarId) -> Unit = {}, onOpenRecord: (ServiceRecordId) -> Unit = {}) =
        DefaultSearchComponent(
            componentContext = DefaultComponentContext(LifecycleRegistry()),
            searchCarsRecords = SearchCarsRecordsUseCase(repository),
            onOpenCar = onOpenCar,
            onOpenRecord = onOpenRecord
        )
}
