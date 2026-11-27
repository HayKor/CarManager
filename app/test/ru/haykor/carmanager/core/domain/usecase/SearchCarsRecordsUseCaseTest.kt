package ru.haykor.carmanager.core.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import ru.haykor.carmanager.core.domain.error.DataError
import ru.haykor.carmanager.core.domain.error.SearchError
import ru.haykor.carmanager.core.domain.fixtures.FakeSearchRepository
import ru.haykor.carmanager.core.result.DataResult
import kotlin.test.assertEquals

class SearchCarsRecordsUseCaseTest {

    @Test
    fun `blank query delegates an empty word list`() = runTest {
        val repository = FakeSearchRepository()
        val useCase = SearchCarsRecordsUseCase(repository)

        useCase("   ").first()

        assertEquals(listOf(emptyList<String>()), repository.searches)
    }

    @Test
    fun `sanitizes and caps at four words`() = runTest {
        val repository = FakeSearchRepository()
        val useCase = SearchCarsRecordsUseCase(repository)

        useCase(" a, b c d e f ").first()

        assertEquals(listOf(listOf("a", "b", "c", "d")), repository.searches)
    }

    @Test
    fun `maps data error to search error`() = runTest {
        val repository = FakeSearchRepository()
        repository.error = DataError.Local.Unknown
        val useCase = SearchCarsRecordsUseCase(repository)

        val result = useCase("Лада").first()

        assertEquals(DataResult.Failure(SearchError.Data(DataError.Local.Unknown)), result)
    }
}
