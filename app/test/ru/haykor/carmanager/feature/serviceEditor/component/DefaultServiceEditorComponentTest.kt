package ru.haykor.carmanager.feature.serviceEditor.component

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceCategoryId
import ru.haykor.carmanager.core.domain.ServiceRecordId
import ru.haykor.carmanager.core.domain.fixtures.FakeCarRepository
import ru.haykor.carmanager.core.domain.fixtures.FakeServiceCategoryRepository
import ru.haykor.carmanager.core.domain.fixtures.FakeServiceRecordRepository
import ru.haykor.carmanager.core.domain.fixtures.aCar
import ru.haykor.carmanager.core.domain.fixtures.aCategory
import ru.haykor.carmanager.core.domain.fixtures.aRecord
import ru.haykor.carmanager.core.domain.fixtures.clock
import ru.haykor.carmanager.core.domain.fixtures.recordParser
import ru.haykor.carmanager.core.domain.model.EditorMode
import ru.haykor.carmanager.core.domain.model.RecordField
import ru.haykor.carmanager.core.domain.usecase.GetInitialServiceEditorDataUseCase
import ru.haykor.carmanager.core.domain.usecase.SaveServiceRecordUseCase
import ru.haykor.carmanager.feature.serviceEditor.component.ServiceEditorState.Form
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultServiceEditorComponentTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val carId = CarId("car")
    private val category = aCategory(id = ServiceCategoryId("category"), name = "Maintenance")
    private val car = aCar(id = carId, mileage = 100_000)
    private val existingRecord = aRecord(
        id = ServiceRecordId("record"),
        carId = carId,
        categoryId = category.id,
        atMileage = 90_000,
        date = 0L
    )
    private val carRepository = FakeCarRepository(listOf(car))
    private val recordRepository = FakeServiceRecordRepository(listOf(existingRecord), category)
    private val categoryRepository = FakeServiceCategoryRepository(listOf(category))
    private val getInitialData =
        GetInitialServiceEditorDataUseCase(carRepository, recordRepository, categoryRepository, clock)
    private val saveRecord = SaveServiceRecordUseCase(recordRepository, carRepository, recordParser)
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
    fun `create mode starts clean`() = runTest(dispatcher) {
        val component = createComponent()

        val state = component.state.value
        assertIs<Form>(state)
        assertEquals(state.initialInput, state.input)
        assertFalse(state.isDirty)
    }

    @Test
    fun `edit mode loads the record and starts clean`() = runTest(dispatcher) {
        val component = createComponent(mode = EditorMode.Edit(existingRecord.id))

        val state = component.state.value
        assertIs<Form>(state)
        assertEquals(existingRecord.atMileage.toString(), state.input.atMileage)
        assertEquals(state.initialInput, state.input)
        assertFalse(state.isDirty)
    }

    @Test
    fun `back without changes closes the screen immediately`() = runTest(dispatcher) {
        val component = createComponent()

        component.accept(ServiceEditorIntent.BackClicked)

        val state = component.state.value
        assertIs<Form>(state)
        assertFalse(state.isExitDialogVisible)
        assertEquals(1, closeCount)
    }

    @Test
    fun `system back without changes is not intercepted`() = runTest(dispatcher) {
        val backDispatcher = BackDispatcher()
        createComponent(backDispatcher = backDispatcher)

        assertFalse(backDispatcher.back())
    }

    @Test
    fun `change makes the form dirty and intercepts system back`() = runTest(dispatcher) {
        val backDispatcher = BackDispatcher()
        val component = createComponent(backDispatcher = backDispatcher)

        component.accept(ServiceEditorIntent.FieldChanged(RecordField.NOTE, "Oil change"))
        assertTrue(backDispatcher.back())

        val state = component.state.value
        assertIs<Form>(state)
        assertTrue(state.isDirty)
        assertTrue(state.isExitDialogVisible)
        assertEquals(0, closeCount)
    }

    @Test
    fun `dialog dismiss keeps the screen`() = runTest(dispatcher) {
        val component = createComponent()
        component.accept(ServiceEditorIntent.FieldChanged(RecordField.NOTE, "Oil change"))
        component.accept(ServiceEditorIntent.BackClicked)

        component.accept(ServiceEditorIntent.DialogDismissed)

        val state = component.state.value
        assertIs<Form>(state)
        assertFalse(state.isExitDialogVisible)
        assertEquals(0, closeCount)
    }

    @Test
    fun `dialog ignore closes the screen without saving`() = runTest(dispatcher) {
        val component = createComponent()
        component.accept(ServiceEditorIntent.FieldChanged(RecordField.NOTE, "Oil change"))
        component.accept(ServiceEditorIntent.BackClicked)

        component.accept(ServiceEditorIntent.DialogIgnoreClicked)

        assertEquals(1, closeCount)
        assertTrue(recordRepository.created.isEmpty())
    }

    @Test
    fun `dialog save saves the record and closes the screen`() = runTest(dispatcher) {
        val component = createComponent()
        component.accept(ServiceEditorIntent.FieldChanged(RecordField.NOTE, "Oil change"))
        component.accept(ServiceEditorIntent.BackClicked)

        component.accept(ServiceEditorIntent.DialogConfirmClicked)

        assertEquals(1, recordRepository.created.size)
        assertEquals(1, closeCount)
    }

    @Test
    fun `back during saving does not open the dialog and closes once`() = runTest(dispatcher) {
        // Arrange
        val gate = CompletableDeferred<Unit>()
        recordRepository.saveGate = gate
        val backDispatcher = BackDispatcher()
        val component = createComponent(backDispatcher = backDispatcher)
        component.accept(ServiceEditorIntent.FieldChanged(RecordField.NOTE, "Oil change"))
        component.accept(ServiceEditorIntent.SaveClicked)

        // Act
        assertTrue((component.state.value as Form).isSaving)
        assertFalse(backDispatcher.back())
        component.accept(ServiceEditorIntent.BackClicked)

        // Assert
        val state = component.state.value
        assertIs<Form>(state)
        assertFalse(state.isExitDialogVisible)
        assertEquals(1, closeCount)

        // Act: the save finishes after the user has already left
        gate.complete(Unit)

        // Assert
        assertEquals(1, closeCount)
    }

    @Test
    fun `back dismisses the mileage conflict first`() = runTest(dispatcher) {
        val component = createComponent()
        component.accept(ServiceEditorIntent.FieldChanged(RecordField.AT_MILEAGE, "90000"))
        component.accept(ServiceEditorIntent.SaveClicked)

        assertNotNull((component.state.value as Form).mileageConflict)

        // Act
        component.accept(ServiceEditorIntent.BackClicked)

        // Assert
        val state = component.state.value
        assertIs<Form>(state)
        assertNull(state.mileageConflict)
        assertFalse(state.isExitDialogVisible)
        assertEquals(0, closeCount)

        // Act: the next back is a regular unsaved changes confirmation
        component.accept(ServiceEditorIntent.BackClicked)

        assertTrue((component.state.value as Form).isExitDialogVisible)
        assertEquals(0, closeCount)
    }

    @Test
    fun `delete closes the screen once`() = runTest(dispatcher) {
        val component = createComponent(mode = EditorMode.Edit(existingRecord.id))

        component.accept(ServiceEditorIntent.DeleteClicked)

        assertEquals(1, closeCount)
    }

    @Test
    fun `leaving twice closes the screen once`() = runTest(dispatcher) {
        val component = createComponent()

        component.run {
            accept(ServiceEditorIntent.BackClicked)
            accept(ServiceEditorIntent.BackClicked)
        }

        assertEquals(1, closeCount)
    }

    private fun createComponent(
        mode: EditorMode = EditorMode.Create(carId),
        lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(),
        backDispatcher: BackDispatcher = BackDispatcher()
    ) = DefaultServiceEditorComponent(
        componentContext = DefaultComponentContext(lifecycleRegistry, backHandler = backDispatcher),
        editorMode = mode,
        getInitialServiceEditorData = getInitialData,
        saveServiceRecord = saveRecord,
        recordRepo = recordRepository,
        onBack = { closeCount++ }
    )
}
