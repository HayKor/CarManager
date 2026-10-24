package ru.haykor.carmanager.feature.carEditor.presentation

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.Test
import ru.haykor.carmanager.core.domain.fixtures.aCar
import ru.haykor.carmanager.core.domain.mapper.toFormInput
import ru.haykor.carmanager.core.domain.model.CarFormInput
import ru.haykor.carmanager.core.domain.model.Field
import ru.haykor.carmanager.feature.carEditor.component.CarEditorMutation
import ru.haykor.carmanager.feature.carEditor.component.CarEditorReducer
import ru.haykor.carmanager.feature.carEditor.component.CarEditorState

class CarEditorReducerTest {

    private val loading: CarEditorState = CarEditorState.Loading
    private val form: CarEditorState = CarEditorState.Form(input = aCar().toFormInput())
    private val allFieldsSet: Set<Field> = Field.entries.toSet()

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
        val state = CarEditorReducer.reduce(form, CarEditorMutation.FieldChanged(Field.BRAND, "BMW"))
        assertIs<CarEditorState.Form>(state)
        assertEquals("BMW", state.input.brand)
        assertEquals("Qashqai", state.input.model)
    }
}
