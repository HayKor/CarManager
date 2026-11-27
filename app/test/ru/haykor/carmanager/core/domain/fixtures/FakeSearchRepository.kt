package ru.haykor.carmanager.core.domain.fixtures

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.haykor.carmanager.core.domain.error.DataError
import ru.haykor.carmanager.core.domain.model.CarSearchResult
import ru.haykor.carmanager.core.domain.repository.SearchRepository
import ru.haykor.carmanager.core.result.DataResult

internal class FakeSearchRepository : SearchRepository {

    val searches = mutableListOf<List<String>>()
    var results: List<CarSearchResult> = emptyList()
    var error: DataError? = null

    override fun search(words: List<String>): Flow<DataResult<List<CarSearchResult>, DataError>> = flow {
        searches += words
        emit(error?.let { DataResult.Failure(it) } ?: DataResult.Success(results))
    }
}
