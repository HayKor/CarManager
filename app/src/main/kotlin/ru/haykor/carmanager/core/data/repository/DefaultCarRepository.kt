package ru.haykor.carmanager.core.data.repository

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.haykor.carmanager.core.data.local.dao.CarDao
import ru.haykor.carmanager.core.data.mapper.toDomain
import ru.haykor.carmanager.core.data.mapper.toEntity
import ru.haykor.carmanager.core.data.mapper.toUpdate
import ru.haykor.carmanager.core.data.safeCall
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.error.DataError
import ru.haykor.carmanager.core.domain.model.Car
import ru.haykor.carmanager.core.domain.model.CarDraft
import ru.haykor.carmanager.core.domain.repository.CarRepository
import ru.haykor.carmanager.core.result.DataResult

@Inject
@ContributesBinding(AppScope::class)
class DefaultCarRepository(private val carDao: CarDao, private val clock: Clock) : CarRepository {

    override suspend fun create(draft: CarDraft): DataResult<CarId, DataError> = safeCall {
        val id = CarId.random()
        val now = clock.now().toEpochMilliseconds()
        carDao.insert(draft.toEntity(id, createdAt = now, updatedAt = now))
        return@safeCall id
    }

    override suspend fun update(id: CarId, draft: CarDraft) = safeCall {
        carDao.update(draft.toUpdate(id, clock.now().toEpochMilliseconds()))
    }

    override suspend fun delete(id: CarId) = safeCall {
        carDao.delete(id)
    }

    override fun observeCars(): Flow<List<Car>> = carDao.observeCars().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun observeCar(id: CarId): Flow<Car?> = carDao.observeCar(id).map { entity ->
        entity?.toDomain()
    }
}
