package ru.haykor.carmanager.core.result

sealed interface DataResult<out D, out E : AppError> {
    data class Success<out D>(val data: D) : DataResult<D, Nothing>

    data class Failure<out E : AppError>(val error: E) : DataResult<Nothing, E>
}

inline fun <D, E : AppError, R> DataResult<D, E>.map(transform: (D) -> R): DataResult<R, E> = when (this) {
    is DataResult.Success -> DataResult.Success(transform(data))
    is DataResult.Failure -> this
}

inline fun <D, E : AppError, R : AppError> DataResult<D, E>.mapError(transform: (E) -> R): DataResult<D, R> =
    when (this) {
        is DataResult.Success -> this
        is DataResult.Failure -> DataResult.Failure(transform(error))
    }

inline fun <D, E : AppError, R> DataResult<D, E>.flatMap(transform: (D) -> DataResult<R, E>): DataResult<R, E> =
    when (this) {
        is DataResult.Success -> transform(data)
        is DataResult.Failure -> this
    }

inline fun <D, E : AppError> DataResult<D, E>.onSuccess(block: (D) -> Unit): DataResult<D, E> =
    also { if (it is DataResult.Success) block(it.data) }

inline fun <D, E : AppError> DataResult<D, E>.onFailure(block: (E) -> Unit): DataResult<D, E> =
    also { if (it is DataResult.Failure) block(it.error) }

inline fun <D, E : AppError> DataResult<D, E>.getOrElse(onFailure: (E) -> D): D = when (this) {
    is DataResult.Success -> data
    is DataResult.Failure -> onFailure(error)
}

inline fun <D, E : AppError> DataResult<D, E>.getOrNull(): D? = when (this) {
    is DataResult.Success -> data
    is DataResult.Failure -> null
}

inline fun <D, E : AppError, R> DataResult<D, E>.fold(onSuccess: (D) -> R, onFailure: (E) -> R): R = when (this) {
    is DataResult.Success -> onSuccess(data)
    is DataResult.Failure -> onFailure(error)
}
