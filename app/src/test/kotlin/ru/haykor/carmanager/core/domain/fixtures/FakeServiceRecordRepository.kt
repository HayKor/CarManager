package ru.haykor.carmanager.core.domain.fixtures

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceRecordId
import ru.haykor.carmanager.core.domain.error.DataError
import ru.haykor.carmanager.core.domain.model.ServiceCategory
import ru.haykor.carmanager.core.domain.model.ServiceRecord
import ru.haykor.carmanager.core.domain.model.ServiceRecordDetails
import ru.haykor.carmanager.core.domain.model.ServiceRecordDraft
import ru.haykor.carmanager.core.domain.repository.ServiceRecordRepository
import ru.haykor.carmanager.core.result.DataResult
import ru.haykor.carmanager.core.result.EmptyResult

internal class FakeServiceRecordRepository(initial: List<ServiceRecord>, val category: ServiceCategory) :
    ServiceRecordRepository {

    private val records = MutableStateFlow(initial.associateBy { it.id })

    val created = mutableListOf<ServiceRecordDraft>()

    // updated cars with their new mileage if present
    val updatedCars = mutableListOf<Pair<CarId, Long?>>()
    val updated = mutableListOf<Pair<ServiceRecordId, ServiceRecordDraft>>()

    override suspend fun create(draft: ServiceRecordDraft): EmptyResult<DataError> {
        created.add(draft)

        val record = draft.toDomain()
        records.update { it + (record.id to record) }
        return DataResult.Success(Unit)
    }

    override suspend fun update(id: ServiceRecordId, draft: ServiceRecordDraft): EmptyResult<DataError> {
        updated.add(id to draft)

        val record = draft.toDomain()
        records.update { it + (record.id to record) }
        return DataResult.Success(Unit)
    }

    override suspend fun delete(id: ServiceRecordId): EmptyResult<DataError> = DataResult.Success(Unit)

    override suspend fun createWithMileage(draft: ServiceRecordDraft, newCarMileage: Long?): EmptyResult<DataError> {
        created.add(draft)

        val record = draft.toDomain()
        records.update { it + (record.id to record) }
        updatedCars.add(record.carId to newCarMileage)
        return DataResult.Success(Unit)
    }

    override suspend fun updateWithMileage(
        id: ServiceRecordId,
        draft: ServiceRecordDraft,
        newCarMileage: Long?
    ): EmptyResult<DataError> {
        updated.add(id to draft)

        val record = draft.toDomain(id)
        records.update { it + (record.id to record) }
        updatedCars.add(record.carId to newCarMileage)
        return DataResult.Success(Unit)
    }

    override suspend fun latestDate(carId: CarId, excludeId: ServiceRecordId?): DataResult<Long?, DataError> {
        val latestDate = records.value.values
            .filter { it.carId == carId && it.id != excludeId }
            .maxOfOrNull { it.date }
        return DataResult.Success(latestDate)
    }

    override fun observeByCar(carId: CarId): Flow<List<ServiceRecordDetails>> = records.map { records ->
        records.values.filter { record -> record.carId == carId }
            .map { record -> ServiceRecordDetails(record, category) }
    }

    override fun observeById(id: ServiceRecordId): Flow<ServiceRecordDetails?> = records.map { records ->
        records[id]?.let { record -> ServiceRecordDetails(record, category) }
    }

    private fun ServiceRecordDraft.toDomain(id: ServiceRecordId = ServiceRecordId.random()) = ServiceRecord(
        id = id,
        carId = carId,
        atMileage = atMileage,
        price = price,
        categoryId = categoryId,
        note = note,
        photoUri = photoUri,
        date = date,
        createdAt = 0L,
        updatedAt = 0L
    )
}
