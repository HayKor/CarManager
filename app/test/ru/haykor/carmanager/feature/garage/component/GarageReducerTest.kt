package ru.haykor.carmanager.feature.garage.component

import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.junit.Test
import ru.haykor.carmanager.core.domain.fixtures.aCar

class GarageReducerTest {

    @Test
    fun `loaded cars produce cars content`() {
        val car = aCar()

        val state = GarageReducer.reduce(GarageState(), GarageMutation.CarsLoaded(listOf(car)))

        assertEquals(GarageState.Content.Cars(listOf(car)), state.content)
    }

    @Test
    fun `empty result without query produces empty garage`() {
        val state = GarageReducer.reduce(GarageState(), GarageMutation.CarsLoaded(emptyList()))

        assertEquals(GarageState.Content.EmptyGarage, state.content)
    }

    @Test
    fun `empty result with blank query produces empty garage`() {
        val state = GarageState(query = "   ")

        val reduced = GarageReducer.reduce(state, GarageMutation.CarsLoaded(emptyList()))

        assertEquals(GarageState.Content.EmptyGarage, reduced.content)
    }

    @Test
    fun `empty result with query produces empty search with trimmed query`() {
        val state = GarageState(query = "  Skyline  ")

        val reduced = GarageReducer.reduce(state, GarageMutation.CarsLoaded(emptyList()))

        assertEquals(GarageState.Content.EmptySearch("Skyline"), reduced.content)
    }

    @Test
    fun `query change preserves current content`() {
        val content = GarageState.Content.Cars(listOf(aCar()))
        val state = GarageState(content = content)

        val reduced = GarageReducer.reduce(state, GarageMutation.QueryChanged("Nissan"))

        assertEquals("Nissan", reduced.query)
        assertIs<GarageState.Content.Cars>(reduced.content)
    }
}
