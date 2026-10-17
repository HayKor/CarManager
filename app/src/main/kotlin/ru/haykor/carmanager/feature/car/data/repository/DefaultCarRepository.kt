package ru.haykor.carmanager.feature.car.data.repository

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.haykor.carmanager.feature.car.data.local.dao.CarDao
import ru.haykor.carmanager.feature.car.data.mapper.toDomain
import ru.haykor.carmanager.feature.car.data.mapper.toEntity
import ru.haykor.carmanager.feature.car.domain.CarId
import ru.haykor.carmanager.feature.car.domain.model.Car
import ru.haykor.carmanager.feature.car.domain.repository.CarRepository

@Inject
@ContributesBinding(AppScope::class)
class DefaultCarRepository(private val carDao: CarDao) : CarRepository {
    override suspend fun upsert(car: Car) {
        carDao.upsert(car.toEntity())
    }

    override suspend fun delete(id: CarId) {
        carDao.delete(id)
    }

    override fun observeCars(): Flow<List<Car>> = carDao.observeCars().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun observeCar(id: CarId): Flow<Car?> = carDao.observeCar(id).map { entity ->
        entity?.toDomain()
    }
}
