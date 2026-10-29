package ru.haykor.carmanager.core.domain.usecase

import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import ru.haykor.carmanager.core.domain.fixtures.FakeCarRepository
import ru.haykor.carmanager.core.domain.fixtures.FakeServiceRecordRepository
import ru.haykor.carmanager.core.domain.fixtures.aCar
import ru.haykor.carmanager.core.domain.fixtures.aCategory
import ru.haykor.carmanager.core.domain.fixtures.aRecord
import ru.haykor.carmanager.core.domain.fixtures.recordParser
import ru.haykor.carmanager.core.domain.model.Car
import ru.haykor.carmanager.core.domain.model.ServiceRecord
import ru.haykor.carmanager.core.domain.model.ServiceRecordFormInput
import ru.haykor.carmanager.core.result.DataResult
import ru.haykor.carmanager.feature.serviceEditor.component.EditorMode

@OptIn(ExperimentalCoroutinesApi::class)
class SaveServiceRecordUseCaseTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private class Sut(initialRecords: List<ServiceRecord> = listOf(defaultRecord), car: Car = defaultCar) {
        val records = FakeServiceRecordRepository(initial = initialRecords, category = defaultCategory)
        val cars = FakeCarRepository(listOf(car))
        val useCase = SaveServiceRecordUseCase(records, cars, recordParser)
    }

    @Test
    fun `creating new latest record updates car's mileage`() = runTest(dispatcher) {
        val sut = Sut()

        val result = sut.useCase(
            mode = EditorMode.Create(defaultCar.id),
            input = input(atMileage = "14000", date = 1L), // later than defaultRecord, so it's the latest
            carId = defaultCar.id
        )

        assertIs<DataResult.Success<*>>(result)
        assertEquals(1, sut.records.created.size)
        assertEquals(14_000L, sut.records.updatedCars.single().second)
    }

    @Test
    fun `editing latest record to lower mileage results in MileageMismatch`() = runTest(dispatcher) {
        val older = aRecord(carId = defaultCar.id, categoryId = defaultCategory.id, atMileage = 8_000L, date = 0L)
        val latest = aRecord(carId = defaultCar.id, categoryId = defaultCategory.id, atMileage = 10_000L, date = 1L)
        val sut = Sut(initialRecords = listOf(older, latest))

        val result = sut.useCase(
            mode = EditorMode.Edit(latest.id),
            input = input(atMileage = "9000", date = 1L),
            carId = defaultCar.id
        )

        assertIs<DataResult.Failure<*>>(result)
        val error = assertIs<SaveRecordError.MileageMismatch>(result.error)
        assertEquals(9_000L, error.recordMileage)
        assertEquals(10_000L, error.carMileage)
        // nothing must be persisted on mismatch
        assertEquals(0, sut.records.updated.size)
        assertEquals(0, sut.records.updatedCars.size)
    }

    private fun input(atMileage: String, date: Long) = ServiceRecordFormInput(
        carId = defaultCar.id,
        atMileage = atMileage,
        categoryId = defaultCategory.id,
        date = date
    )

    private companion object {
        val defaultCar = aCar(mileage = 10_000L)
        val defaultCategory = aCategory()
        val defaultRecord = aRecord(carId = defaultCar.id, categoryId = defaultCategory.id, atMileage = 10_000L)
    }
}
