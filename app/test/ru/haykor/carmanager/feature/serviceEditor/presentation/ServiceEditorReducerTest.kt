package ru.haykor.carmanager.feature.serviceEditor.presentation

import org.junit.Test
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceCategoryId
import ru.haykor.carmanager.core.domain.fixtures.aCar
import ru.haykor.carmanager.core.domain.fixtures.aCategory
import ru.haykor.carmanager.core.domain.model.RecordField
import ru.haykor.carmanager.core.domain.model.ServiceRecordFormInput
import ru.haykor.carmanager.feature.serviceEditor.component.ServiceEditorMutation
import ru.haykor.carmanager.feature.serviceEditor.component.ServiceEditorReducer
import ru.haykor.carmanager.feature.serviceEditor.component.ServiceEditorState
import ru.haykor.carmanager.feature.serviceEditor.component.ServiceEditorState.Form
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ServiceEditorReducerTest {

    private val carId = CarId("car")
    private val category = aCategory(id = ServiceCategoryId("category"))
    private val input = ServiceRecordFormInput(
        carId = carId,
        categoryId = category.id,
        atMileage = "100000",
        price = "1500",
        date = 0L
    )
    private val form: ServiceEditorState = Form(
        input = input,
        initialInput = input,
        car = aCar(id = carId),
        categories = listOf(category)
    )

    @Test
    fun `loaded form is not dirty`() {
        val state = ServiceEditorReducer.reduce(
            ServiceEditorState.Loading,
            ServiceEditorMutation.InitialFormLoaded(
                input = input,
                car = aCar(id = carId),
                categories = listOf(category)
            )
        )
        assertIs<Form>(state)
        assertEquals(input, state.initialInput)
        assertFalse(state.isDirty)
    }

    @Test
    fun `changed field makes the form dirty`() {
        val state = ServiceEditorReducer.reduce(
            form,
            ServiceEditorMutation.FieldChanged(RecordField.AT_MILEAGE, "90000")
        )
        assertIs<Form>(state)
        assertTrue(state.isDirty)
    }

    @Test
    fun `reverting a field makes the form clean`() {
        val dirty = ServiceEditorReducer.reduce(
            form,
            ServiceEditorMutation.FieldChanged(RecordField.NOTE, "Oil change")
        )

        val clean = ServiceEditorReducer.reduce(dirty, ServiceEditorMutation.FieldChanged(RecordField.NOTE, input.note))

        assertIs<Form>(clean)
        assertFalse(clean.isDirty)
    }

    @Test
    fun `picking another category makes the form dirty`() {
        val state = ServiceEditorReducer.reduce(form, ServiceEditorMutation.CategoryPicked(ServiceCategoryId("other")))
        assertIs<Form>(state)
        assertTrue(state.isDirty)
    }

    @Test
    fun `picking another date makes the form dirty`() {
        val state = ServiceEditorReducer.reduce(form, ServiceEditorMutation.DatePicked(1L))
        assertIs<Form>(state)
        assertTrue(state.isDirty)
    }

    @Test
    fun `set deleting does not touch saving`() {
        val state = ServiceEditorReducer.reduce(form, ServiceEditorMutation.SetIsDeleting(true))
        assertIs<Form>(state)
        assertTrue(state.isDeleting)
        assertFalse(state.isSaving)
        assertTrue(state.isBusy)
    }

    @Test
    fun `exit dialog visibility is updated`() {
        val shown = ServiceEditorReducer.reduce(form, ServiceEditorMutation.SetExitDialogVisible(true))
        assertIs<Form>(shown)
        assertTrue(shown.isExitDialogVisible)

        val hidden = ServiceEditorReducer.reduce(shown, ServiceEditorMutation.SetExitDialogVisible(false))
        assertIs<Form>(hidden)
        assertFalse(hidden.isExitDialogVisible)
    }
}
