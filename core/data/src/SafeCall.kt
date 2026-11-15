package ru.haykor.carmanager.core.data

import androidx.sqlite.SQLiteException
import ru.haykor.carmanager.core.domain.error.DataError
import ru.haykor.carmanager.core.result.DataResult
import kotlin.coroutines.cancellation.CancellationException

suspend inline fun <T> safeCall(block: () -> T): DataResult<T, DataError> = try {
    DataResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: SQLiteException) {
    DataResult.Failure(DataError.Local.Unknown)
}
