package ru.haykor.carmanager.core.domain.repository

import ru.haykor.carmanager.core.domain.error.DataError
import ru.haykor.carmanager.core.result.DataResult
import ru.haykor.carmanager.core.result.EmptyResult

interface PhotoRepository {
    /**
     * @param source path to an image
     *
     * @return relative to filesDir path to locally copied image OR [DataError]
     */
    suspend fun save(source: String): DataResult<String, DataError>

    /**
     * is best-effort - delete as much as possible
     * @param keys list of locally copied images
     *
     * @return doesn't return any errors actually
     */
    suspend fun delete(keys: List<String>): EmptyResult<DataError>
}
