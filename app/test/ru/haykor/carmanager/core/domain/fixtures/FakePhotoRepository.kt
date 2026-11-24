package ru.haykor.carmanager.core.domain.fixtures

import kotlinx.coroutines.CompletableDeferred
import ru.haykor.carmanager.core.domain.error.DataError
import ru.haykor.carmanager.core.domain.repository.PhotoRepository
import ru.haykor.carmanager.core.result.DataResult
import ru.haykor.carmanager.core.result.EmptyResult

internal class FakePhotoRepository : PhotoRepository {

    val saved = mutableListOf<String>()
    val deleted = mutableListOf<List<String>>()

    /** Overrides the result of [save] for every call when set. */
    var saveResult: DataResult<String, DataError>? = null

    /** Overrides the result of [delete] for every call when set. */
    var deleteResult: EmptyResult<DataError>? = null

    /** Suspends every [save] call until completed. */
    var saveGate: CompletableDeferred<Unit>? = null

    private val saveGates = mutableMapOf<String, CompletableDeferred<Unit>>()

    /** Suspends [save] for [source] until the returned deferred is completed. */
    fun gateSave(source: String): CompletableDeferred<Unit> =
        CompletableDeferred<Unit>().also { saveGates[source] = it }

    override suspend fun save(source: String): DataResult<String, DataError> {
        val index = saved.size + 1
        saved.add(source)
        saveGates[source]?.await()
        saveGate?.await()
        return saveResult ?: DataResult.Success("photo/$index.jpg")
    }

    override suspend fun delete(keys: List<String>): EmptyResult<DataError> {
        deleted.add(keys)
        return deleteResult ?: DataResult.Success(Unit)
    }

    override suspend fun saveToGallery(photoPath: String): EmptyResult<DataError> {
        // TODO: implement
        return DataResult.Success(Unit)
    }
}
