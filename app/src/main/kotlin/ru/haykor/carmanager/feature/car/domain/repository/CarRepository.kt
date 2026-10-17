package ru.haykor.carmanager.feature.car.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.feature.car.domain.model.Car
import ru.haykor.carmanager.feature.car.domain.model.CarDraft

interface CarRepository {
    suspend fun create(draft: CarDraft): CarId
    suspend fun upsert(id: CarId, draft: CarDraft)
    suspend fun delete(id: CarId)
    fun observeCars(): Flow<List<Car>>
    fun observeCar(id: CarId): Flow<Car?>
}
