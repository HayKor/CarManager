package ru.haykor.carmanager.core.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.error.DataError
import ru.haykor.carmanager.core.domain.model.Car
import ru.haykor.carmanager.core.domain.model.CarDraft
import ru.haykor.carmanager.core.result.DataResult
import ru.haykor.carmanager.core.result.EmptyResult

interface CarRepository {
    suspend fun create(draft: CarDraft): DataResult<CarId, DataError>
    suspend fun update(id: CarId, draft: CarDraft): EmptyResult<DataError>
    suspend fun delete(id: CarId): EmptyResult<DataError>

    fun observeCars(): Flow<List<Car>>
    fun observeCar(id: CarId): Flow<Car?>
}
