package ru.haykor.carmanager.core.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceRecordId
import ru.haykor.carmanager.core.domain.error.DataError
import ru.haykor.carmanager.core.domain.model.ServiceRecordDetails
import ru.haykor.carmanager.core.domain.model.ServiceRecordDraft
import ru.haykor.carmanager.core.result.DataResult
import ru.haykor.carmanager.core.result.EmptyResult

interface ServiceRecordRepository {

    suspend fun create(draft: ServiceRecordDraft): EmptyResult<DataError>
    suspend fun update(id: ServiceRecordId, draft: ServiceRecordDraft): EmptyResult<DataError>
    suspend fun delete(id: ServiceRecordId): EmptyResult<DataError>

    suspend fun createWithMileage(draft: ServiceRecordDraft, newCarMileage: Long?): EmptyResult<DataError>
    suspend fun updateWithMileage(
        id: ServiceRecordId,
        draft: ServiceRecordDraft,
        newCarMileage: Long?
    ): EmptyResult<DataError>

    suspend fun latestDate(carId: CarId, excludeId: ServiceRecordId?): DataResult<Long?, DataError>

    fun observeByCar(carId: CarId): Flow<List<ServiceRecordDetails>>
    fun observeById(id: ServiceRecordId): Flow<ServiceRecordDetails?>
}
