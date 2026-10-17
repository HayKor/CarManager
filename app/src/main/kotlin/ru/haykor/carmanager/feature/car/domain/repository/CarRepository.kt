package ru.haykor.carmanager.feature.car.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.haykor.carmanager.feature.car.domain.CarId
import ru.haykor.carmanager.feature.car.domain.model.Car

interface CarRepository {
    suspend fun upsert(car: Car)
    suspend fun delete(id: CarId)
    fun observeCars(): Flow<List<Car>>
    fun observeCar(id: CarId): Flow<Car?>
}
