package ru.haykor.carmanager.core.domain.usecase

import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import ru.haykor.carmanager.core.domain.fixtures.FakeCarRepository
import ru.haykor.carmanager.core.domain.fixtures.aCar

class SearchCarsUseCaseTest {

    @Test
    fun `blank query observes all cars`() = runTest {
        val car = aCar()
        val repository = FakeCarRepository(listOf(car))
        val useCase = SearchCarsUseCase(repository)

        assertEquals(listOf(car), useCase("   ").first())
        assertEquals(1, repository.observeCarsCalls)
        assertEquals(emptyList(), repository.searchedQueries)
    }

    @Test
    fun `query is sanitized into unicode word prefixes`() = runTest {
        val car = aCar(brand = "Лада", model = "Vesta", note = "семейная машина")
        val repository = FakeCarRepository(listOf(car))
        val useCase = SearchCarsUseCase(repository)

        assertEquals(listOf(car), useCase("  Лада, Ves!  ").first())
        assertEquals(listOf("Лада* Ves*"), repository.searchedQueries)
        assertEquals(0, repository.observeCarsCalls)
    }

    @Test
    fun `punctuation-only query falls back to observing all cars`() = runTest {
        val repository = FakeCarRepository(listOf(aCar()))
        val useCase = SearchCarsUseCase(repository)

        useCase("!!! ---").first()

        assertEquals(1, repository.observeCarsCalls)
        assertEquals(emptyList(), repository.searchedQueries)
    }
}
