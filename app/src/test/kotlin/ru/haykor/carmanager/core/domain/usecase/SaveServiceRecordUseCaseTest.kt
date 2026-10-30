package ru.haykor.carmanager.core.domain.usecase

import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
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
import ru.haykor.carmanager.core.result.EmptyResult
import ru.haykor.carmanager.feature.serviceEditor.component.EditorMode

@OptIn(ExperimentalCoroutinesApi::class)
class SaveServiceRecordUseCaseTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private class Sut(initialRecords: List<ServiceRecord> = listOf(defaultRecord), car: Car = defaultCar) {
        val records = FakeServiceRecordRepository(initial = initialRecords, category = defaultCategory)
        val cars = FakeCarRepository(listOf(car))
        val useCase = SaveServiceRecordUseCase(records, cars, recordParser)

        suspend fun create(atMileage: Long, date: Long, resolution: MileageResolution = MileageResolution.ASK) =
            useCase(
                mode = EditorMode.Create(defaultCar.id),
                input = input(atMileage.toString(), date),
                carId = defaultCar.id,
                resolution = resolution
            )

        suspend fun edit(
            record: ServiceRecord,
            atMileage: Long,
            date: Long = record.date,
            resolution: MileageResolution = MileageResolution.ASK
        ) = useCase(
            mode = EditorMode.Edit(record.id),
            input = input(atMileage.toString(), date),
            carId = defaultCar.id,
            resolution = resolution
        )

        fun assertNothingPersisted() {
            assertEquals(0, records.created.size, "created")
            assertEquals(0, records.updated.size, "updated")
            assertEquals(0, records.updatedCars.size, "updatedCars")
        }
    }

    // ---------- Create ----------

    @Test
    fun `create latest record above car mileage raises car mileage`() = runTest(dispatcher) {
        val sut = Sut() // car 10_000, one record at 10_000 on day 0

        val result = sut.create(atMileage = 14_000L, date = 1L)

        assertIs<DataResult.Success<*>>(result)
        assertEquals(14_000L, sut.records.created.single().atMileage)
        assertEquals(defaultCar.id to 14_000L, sut.records.updatedCars.single())
    }

    @Test
    fun `create latest record with mileage equal to car's doesn't touch car`() = runTest(dispatcher) {
        val sut = Sut()

        val result = sut.create(atMileage = 10_000L, date = 1L)

        assertIs<DataResult.Success<*>>(result)
        assertEquals(1, sut.records.created.size)
        assertEquals(0, sut.records.updatedCars.size)
    }

    @Test
    fun `create first record of a car without history raises car mileage`() = runTest(dispatcher) {
        val sut = Sut(initialRecords = emptyList())

        val result = sut.create(atMileage = 12_000L, date = 1L)

        assertIs<DataResult.Success<*>>(result)
        assertEquals(12_000L, sut.records.updatedCars.single().second)
    }

    @Test
    fun `create latest record below car mileage returns MileageMismatch and persists nothing`() = runTest(dispatcher) {
        val sut = Sut(listOf(record(atMileage = 8_000L, date = 0L)))

        val result = sut.create(atMileage = 9_000L, date = 1L)

        val error = assertFailure<SaveRecordError.MileageMismatch>(result)
        assertEquals(9_000L, error.recordMileage)
        assertEquals(10_000L, error.carMileage)
        sut.assertNothingPersisted()
    }

    @Test
    fun `create latest record below car mileage with UPDATE_CAR lowers car mileage`() = runTest(dispatcher) {
        val sut = Sut(listOf(record(atMileage = 8_000L, date = 0L)))

        val result = sut.create(atMileage = 9_000L, date = 1L, resolution = MileageResolution.UPDATE_CAR)

        assertIs<DataResult.Success<*>>(result)
        assertEquals(1, sut.records.created.size)
        assertEquals(9_000L, sut.records.updatedCars.single().second)
    }

    @Test
    fun `create latest record below car mileage with KEEP_CAR saves record and keeps car`() = runTest(dispatcher) {
        val sut = Sut(listOf(record(atMileage = 8_000L, date = 0L)))

        val result = sut.create(atMileage = 9_000L, date = 1L, resolution = MileageResolution.KEEP_CAR)

        assertIs<DataResult.Success<*>>(result)
        assertEquals(1, sut.records.created.size)
        assertEquals(0, sut.records.updatedCars.size)
    }

    @Test
    fun `create backdated record never shows mismatch and never touches car`() = runTest(dispatcher) {
        for (resolution in MileageResolution.entries) {
            val sut = Sut(listOf(low, high))

            // between low and high by date, below car mileage (10_000), but not the latest record
            val result = sut.create(atMileage = 7_000L, date = 5L, resolution = resolution)

            assertIs<DataResult.Success<*>>(result, "resolution=$resolution")
            assertEquals(1, sut.records.created.size, "resolution=$resolution")
            assertEquals(0, sut.records.updatedCars.size, "resolution=$resolution")
        }
    }

    // ---------- Mileage sequence ----------

    @Test
    fun `create record below lower neighbour returns MileageOutOfSequence`() = runTest(dispatcher) {
        val sut = Sut(listOf(low, high))

        val result = sut.create(atMileage = 4_000L, date = 5L)

        val error = assertFailure<SaveRecordError.MileageOutOfSequence>(result)
        assertEquals(5_000L, error.min)
        assertEquals(10_000L, error.max)
        assertEquals(4_000L, error.recordMileage)
        sut.assertNothingPersisted()
    }

    @Test
    fun `create record above upper neighbour returns MileageOutOfSequence`() = runTest(dispatcher) {
        val sut = Sut(listOf(low, high))

        val result = sut.create(atMileage = 11_000L, date = 5L)

        assertFailure<SaveRecordError.MileageOutOfSequence>(result)
        sut.assertNothingPersisted()
    }

    @Test
    fun `create record with mileage equal to a neighbour's is allowed`() = runTest(dispatcher) {
        for (mileage in listOf(5_000L, 10_000L)) {
            val sut = Sut(listOf(low, high))

            val result = sut.create(atMileage = mileage, date = 5L)

            assertIs<DataResult.Success<*>>(result, "mileage=$mileage")
        }
    }

    @Test
    fun `out of sequence takes priority over mismatch`() = runTest(dispatcher) {
        // 7_000 is below both the lower neighbor (8_000) and the car (10_000)
        val sut = Sut(listOf(record(atMileage = 8_000L, date = 0L)))

        val result = sut.create(atMileage = 7_000L, date = 1L)

        assertFailure<SaveRecordError.MileageOutOfSequence>(result)
    }

    @Test
    fun `records on the same date don't constrain mileage`() = runTest(dispatcher) {
        val sut = Sut(listOf(record(atMileage = 12_000L, date = 1L)))

        // KEEP_CAR so the car-mileage dialog doesn't interfere
        val result = sut.create(atMileage = 9_000L, date = 1L, resolution = MileageResolution.KEEP_CAR)

        assertIs<DataResult.Success<*>>(result)
    }

    // ---------- Edit ----------

    @Test
    fun `edit latest record above car mileage raises car mileage`() = runTest(dispatcher) {
        val latest = record(atMileage = 8_000L, date = 1L)
        val sut = Sut(listOf(latest))

        val result = sut.edit(latest, atMileage = 11_000L)

        assertIs<DataResult.Success<*>>(result)
        assertEquals(latest.id, sut.records.updated.single().first)
        assertEquals(11_000L, sut.records.updatedCars.single().second)
    }

    @Test
    fun `edit latest record below car mileage returns MileageMismatch and persists nothing`() = runTest(dispatcher) {
        val older = record(atMileage = 8_000L, date = 0L)
        val latest = record(atMileage = 10_000L, date = 1L)
        val sut = Sut(listOf(older, latest))

        val result = sut.edit(latest, atMileage = 9_000L)

        val error = assertFailure<SaveRecordError.MileageMismatch>(result)
        assertEquals(9_000L, error.recordMileage)
        assertEquals(10_000L, error.carMileage)
        sut.assertNothingPersisted()
    }

    @Test
    fun `edit latest record below car mileage with KEEP_CAR saves record and keeps car`() = runTest(dispatcher) {
        val latest = record(atMileage = 8_000L, date = 1L)
        val sut = Sut(listOf(latest))

        val result = sut.edit(latest, atMileage = 9_000L, resolution = MileageResolution.KEEP_CAR)

        assertIs<DataResult.Success<*>>(result)
        assertEquals(1, sut.records.updated.size)
        assertEquals(0, sut.records.updatedCars.size)
    }

    @Test
    fun `edit latest record below car mileage with UPDATE_CAR lowers car mileage`() = runTest(dispatcher) {
        val older = record(atMileage = 8_000L, date = 0L)
        val latest = record(atMileage = 10_000L, date = 1L)
        val sut = Sut(listOf(older, latest))

        val result = sut.edit(latest, atMileage = 9_000L, resolution = MileageResolution.UPDATE_CAR)

        assertIs<DataResult.Success<*>>(result)
        assertEquals(9_000L, sut.records.updatedCars.single().second)
    }

    @Test
    fun `edit non-latest record never shows mismatch and never touches car`() = runTest(dispatcher) {
        val sut = Sut(listOf(low, high))

        // 6_000 is below car mileage (10_000), but `low` isn't the latest record
        val result = sut.edit(low, atMileage = 6_000L)

        assertIs<DataResult.Success<*>>(result)
        assertEquals(1, sut.records.updated.size)
        assertEquals(0, sut.records.updatedCars.size)
    }

    @Test
    fun `edit non-latest record above upper neighbour returns MileageOutOfSequence`() = runTest(dispatcher) {
        val sut = Sut(listOf(low, high))

        val result = sut.edit(low, atMileage = 11_000L)

        val error = assertFailure<SaveRecordError.MileageOutOfSequence>(result)
        assertEquals(null, error.min) // nothing below `low` once itself is excluded
        assertEquals(10_000L, error.max)
        sut.assertNothingPersisted()
    }

    @Test
    fun `edit doesn't compare record with its own previous version`() = runTest(dispatcher) {
        val only = record(atMileage = 10_000L, date = 1L)
        val sut = Sut(listOf(only))

        // moving the date forward and lowering mileage: without excluding itself,
        // the old version (10_000 @ day 1) would become a lower bound and fail
        val result = sut.edit(only, atMileage = 9_500L, date = 2L, resolution = MileageResolution.KEEP_CAR)

        assertIs<DataResult.Success<*>>(result)
        assertEquals(only.id, sut.records.updated.single().first)
    }

    @Test
    fun `edit that moves record by date is validated against new neighbours`() = runTest(dispatcher) {
        val mid = record(atMileage = 8_000L, date = 5L)
        val sut = Sut(listOf(low, mid, high))

        // `low` (5_000) moved to day 7, between `mid` (8_000) and `high` (10_000)
        val result = sut.edit(low, atMileage = 5_000L, date = 7L)

        val error = assertFailure<SaveRecordError.MileageOutOfSequence>(result)
        assertEquals(8_000L, error.min)
        assertEquals(10_000L, error.max)
        sut.assertNothingPersisted()
    }

    // ---------- Parsing ----------

    @Test
    fun `invalid input returns ParseFailure and persists nothing`() = runTest(dispatcher) {
        val sut = Sut()

        val result = sut.useCase(
            mode = EditorMode.Create(defaultCar.id),
            input = input(atMileage = "abc", date = 1L),
            carId = defaultCar.id
        )

        val error = assertFailure<SaveRecordError.ParseFailure>(result)
        assertTrue(error.errors.isNotEmpty())
        sut.assertNothingPersisted()
    }

    private inline fun <reified E : SaveRecordError> assertFailure(result: EmptyResult<SaveRecordError>): E {
        assertIs<DataResult.Failure<*>>(result)
        return assertIs<E>(result.error)
    }

    private companion object {
        val defaultCar = aCar(mileage = 10_000L)
        val defaultCategory = aCategory()

        fun record(atMileage: Long, date: Long) = aRecord(
            carId = defaultCar.id,
            categoryId = defaultCategory.id,
            atMileage = atMileage,
            date = date
        )

        val defaultRecord = record(atMileage = 10_000L, date = 0L)

        // two records to put new/edited ones between
        val low = record(atMileage = 5_000L, date = 0L)
        val high = record(atMileage = 10_000L, date = 10L)

        fun input(atMileage: String, date: Long) = ServiceRecordFormInput(
            carId = defaultCar.id,
            atMileage = atMileage,
            categoryId = defaultCategory.id,
            date = date
        )
    }
}
