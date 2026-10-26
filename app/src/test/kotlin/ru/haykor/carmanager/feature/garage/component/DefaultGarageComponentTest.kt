package ru.haykor.carmanager.feature.garage.component

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import ru.haykor.carmanager.core.domain.fixtures.FakeCarRepository
import ru.haykor.carmanager.core.domain.fixtures.aCar
import ru.haykor.carmanager.core.domain.usecase.SearchCarsUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGarageComponentTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val car = aCar(brand = "Nissan", model = "Qashqai")
    private val repository = FakeCarRepository(listOf(car))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial query loads all cars`() = runTest(dispatcher) {
        val component = createComponent()

        advanceTimeBy(301)
        runCurrent()

        assertEquals(1, repository.observeCarsCalls)
        assertEquals(GarageState.Content.Cars(listOf(car)), component.state.value.content)
    }

    @Test
    fun `query change searches and clear returns to all cars`() = runTest(dispatcher) {
        val component = createComponent()
        advanceTimeBy(301)
        runCurrent()

        component.accept(GarageIntent.QueryChanged("Nis"))
        advanceTimeBy(301)
        runCurrent()

        assertEquals(listOf("Nis*"), repository.searchedQueries)
        assertEquals("Nis", component.state.value.query)
        assertEquals(GarageState.Content.Cars(listOf(car)), component.state.value.content)

        component.accept(GarageIntent.ClearQueryClicked)
        advanceTimeBy(301)
        runCurrent()

        assertEquals(2, repository.observeCarsCalls)
        assertEquals("", component.state.value.query)
        assertEquals(GarageState.Content.Cars(listOf(car)), component.state.value.content)
    }

    @Test
    fun `same query is not searched twice`() = runTest(dispatcher) {
        val component = createComponent()
        advanceTimeBy(301)
        runCurrent()

        component.accept(GarageIntent.QueryChanged("Nis"))
        advanceTimeBy(301)
        runCurrent()
        component.accept(GarageIntent.QueryChanged("Nis"))
        advanceTimeBy(301)
        runCurrent()

        assertEquals(listOf("Nis*"), repository.searchedQueries)
    }

    @Test
    fun `add and car intents call their callbacks`() {
        var addCount = 0
        val openedIds = mutableListOf<CarId>()
        val component = createComponent(
            onOpenCar = openedIds::add,
            onAddCar = { addCount++ }
        )

        component.accept(GarageIntent.AddCarClicked)
        component.accept(GarageIntent.CarClicked(car.id))

        assertEquals(1, addCount)
        assertEquals(listOf(car.id), openedIds)
    }

    private fun createComponent(
        onOpenCar: (CarId) -> Unit = {},
        onAddCar: () -> Unit = {}
    ) = DefaultGarageComponent(
        componentContext = DefaultComponentContext(LifecycleRegistry()),
        carRepository = repository,
        searchCars = SearchCarsUseCase(repository),
        onOpenCar = onOpenCar,
        onAddCar = onAddCar
    )
}
