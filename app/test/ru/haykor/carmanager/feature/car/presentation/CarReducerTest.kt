package ru.haykor.carmanager.feature.car.presentation

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import ru.haykor.carmanager.core.domain.fixtures.aCar
import ru.haykor.carmanager.feature.car.component.CarMutation
import ru.haykor.carmanager.feature.car.component.CarReducer
import ru.haykor.carmanager.feature.car.component.CarSection
import ru.haykor.carmanager.feature.car.component.CarState
import ru.haykor.carmanager.feature.car.component.RecordsSection

class CarReducerTest {

    private val car = aCar()
    private val initialState = CarState(
        carSection = CarSection.Loaded(car),
        recordsSection = RecordsSection.Loading
    )

    @Test
    fun `not found is ignored while isDeleting=true`() {
        val state = initialState.copy(isDeleting = true)
        val reduced = CarReducer.reduce(state, CarMutation.CarNotFound)

        assertEquals(CarSection.Loaded(car), reduced.carSection)
    }

    @Test
    fun `not found updates carSection to NotFound while isDeleting=false`() {
        val state = initialState.copy(isDeleting = false)
        val reduced = CarReducer.reduce(state, CarMutation.CarNotFound)

        assertEquals(CarSection.NotFound, reduced.carSection)
    }

    @Test
    fun `car loaded updates carSection to Loaded`() {
        val state = CarState()
        val reduced = CarReducer.reduce(state, CarMutation.CarLoaded(car))

        assertEquals(CarSection.Loaded(car), reduced.carSection)
    }

    @Test
    fun `records loaded updates recordsSection to Loaded`() {
        val state = CarState()
        val reduced = CarReducer.reduce(state, CarMutation.RecordsLoaded(emptyList()))

        assertEquals(RecordsSection.Loaded(emptyList()), reduced.recordsSection)
    }

    @Test
    fun `set show dialog updates isShowDialog`() {
        val state = CarState(isShowDialog = false)

        val shownState = CarReducer.reduce(state, CarMutation.SetShowDialog(true))
        assertTrue(shownState.isShowDialog)

        val hiddenState = CarReducer.reduce(shownState, CarMutation.SetShowDialog(false))
        assertFalse(hiddenState.isShowDialog)
    }

    @Test
    fun `set is deleting updates isDeleting`() {
        val state = CarState(isDeleting = false)

        val deletingState = CarReducer.reduce(state, CarMutation.SetIsDeleting(true))
        assertTrue(deletingState.isDeleting)

        val notDeletingState = CarReducer.reduce(deletingState, CarMutation.SetIsDeleting(false))
        assertFalse(notDeletingState.isDeleting)
    }
}
