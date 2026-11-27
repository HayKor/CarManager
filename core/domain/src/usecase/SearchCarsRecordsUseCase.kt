package ru.haykor.carmanager.core.domain.usecase

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.haykor.carmanager.core.domain.error.SearchError
import ru.haykor.carmanager.core.domain.model.CarSearchResult
import ru.haykor.carmanager.core.domain.repository.SearchRepository
import ru.haykor.carmanager.core.domain.search.SearchQuery
import ru.haykor.carmanager.core.result.DataResult
import ru.haykor.carmanager.core.result.mapError

@Inject
class SearchCarsRecordsUseCase(private val repository: SearchRepository) {

    operator fun invoke(query: String): Flow<DataResult<List<CarSearchResult>, SearchError>> {
        val words = SearchQuery.sanitizeWords(query).take(4)
        return repository.search(words).map { it.mapError(SearchError::Data) }
    }
}
