package ru.haykor.carmanager.core.domain.fixtures

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceRecordId
import ru.haykor.carmanager.core.domain.error.DataError
import ru.haykor.carmanager.core.domain.model.ServiceRecordDetails
import ru.haykor.carmanager.core.domain.model.ServiceRecordDraft
import ru.haykor.carmanager.core.domain.repository.ServiceRecordRepository
import ru.haykor.carmanager.core.result.DataResult
import ru.haykor.carmanager.core.result.EmptyResult

internal class FakeServiceRecordRepository(
    initial: List<ServiceRecordDetails> = emptyList()
) : ServiceRecordRepository {

    private val records = MutableStateFlow(initial)

    override suspend fun create(draft: ServiceRecordDraft): EmptyResult<DataError> {
        return DataResult.Success(Unit)
    }

    override suspend fun update(id: ServiceRecordId, draft: ServiceRecordDraft): EmptyResult<DataError> {
        return DataResult.Success(Unit)
    }

    override suspend fun delete(id: ServiceRecordId): EmptyResult<DataError> {
        return DataResult.Success(Unit)
    }

    override fun observeByCar(carId: CarId): Flow<List<ServiceRecordDetails>> {
        return records.map { list -> list.filter { it.record.carId == carId } }
    }
}
