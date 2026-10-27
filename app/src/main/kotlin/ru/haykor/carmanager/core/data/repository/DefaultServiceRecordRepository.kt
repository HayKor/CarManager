package ru.haykor.carmanager.core.data.repository

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.haykor.carmanager.core.data.local.dao.ServiceRecordDao
import ru.haykor.carmanager.core.data.mapper.toDetails
import ru.haykor.carmanager.core.data.mapper.toEntity
import ru.haykor.carmanager.core.data.mapper.toUpdate
import ru.haykor.carmanager.core.data.safeCall
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceRecordId
import ru.haykor.carmanager.core.domain.error.DataError
import ru.haykor.carmanager.core.domain.model.ServiceRecordDetails
import ru.haykor.carmanager.core.domain.model.ServiceRecordDraft
import ru.haykor.carmanager.core.domain.repository.ServiceRecordRepository
import ru.haykor.carmanager.core.result.EmptyResult

@Inject
@ContributesBinding(AppScope::class)
class DefaultServiceRecordRepository(private val recordDao: ServiceRecordDao, private val clock: Clock) :
    ServiceRecordRepository {

    override suspend fun create(draft: ServiceRecordDraft): EmptyResult<DataError> = safeCall {
        val id = ServiceRecordId.random()
        val now = clock.now().toEpochMilliseconds()
        recordDao.insert(draft.toEntity(id, createdAt = now, updatedAt = now))
    }

    override suspend fun update(id: ServiceRecordId, draft: ServiceRecordDraft): EmptyResult<DataError> = safeCall {
        val now = clock.now().toEpochMilliseconds()
        recordDao.update(draft.toUpdate(id = id, updatedAt = now))
    }

    override suspend fun delete(id: ServiceRecordId): EmptyResult<DataError> = safeCall {
        recordDao.delete(id)
    }

    override fun observeByCar(carId: CarId): Flow<List<ServiceRecordDetails>> =
        recordDao.observeByCar(carId).map { records ->
            records.map { it.toDetails() }
        }

    override fun observeById(id: ServiceRecordId): Flow<ServiceRecordDetails?> =
        recordDao.observeWithCategory(id).map { withCategory ->
            withCategory?.toDetails()
        }
}
