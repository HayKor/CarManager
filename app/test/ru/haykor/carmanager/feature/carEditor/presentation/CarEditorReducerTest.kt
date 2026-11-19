package ru.haykor.carmanager.feature.carEditor.presentation

import org.junit.Test
import ru.haykor.carmanager.core.domain.fixtures.aCar
import ru.haykor.carmanager.core.domain.mapper.toFormInput
import ru.haykor.carmanager.core.domain.model.CarField
import ru.haykor.carmanager.core.domain.model.CarFormInput
import ru.haykor.carmanager.feature.carEditor.component.CarEditorMutation
import ru.haykor.carmanager.feature.carEditor.component.CarEditorReducer
import ru.haykor.carmanager.feature.carEditor.component.CarEditorState
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CarEditorReducerTest {

    private val loading: CarEditorState = CarEditorState.Loading
    private val loadedInput: CarFormInput = aCar().toFormInput()
    private val form: CarEditorState = CarEditorState.Form(currentInput = loadedInput, initialInput = loadedInput)
    private val allFieldsSet: Set<CarField> = CarField.entries.toSet()

    @Test
    fun `new form is not dirty`() {
        val state = CarEditorReducer.reduce(loading, CarEditorMutation.FormInputNew)
        assertIs<CarEditorState.Form>(state)
        assertFalse(state.isDirty)
    }

    @Test
    fun `loaded form is not dirty`() {
        val state = CarEditorReducer.reduce(loading, CarEditorMutation.FormInputLoaded(loadedInput))
        assertIs<CarEditorState.Form>(state)
        assertFalse(state.isDirty)
    }

    @Test
    fun `changed field makes the form dirty`() {
        val state = CarEditorReducer.reduce(form, CarEditorMutation.FieldChanged(CarField.BRAND, "BMW"))
        assertIs<CarEditorState.Form>(state)
        assertTrue(state.isDirty)
    }

    @Test
    fun `reverting a field makes the form clean`() {
        val dirty = CarEditorReducer.reduce(form, CarEditorMutation.FieldChanged(CarField.BRAND, "BMW"))
        val clean = CarEditorReducer.reduce(dirty, CarEditorMutation.FieldChanged(CarField.BRAND, loadedInput.brand))
        assertIs<CarEditorState.Form>(clean)
        assertFalse(clean.isDirty)
    }

    @Test
    fun `new form has isNew=true`() {
        val state = CarEditorReducer.reduce(loading, CarEditorMutation.FormInputNew)
        assertIs<CarEditorState.Form>(state)
        assertTrue(state.isNew)
    }

    @Test
    fun `loaded form has isNew=false`() {
        val state = CarEditorReducer.reduce(loading, CarEditorMutation.FormInputLoaded(CarFormInput()))
        assertIs<CarEditorState.Form>(state)
        assertFalse(state.isNew)
    }

    @Test
    fun `all fields touched correctly`() {
        val state = CarEditorReducer.reduce(form, CarEditorMutation.AllFieldsTouched)
        assertIs<CarEditorState.Form>(state)
        assertEquals(allFieldsSet, state.touched)
    }

    @Test
    fun `mutation while Loading is ignored`() {
        val state = CarEditorReducer.reduce(loading, CarEditorMutation.AllFieldsTouched)
        assertIs<CarEditorState.Loading>(state)
    }

    @Test
    fun `mutating a field changes only its value`() {
        val state = CarEditorReducer.reduce(form, CarEditorMutation.FieldChanged(CarField.BRAND, "BMW"))
        assertIs<CarEditorState.Form>(state)
        assertEquals("BMW", state.currentInput.brand)
        assertEquals("Qashqai", state.currentInput.model)
    }
}
