package ru.haykor.carmanager.feature.car.presentation

import kotlin.test.assertIs
import org.junit.Test
import ru.haykor.carmanager.core.domain.fixtures.aCar
import ru.haykor.carmanager.feature.car.component.CarMutation
import ru.haykor.carmanager.feature.car.component.CarReducer
import ru.haykor.carmanager.feature.car.component.CarState

class CarReducerTest {

    private val content = CarState.Content(car = aCar())

    @Test
    fun `not found is ignored while isDeleting=true`() {
        val state = CarReducer.reduce(content.copy(isDeleting = true), CarMutation.NotFound)
        assertIs<CarState.Content>(state)
    }

    @Test
    fun `not found isn't ignored while isDeleting=false`() {
        val state = CarReducer.reduce(content.copy(isDeleting = false), CarMutation.NotFound)
        assertIs<CarState.NotFound>(state)
    }
}
