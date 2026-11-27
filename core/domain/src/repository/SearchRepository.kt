package ru.haykor.carmanager.core.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.haykor.carmanager.core.domain.error.DataError
import ru.haykor.carmanager.core.domain.model.CarSearchResult
import ru.haykor.carmanager.core.result.DataResult

interface SearchRepository {
    fun search(words: List<String>): Flow<DataResult<List<CarSearchResult>, DataError>>
}
