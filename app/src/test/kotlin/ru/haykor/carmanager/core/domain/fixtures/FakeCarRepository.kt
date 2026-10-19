package ru.haykor.carmanager.core.domain.fixtures

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.model.Car
import ru.haykor.carmanager.core.domain.model.CarDraft
import ru.haykor.carmanager.core.domain.repository.CarRepository

internal class FakeCarRepository(initial: List<Car>) : CarRepository {

    private val cars = MutableStateFlow(initial.associateBy { it.id })

    val created = mutableListOf<CarDraft>()
    val updated = mutableListOf<Pair<CarId, CarDraft>>()
    var saveGate: CompletableDeferred<Unit>? = null
    var saveError: Throwable? = null

    override suspend fun create(draft: CarDraft): CarId {
        created.add(draft)
        saveGate?.await()
        saveError?.let { throw it }

        val car = draft.toCar()
        cars.update { it + (car.id to car) }
        return car.id
    }

    override suspend fun update(id: CarId, draft: CarDraft) {
        updated.add(id to draft)
        saveGate?.await()
        saveError?.let { throw it }

        val car = draft.toCar(id)
        cars.update { it + (id to car) }
    }

    override suspend fun delete(id: CarId) {
        cars.update { it - id }
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
