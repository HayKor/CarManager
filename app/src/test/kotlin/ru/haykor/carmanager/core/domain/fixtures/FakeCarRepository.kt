package ru.haykor.carmanager.core.domain.fixtures

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.error.DataError
import ru.haykor.carmanager.core.domain.model.Car
import ru.haykor.carmanager.core.domain.model.CarDraft
import ru.haykor.carmanager.core.domain.repository.CarRepository
import ru.haykor.carmanager.core.result.DataResult
import ru.haykor.carmanager.core.result.EmptyResult

internal class FakeCarRepository(initial: List<Car>) : CarRepository {

    private val cars = MutableStateFlow(initial.associateBy { it.id })

    val created = mutableListOf<CarDraft>()
    val updated = mutableListOf<Pair<CarId, CarDraft>>()
    var saveGate: CompletableDeferred<Unit>? = null
    var saveError: DataError? = null

    override suspend fun create(draft: CarDraft): DataResult<CarId, DataError> {
        created.add(draft)
        saveGate?.await()
        saveError?.let { return DataResult.Failure(it) }

        val car = draft.toCar()
        cars.update { it + (car.id to car) }
        return DataResult.Success(car.id)
    }

    override suspend fun update(id: CarId, draft: CarDraft): EmptyResult<DataError> {
        updated.add(id to draft)
        saveGate?.await()
        saveError?.let { return DataResult.Failure(it) }

        val car = draft.toCar(id)
        cars.update { it + (id to car) }
        return DataResult.Success(Unit)
    }

    override suspend fun delete(id: CarId): EmptyResult<DataError> {
        cars.update { it - id }
        return DataResult.Success(Unit)
    }

    override fun observeCars(): Flow<List<Car>> = cars.map { it.values.toList() }

    override fun observeCar(id: CarId): Flow<Car?> = cars.map { it[id] }

    private fun CarDraft.toCar(id: CarId = CarId.random()) = Car(
        id = id,
        brand = brand,
        model = model,
        year = year,
        vin = vin,
        plateCountry = plateCountry,
        plate = plate,
        mileage = mileage,
        photoUri = photoUri,
        createdAt = 0L,
        updatedAt = 0L
    )
}
