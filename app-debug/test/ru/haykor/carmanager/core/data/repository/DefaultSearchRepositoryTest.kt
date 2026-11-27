package ru.haykor.carmanager.core.data.repository

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import ru.haykor.carmanager.core.data.local.AppDatabase
import ru.haykor.carmanager.core.data.local.BundledSqliteNative
import ru.haykor.carmanager.core.data.local.entity.car.CarEntity
import ru.haykor.carmanager.core.data.local.entity.service.ServiceCategoryEntity
import ru.haykor.carmanager.core.data.local.entity.service.ServiceRecordEntity
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceCategoryId
import ru.haykor.carmanager.core.domain.ServiceRecordId
import ru.haykor.carmanager.core.domain.error.DataError
import ru.haykor.carmanager.core.domain.model.CarSearchResult
import ru.haykor.carmanager.core.result.DataResult
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DefaultSearchRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: DefaultSearchRepository
    private val categoryId = ServiceCategoryId.random()

    @Before
    fun setUp() {
        BundledSqliteNative.install()
        database = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        repository = DefaultSearchRepository(database.carDao())
        runBlocking {
            database.serviceCategoryDao()
                .insertAll(listOf(ServiceCategoryEntity(id = categoryId, name = "Maintenance")))
        }
    }

    @After
    fun tearDown() = runBlocking {
        database.close()
    }

    @Test
    fun blankWordsReturnAllHistory() = runBlocking {
        val first = car(brand = "Toyota", model = "Camry", createdAt = 1)
        val second = car(brand = "Honda", model = "Civic", createdAt = 2)
        database.carDao().insert(first)
        database.carDao().insert(second)
        record(first.id, note = "newer", date = 20)
        record(first.id, note = "older", date = 10)

        val groups = repository.search(emptyList()).first().requireSuccess()

        assertEquals(listOf(first.id, second.id), groups.map { it.car.id })
        assertEquals(listOf(20L, 10L), groups[0].records.map { it.record.date })
        assertEquals(listOf("newer", "older"), groups[0].records.map { it.record.note })
        assertEquals(listOf("Maintenance", "Maintenance"), groups[0].records.map { it.category.name })
        assertEquals(emptyList(), groups[1].records)
    }

    @Test
    fun matchedWordsReturnGroupsWithOnlyMatchedRecords() = runBlocking {
        val civic = car(brand = "Honda", model = "Civic")
        database.carDao().insert(civic)
        val oilChange = record(civic.id, note = "oil change", date = 10)
        record(civic.id, note = "brake pads", date = 20)

        val groups = repository.search(listOf("oil")).first().requireSuccess()

        assertEquals(listOf(civic.id), groups.map { it.car.id })
        assertEquals(listOf(oilChange.id), groups.single().records.map { it.record.id })
    }

    @Test
    fun carWithoutMatchingRecordsIsKept() = runBlocking {
        val camry = car(brand = "Toyota", model = "Camry")
        database.carDao().insert(camry)
        record(camry.id, note = "dealer service", date = 10)

        val groups = repository.search(listOf("camry")).first().requireSuccess()

        assertEquals(camry.id, groups.single().car.id)
        assertEquals(emptyList(), groups.single().records)
    }

    private fun DataResult<List<CarSearchResult>, DataError>.requireSuccess(): List<CarSearchResult> =
        assertIs<DataResult.Success<List<CarSearchResult>>>(this).data

    private fun car(id: CarId = CarId.random(), brand: String, model: String, createdAt: Long = 1) = CarEntity(
        id = id,
        brand = brand,
        model = model,
        year = 2020,
        note = null,
        vin = null,
        plateCountry = null,
        plate = null,
        mileage = 12_000,
        photoUri = null,
        createdAt = createdAt,
        updatedAt = createdAt
    )

    private suspend fun record(carId: CarId, note: String?, date: Long, mileage: Long = 1_000): ServiceRecordEntity {
        val entity = ServiceRecordEntity(
            id = ServiceRecordId.random(),
            carId = carId,
            atMileage = mileage,
            price = null,
            categoryId = categoryId,
            note = note,
            photoUri = null,
            date = date,
            createdAt = date,
            updatedAt = date
        )
        database.serviceRecordDao().insert(entity)
        return entity
    }
}
