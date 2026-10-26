package ru.haykor.carmanager.feature.car.component

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import junit.framework.TestCase.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.error.DataError
import ru.haykor.carmanager.core.domain.fixtures.FakeCarRepository
import ru.haykor.carmanager.core.domain.fixtures.FakeServiceRecordRepository
import ru.haykor.carmanager.core.domain.fixtures.aCar
import ru.haykor.carmanager.core.domain.usecase.DeleteCarUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultCarComponentTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val carRepository = FakeCarRepository(listOf(defaultCar))
    private val recordRepository = FakeServiceRecordRepository()
    private val deleteCar = DeleteCarUseCase(carRepository)
    private var closeCount: Int = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `confirm deletes car and calls onBack`() = runTest(dispatcher) {
        val component = createComponent()
        component.run {
            accept(CarIntent.DeleteClicked)
            accept(CarIntent.DeleteConfirmed)
        }
        assertEquals(1, carRepository.deleted.size)
        assertEquals(1, closeCount)
        assertEquals(listOf(defaultCar.id), carRepository.deleted)
    }

    @Test
    fun `dismiss doesn't call useCase and onBack`() = runTest(dispatcher) {
        val component = createComponent()
        component.run {
            accept(CarIntent.DeleteClicked)
            accept(CarIntent.DeleteDismissed)
        }
        assertEquals(0, carRepository.deleted.size)
        assertEquals(0, closeCount)
    }

    @Test
    fun `confirm button dismisses dialog`() = runTest(dispatcher) {
        val component = createComponent()
        component.run {
            accept(CarIntent.DeleteClicked)
            accept(CarIntent.DeleteConfirmed)
        }
        val state = component.state.value
        assertFalse(state.isShowDialog)
    }

    @Test
    fun `double tap on Confirm deletes car once`() = runTest(dispatcher) {
        // Arrange
        val gate = CompletableDeferred<Unit>()
        carRepository.deleteGate = gate
        val component = createComponent()

        // Act
        component.run {
            accept(CarIntent.DeleteClicked)
            repeat(2) {
                accept(CarIntent.DeleteConfirmed)
            }
        }

        // Assert
        val state = component.state.value
        assertEquals(1, carRepository.deleted.size)
        assertTrue(state.isDeleting)
        assertEquals(0, closeCount)

        // Act
        gate.complete(Unit)

        // Assert
        assertEquals(1, carRepository.deleted.size)
        assertEquals(1, closeCount)
    }

    @Test
    fun `shows error thrown in repo`() = runTest(dispatcher) {
        val dataError = DataError.Local.Unknown
        carRepository.deleteError = dataError
        val component = createComponent()

        val events = mutableListOf<CarEvent>()
        backgroundScope.launch { component.events.toList(events) }

        component.run {
            accept(CarIntent.DeleteClicked)
            accept(CarIntent.DeleteConfirmed)
        }

        val state = component.state.value
        assertFalse(state.isShowDialog)
        assertFalse(state.isDeleting)
        assertEquals(0, closeCount)
        assertEquals(listOf<CarEvent>(CarEvent.DeleteFailed(dataError)), events)
    }

    private fun createComponent(id: CarId = defaultCar.id, lifecycleRegistry: LifecycleRegistry = LifecycleRegistry()) =
        DefaultCarComponent(
            componentContext = DefaultComponentContext(lifecycleRegistry),
            carRepository = carRepository,
            recordRepository = recordRepository,
            deleteCar = deleteCar,
            id = id,
            onBack = { closeCount++ },
            onOpenEdit = {}
        )

    private companion object {
        val defaultCar = aCar()
    }
}
