package ru.haykor.carmanager.feature.car.component

import androidx.sqlite.SQLiteException
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import java.io.IOException
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
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
import ru.haykor.carmanager.core.domain.fixtures.aCar
import ru.haykor.carmanager.core.domain.fixtures.allRequiredErrors
import ru.haykor.carmanager.core.domain.fixtures.parser
import ru.haykor.carmanager.core.domain.mapper.toFormInput
import ru.haykor.carmanager.core.domain.model.Field

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultCarEditorComponentTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val carRepository = FakeCarRepository(listOf(defaultCar))
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
    fun `create mode starts with isNew=true`() = runTest(dispatcher) {
        val component = createComponent(id = null)
        val state = component.state.value
        assertIs<CarEditorState.Form>(state)
        assertTrue(state.isNew)
    }

    @Test
    fun `edit mode loads car & starts with isNew=false`() = runTest(dispatcher) {
        val component = createComponent(id = defaultCar.id)
        val state = component.state.value
        assertIs<CarEditorState.Form>(state)
        assertFalse(state.isNew)
        assertEquals(defaultCar.toFormInput(), state.input)
    }

    @Test
    fun `edit mode sets NotFound when car not found`() = runTest(dispatcher) {
        val component = createComponent(id = CarId.random())
        val state = component.state.value
        assertIs<CarEditorState.NotFound>(state)
    }

    @Test
    fun `save on empty form results in all required errors`() = runTest(dispatcher) {
        val component = createComponent(id = null)
        component.accept(CarEditorIntent.SaveClicked)
        val state = component.state.value
        assertIs<CarEditorState.Form>(state)
        assertEquals(allRequiredErrors, state.errors)
        assertEquals(0, closeCount)
        assertTrue(carRepository.created.isEmpty())
    }

    @Test
    fun `save on new form calls create & onBack`() = runTest(dispatcher) {
        val component = createComponent(id = null)
        fillForm(component)
        component.accept(CarEditorIntent.SaveClicked)

        assertEquals(1, carRepository.created.size)
        assertEquals(1, closeCount)
        assertEquals(0, carRepository.updated.size)
    }

    @Test
    fun `save on an old form calls update & onBack`() = runTest(dispatcher) {
        val component = createComponent(id = defaultCar.id)
        fillForm(component)
        component.accept(CarEditorIntent.SaveClicked)

        assertEquals(1, carRepository.updated.size)
        assertEquals(1, closeCount)
        assertEquals(0, carRepository.created.size)
    }

    @Test
    fun `no errors without blur`() = runTest(dispatcher) {
        val component = createComponent(id = null)
        component.accept(CarEditorIntent.FieldChanged(Field.BRAND, ""))
        val state = component.state.value
        assertIs<CarEditorState.Form>(state)
        assertEquals(0, state.errors.size)
    }

    @Test
    fun `only related errors after blur`() = runTest(dispatcher) {
        val component = createComponent(id = null)
        component.accept(CarEditorIntent.FieldBlurred(Field.BRAND))
        val state = component.state.value
        assertIs<CarEditorState.Form>(state)
        assertContains(state.errors, Field.BRAND)
        assertFalse(Field.MODEL in state.errors)
    }

    @Test
    fun `changing value in error field fixes error`() = runTest(dispatcher) {
        val component = createComponent(id = null)
        component.accept(CarEditorIntent.FieldBlurred(Field.BRAND))
        component.accept(CarEditorIntent.FieldChanged(Field.BRAND, "Aaaa"))
        val state = component.state.value
        assertIs<CarEditorState.Form>(state)
        assertFalse(Field.BRAND in state.errors)
    }

    @Test
    fun `double tap on Save creates car once`() = runTest(dispatcher) {
        // Arrange
        val gate = CompletableDeferred<Unit>()
        carRepository.saveGate = gate
        val component = createComponent(id = null)
        fillForm(component)

        // Act
        component.run {
            accept(CarEditorIntent.SaveClicked)
            accept(CarEditorIntent.SaveClicked)
        }

        // Assert
        val state = component.state.value
        assertEquals(1, carRepository.created.size)
        assertIs<CarEditorState.Form>(state)
        assertTrue(state.isSaving)
        assertEquals(0, closeCount)

        // Act
        gate.complete(Unit)

        // Assert
        assertEquals(1, carRepository.created.size)
        assertEquals(1, closeCount)
    }

    @Test
    fun `component handles error thrown in repo`() = runTest(dispatcher) {
        // Arrange
        val dataError = DataError.Local.Unknown
        carRepository.saveError = dataError
        val component = createComponent(id = null)
        fillForm(component)

        val events = mutableListOf<CarEditorEvent>()
        backgroundScope.launch { component.events.toList(events) }

        // Act
        component.accept(CarEditorIntent.SaveClicked)

        // Assert
        val state = component.state.value
        assertIs<CarEditorState.Form>(state)
        assertFalse(state.isSaving)
        assertEquals(0, closeCount)
        assertEquals<List<CarEditorEvent>>(listOf(CarEditorEvent.SaveFailed(DataError.Local.Unknown)), events)
    }

    private fun fillForm(component: CarEditorComponent) {
        component.run {
            accept(CarEditorIntent.FieldChanged(Field.BRAND, "Nissan"))
            accept(CarEditorIntent.FieldChanged(Field.MODEL, "Qashqai"))
            accept(CarEditorIntent.FieldChanged(Field.YEAR, "2010"))
            accept(CarEditorIntent.FieldChanged(Field.MILEAGE, "10000"))
        }
    }

    private fun createComponent(id: CarId? = null, lifecycleRegistry: LifecycleRegistry = LifecycleRegistry()) =
        DefaultCarEditorComponent(
            componentContext = DefaultComponentContext(lifecycleRegistry),
            id = id,
            carRepository = carRepository,
            parser = parser,
            onBack = { closeCount++ }
        )

    private companion object {
        val defaultCar = aCar()
    }
}
