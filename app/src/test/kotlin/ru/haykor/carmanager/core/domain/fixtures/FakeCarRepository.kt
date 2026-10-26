package ru.haykor.carmanager.core.domain.fixtures

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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
    val deleted = mutableListOf<CarId>()
    val searchedQueries = mutableListOf<String>()
    var observeCarsCalls = 0
        private set

    var saveGate: CompletableDeferred<Unit>? = null
    var deleteGate: CompletableDeferred<Unit>? = null
    var saveError: DataError? = null
    var deleteError: DataError? = null

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
        deleted.add(id)
        deleteGate?.await()
        deleteError?.let { return DataResult.Failure(it) }

        cars.update { it - id }
        return DataResult.Success(Unit)
    }

    override fun observeCars(): Flow<List<Car>> {
        observeCarsCalls++
        return cars.map { it.values.toList() }
    }

    override fun observeCar(id: CarId): Flow<Car?> = cars.map { it[id] }

    override fun searchCars(query: String): Flow<List<Car>> = flow {
        searchedQueries += query
        emitAll(
            cars.map { currentCars ->
                val terms = query.split(' ').map { it.removeSuffix("*") }.filter(String::isNotBlank)
                currentCars.values.filter { car ->
                    val searchableText = listOfNotNull(car.brand, car.model, car.plate, car.note)
                        .joinToString(" ")
                        .lowercase()
                    terms.all { term -> searchableText.split(Regex("\\s+")).any { it.startsWith(term.lowercase()) } }
                }
            }
        )
    }

    private fun CarDraft.toCar(id: CarId = CarId.random()) = Car(
        id = id,
        brand = brand,
        model = model,
        year = year,
        note = note,
        vin = vin,
        plateCountry = plateCountry,
        plate = plate,
        mileage = mileage,
        photoUri = photoUri,
        createdAt = 0L,
        updatedAt = 0L
    )
}
