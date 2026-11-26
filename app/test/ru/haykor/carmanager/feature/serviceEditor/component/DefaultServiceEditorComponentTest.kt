package ru.haykor.carmanager.feature.serviceEditor.component

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import ru.haykor.carmanager.core.domain.error.DataError
import ru.haykor.carmanager.core.domain.fixtures.FakeCarRepository
import ru.haykor.carmanager.core.domain.fixtures.FakePhotoRepository
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
import ru.haykor.carmanager.core.result.DataResult
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
        date = 0L,
        photoUri = STORED_PHOTO
    )

    // Saved record with a photo and mileage equal to the car's, so saving needs no conflict dialog
    private val editableRecord = aRecord(
        id = ServiceRecordId("editable-record"),
        carId = carId,
        categoryId = category.id,
        atMileage = car.mileage,
        date = 0L,
        photoUri = STORED_PHOTO
    )

    private val carRepository = FakeCarRepository(listOf(car))
    private val recordRepository = FakeServiceRecordRepository(listOf(existingRecord), category)
    private val categoryRepository = FakeServiceCategoryRepository(listOf(category))
    private val photoRepository = FakePhotoRepository()
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
    fun `edit mode restores the stored photo`() = runTest(dispatcher) {
        val component = createComponent(mode = EditorMode.Edit(existingRecord.id))

        val state = component.state.value
        assertIs<Form>(state)
        assertEquals(STORED_PHOTO, state.input.photoUri)
    }

    @Test
    fun `back without changes closes the screen immediately`() = runTest(dispatcher) {
        val component = createComponent()

        component.accept(ServiceEditorIntent.BackClicked)

        val state = component.state.value
        assertIs<Form>(state)
        assertFalse(state.isExitDialogVisible)
        assertEquals(1, closeCount)
        assertTrue(photoRepository.deleted.isEmpty())
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
        assertTrue(photoRepository.deleted.isEmpty())

        // Act: the save finishes after the user has already left
        gate.complete(Unit)

        // Assert
        assertEquals(1, closeCount)
        assertTrue(photoRepository.deleted.flatten().isEmpty())
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
    fun `delete removes the record photo from storage`() = runTest(dispatcher) {
        val component = createComponent(mode = EditorMode.Edit(existingRecord.id))

        component.accept(ServiceEditorIntent.DeleteClicked)

        assertEquals(listOf(listOf(STORED_PHOTO)), photoRepository.deleted)
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

    // region photo import

    @Test
    fun `picking an image imports it and makes the form dirty`() = runTest(dispatcher) {
        val component = createComponent()

        component.accept(ServiceEditorIntent.ImagePicked("picked.jpg"))

        val state = component.state.value
        assertIs<Form>(state)
        assertEquals(listOf("picked.jpg"), photoRepository.saved)
        assertEquals("photo/1.jpg", state.input.photoUri)
        assertTrue(state.isDirty)
        assertFalse(state.isImporting)
    }

    @Test
    fun `cancelling the picker keeps the form untouched`() = runTest(dispatcher) {
        val component = createComponent()
        val before = component.state.value

        component.accept(ServiceEditorIntent.ImagePicked(null))

        assertEquals(before, component.state.value)
        assertTrue(photoRepository.saved.isEmpty())
    }

    @Test
    fun `failed import keeps the stored photo and emits an event`() = runTest(dispatcher) {
        photoRepository.saveResult = DataResult.Failure(DataError.Local.Unknown)
        val component = createComponent(mode = EditorMode.Edit(existingRecord.id))

        component.accept(ServiceEditorIntent.ImagePicked("picked.jpg"))

        val state = component.state.value
        assertIs<Form>(state)
        assertEquals(STORED_PHOTO, state.input.photoUri)
        assertFalse(state.isImporting)
        assertEquals(
            ServiceEditorEvent.PhotoImportFailed(DataError.Local.Unknown),
            component.events.first()
        )
    }

    @Test
    fun `the last pick wins when imports finish out of order`() = runTest(dispatcher) {
        val firstGate = photoRepository.gateSave("first.jpg")
        val secondGate = photoRepository.gateSave("second.jpg")
        val component = createComponent()

        component.accept(ServiceEditorIntent.ImagePicked("first.jpg"))
        component.accept(ServiceEditorIntent.ImagePicked("second.jpg"))
        assertTrue((component.state.value as Form).isImporting)

        // The second pick finishes first
        secondGate.complete(Unit)
        assertEquals("photo/2.jpg", (component.state.value as Form).input.photoUri)

        // The stale first pick must not overwrite it
        firstGate.complete(Unit)

        val state = component.state.value
        assertIs<Form>(state)
        assertEquals("photo/2.jpg", state.input.photoUri)
        assertFalse(state.isImporting)
    }

    @Test
    fun `save during import is ignored and later saves the photo`() = runTest(dispatcher) {
        val gate = photoRepository.gateSave("picked.jpg")
        val component = createComponent()

        component.accept(ServiceEditorIntent.ImagePicked("picked.jpg"))
        component.accept(ServiceEditorIntent.SaveClicked)

        assertTrue(recordRepository.created.isEmpty())
        assertEquals(0, closeCount)

        gate.complete(Unit)
        component.accept(ServiceEditorIntent.SaveClicked)

        assertEquals(1, recordRepository.created.size)
        assertEquals("photo/1.jpg", recordRepository.created.single().photoUri)
        assertEquals(1, closeCount)
    }

    // endregion

    // region discard

    @Test
    fun `discarding changes deletes all picked images`() = runTest(dispatcher) {
        val component = createComponent()
        component.accept(ServiceEditorIntent.ImagePicked("first.jpg"))
        component.accept(ServiceEditorIntent.ImagePicked("second.jpg"))
        component.accept(ServiceEditorIntent.BackClicked)

        assertTrue((component.state.value as Form).isExitDialogVisible)

        component.accept(ServiceEditorIntent.DialogIgnoreClicked)

        assertEquals(listOf(listOf("photo/1.jpg", "photo/2.jpg")), photoRepository.deleted)
        assertEquals(1, closeCount)
    }

    @Test
    fun `discarding a picked image deletes it and closes`() = runTest(dispatcher) {
        val component = createComponent()
        component.accept(ServiceEditorIntent.ImagePicked("picked.jpg"))
        component.accept(ServiceEditorIntent.BackClicked)

        component.accept(ServiceEditorIntent.DialogIgnoreClicked)

        assertEquals(listOf(listOf("photo/1.jpg")), photoRepository.deleted)
        assertEquals(1, closeCount)
    }

    @Test
    fun `discarding an edit deletes only the new pick`() = runTest(dispatcher) {
        val component = createComponent(mode = EditorMode.Edit(existingRecord.id))
        component.accept(ServiceEditorIntent.ImagePicked("picked.jpg"))
        component.accept(ServiceEditorIntent.BackClicked)

        component.accept(ServiceEditorIntent.DialogIgnoreClicked)

        assertEquals(listOf(listOf("photo/1.jpg")), photoRepository.deleted)
        assertTrue(photoRepository.deleted.flatten().none { it == STORED_PHOTO })
        assertEquals(1, closeCount)
    }

    // endregion

    // region save

    @Test
    fun `saving deletes the replaced pick`() = runTest(dispatcher) {
        val component = createComponent()
        component.accept(ServiceEditorIntent.ImagePicked("first.jpg"))
        component.accept(ServiceEditorIntent.ImagePicked("second.jpg"))

        component.accept(ServiceEditorIntent.SaveClicked)

        assertEquals("photo/2.jpg", recordRepository.created.single().photoUri)
        assertEquals(listOf(listOf("photo/1.jpg")), photoRepository.deleted)
        assertEquals(1, closeCount)
    }

    @Test
    fun `saving keeps the only picked photo`() = runTest(dispatcher) {
        val component = createComponent()
        component.accept(ServiceEditorIntent.ImagePicked("picked.jpg"))

        component.accept(ServiceEditorIntent.SaveClicked)

        assertEquals(listOf(emptyList<String>()), photoRepository.deleted)
        assertEquals(1, closeCount)
    }

    @Test
    fun `saving without photos deletes nothing`() = runTest(dispatcher) {
        val component = createComponent()

        component.accept(ServiceEditorIntent.SaveClicked)

        assertEquals(listOf(emptyList<String>()), photoRepository.deleted)
        assertEquals(1, closeCount)
    }

    @Test
    fun `edit without a photo replacement keeps the stored photo`() = runTest(dispatcher) {
        val records = FakeServiceRecordRepository(listOf(editableRecord), category)
        val component = createComponent(mode = EditorMode.Edit(editableRecord.id), records = records)

        component.accept(ServiceEditorIntent.SaveClicked)

        assertEquals(1, records.updated.size)
        assertEquals(listOf(emptyList<String>()), photoRepository.deleted)
        assertEquals(1, closeCount)
    }

    @Test
    fun `edit with a new photo deletes the replaced stored photo`() = runTest(dispatcher) {
        val records = FakeServiceRecordRepository(listOf(editableRecord), category)
        val component = createComponent(mode = EditorMode.Edit(editableRecord.id), records = records)

        component.accept(ServiceEditorIntent.ImagePicked("picked.jpg"))
        component.accept(ServiceEditorIntent.SaveClicked)

        assertEquals("photo/1.jpg", records.updated.single().second.photoUri)
        assertEquals(listOf(listOf(STORED_PHOTO)), photoRepository.deleted)
        assertEquals(1, closeCount)
    }

    @Test
    fun `unpinning the stored photo deletes it after saving`() = runTest(dispatcher) {
        val records = FakeServiceRecordRepository(listOf(editableRecord), category)
        val component = createComponent(mode = EditorMode.Edit(editableRecord.id), records = records)

        component.accept(ServiceEditorIntent.ImageUnpinned)
        component.accept(ServiceEditorIntent.SaveClicked)

        assertNull(records.updated.single().second.photoUri)
        assertEquals(listOf(listOf(STORED_PHOTO)), photoRepository.deleted)
        assertEquals(1, closeCount)
    }

    @Test
    fun `unpinning the stored photo and discarding keeps it`() = runTest(dispatcher) {
        val component = createComponent(mode = EditorMode.Edit(existingRecord.id))

        component.accept(ServiceEditorIntent.ImageUnpinned)
        component.accept(ServiceEditorIntent.BackClicked)
        component.accept(ServiceEditorIntent.DialogIgnoreClicked)

        assertTrue(photoRepository.deleted.flatten().none { it == STORED_PHOTO })
        assertEquals(1, closeCount)
    }

    @Test
    fun `unpinning a picked photo drops it from the record`() = runTest(dispatcher) {
        val component = createComponent()
        component.accept(ServiceEditorIntent.ImagePicked("picked.jpg"))
        component.accept(ServiceEditorIntent.ImageUnpinned)

        assertNull((component.state.value as Form).input.photoUri)

        component.accept(ServiceEditorIntent.SaveClicked)

        assertNull(recordRepository.created.single().photoUri)
        assertEquals(listOf(listOf("photo/1.jpg")), photoRepository.deleted)
    }

    @Test
    fun `pick then unpin then back deletes the import without a dialog`() = runTest(dispatcher) {
        val component = createComponent()
        component.accept(ServiceEditorIntent.ImagePicked("picked.jpg"))
        component.accept(ServiceEditorIntent.ImageUnpinned)

        assertFalse((component.state.value as Form).isDirty)

        component.accept(ServiceEditorIntent.BackClicked)

        assertEquals(listOf(listOf("photo/1.jpg")), photoRepository.deleted)
        assertEquals(1, closeCount)
    }

    @Test
    fun `pick then unpin still intercepts system back`() = runTest(dispatcher) {
        val backDispatcher = BackDispatcher()
        val component = createComponent(backDispatcher = backDispatcher)
        component.accept(ServiceEditorIntent.ImagePicked("picked.jpg"))
        component.accept(ServiceEditorIntent.ImageUnpinned)

        assertTrue(backDispatcher.back())

        assertEquals(listOf(listOf("photo/1.jpg")), photoRepository.deleted)
        assertEquals(1, closeCount)
    }

    @Test
    fun `unpinning during import wins over the late import`() = runTest(dispatcher) {
        val gate = photoRepository.gateSave("picked.jpg")
        val component = createComponent(mode = EditorMode.Edit(existingRecord.id))

        component.accept(ServiceEditorIntent.ImagePicked("picked.jpg"))
        component.accept(ServiceEditorIntent.ImageUnpinned)
        gate.complete(Unit)

        val state = component.state.value
        assertIs<Form>(state)
        assertNull(state.input.photoUri)
        assertFalse(state.isImporting)
    }

    @Test
    fun `failed save keeps picked images until the changes are discarded`() = runTest(dispatcher) {
        recordRepository.writeError = DataError.Local.Unknown
        val component = createComponent()
        component.accept(ServiceEditorIntent.ImagePicked("first.jpg"))
        component.accept(ServiceEditorIntent.ImagePicked("second.jpg"))

        component.accept(ServiceEditorIntent.SaveClicked)

        assertTrue(photoRepository.deleted.isEmpty())
        assertFalse((component.state.value as Form).isSaving)

        component.accept(ServiceEditorIntent.BackClicked)
        component.accept(ServiceEditorIntent.DialogIgnoreClicked)

        assertEquals(listOf(listOf("photo/1.jpg", "photo/2.jpg")), photoRepository.deleted)
        assertEquals(1, closeCount)
    }

    @Test
    fun `failed photo cleanup still closes the screen after saving`() = runTest(dispatcher) {
        photoRepository.deleteResult = DataResult.Failure(DataError.Local.Unknown)
        val component = createComponent()
        component.accept(ServiceEditorIntent.ImagePicked("picked.jpg"))

        component.accept(ServiceEditorIntent.SaveClicked)

        assertEquals(1, recordRepository.created.size)
        assertEquals(1, closeCount)
    }

    // endregion

    private fun createComponent(
        mode: EditorMode = EditorMode.Create(carId),
        lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(),
        backDispatcher: BackDispatcher = BackDispatcher(),
        records: FakeServiceRecordRepository = recordRepository
    ) = DefaultServiceEditorComponent(
        componentContext = DefaultComponentContext(lifecycleRegistry, backHandler = backDispatcher),
        editorMode = mode,
        getInitialServiceEditorData = GetInitialServiceEditorDataUseCase(
            carRepository,
            records,
            categoryRepository,
            clock
        ),
        saveServiceRecord = SaveServiceRecordUseCase(records, carRepository, recordParser),
        recordRepo = records,
        photoRepo = photoRepository,
        onBack = { closeCount++ }
    )

    private companion object {
        const val STORED_PHOTO = "photo/stored.jpg"
    }
}
