package ru.haykor.carmanager.core.data.local.dao

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.haykor.carmanager.core.data.local.AppDatabase
import ru.haykor.carmanager.core.data.local.BundledSqliteNative
import ru.haykor.carmanager.core.data.local.entity.car.CarEntity
import ru.haykor.carmanager.core.data.local.entity.service.ServiceCategoryEntity
import ru.haykor.carmanager.core.data.local.entity.service.ServiceRecordEntity
import ru.haykor.carmanager.core.data.model.ServiceRecordUpdate
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceCategoryId
import ru.haykor.carmanager.core.domain.ServiceRecordId

class CarDaoSearchRecordsTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: CarDao
    private lateinit var recordDao: ServiceRecordDao
    private val categoryId = ServiceCategoryId.random()

    @Before
    fun setUp() {
        BundledSqliteNative.install()
        database = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        dao = database.carDao()
        recordDao = database.serviceRecordDao()
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
    fun oneWordQueryDoesNotAttachUnmatchedRecords() = runBlocking {
        val camry = car(brand = "Toyota", model = "Camry")
        dao.insert(camry)
        record(camry.id, note = "Engine camry parts", date = 10)
        record(camry.id, note = "Dealer service", date = 20)

        val rows = dao.searchCarsAndRecords("cam*", null, null, null).first()

        assertEquals(listOf("Engine camry parts"), rows.map { it.note })
    }

    @Test
    fun recordMatchReturnsCarWithOnlyMatchedRecords() = runBlocking {
        val civic = car(brand = "Honda", model = "Civic")
        dao.insert(civic)
        record(civic.id, note = "oil change", date = 10)
        record(civic.id, note = "brake pads", date = 20)

        val rows = dao.searchCarsAndRecords("oil*", null, null, null).first()

        assertEquals(listOf(civic.id), rows.map { it.car.id })
        assertEquals(listOf("oil change"), rows.map { it.note })
    }

    @Test
    fun carMatchWithNoMatchingRecordsReturnsCarWithEmptyRecords() = runBlocking {
        val camry = car(brand = "Toyota", model = "Camry")
        dao.insert(camry)
        record(camry.id, note = "dealer service", date = 10)

        val row = dao.searchCarsAndRecords("cam*", null, null, null).first().single()

        assertEquals(camry.id, row.car.id)
        assertNull(row.recordId)
        assertNull(row.note)
        assertNull(row.categoryName)
    }

    @Test
    fun allTokensMustMatchCarOrItsRecords() = runBlocking {
        val camry = car(brand = "Toyota", model = "Camry")
        dao.insert(camry)
        record(camry.id, note = "oil change", date = 10)

        assertTrue(dao.searchCarsAndRecords("cam*", "brak*", null, null).first().isEmpty())
    }

    @Test
    fun tokensMayMatchDifferentRecordsOfSameCar() = runBlocking {
        val camry = car(brand = "Toyota", model = "Camry")
        dao.insert(camry)
        record(camry.id, note = "camry parts", date = 10)
        record(camry.id, note = "oil change", date = 20)

        val rows = dao.searchCarsAndRecords("cam*", "oil*", null, null).first()

        assertEquals(listOf(camry.id, camry.id), rows.map { it.car.id })
        assertEquals(listOf(20L, 10L), rows.map { it.date })
    }

    @Test
    fun recordsMatchingAnyTokenAreReturnedNewestFirst() = runBlocking {
        val honda = car(brand = "Honda", model = "Accord")
        dao.insert(honda)
        record(honda.id, note = "camry parts", date = 10)
        record(honda.id, note = "oil change", date = 20)

        val rows = dao.searchCarsAndRecords("cam*", "oil*", null, null).first()

        assertEquals(listOf("oil change", "camry parts"), rows.map { it.note })
        assertEquals(listOf(20L, 10L), rows.map { it.date })
    }

    @Test
    fun recordFtsFollowsInsertUpdateAndDelete() = runBlocking {
        val corolla = car(brand = "Toyota", model = "Corolla")
        dao.insert(corolla)
        val oilChange = record(corolla.id, note = "oil change", date = 10)

        assertEquals(
            listOf(oilChange.id),
            dao.searchCarsAndRecords("oil*", null, null, null).first().map { it.recordId }
        )

        recordDao.update(
            ServiceRecordUpdate(
                id = oilChange.id,
                carId = oilChange.carId,
                atMileage = oilChange.atMileage,
                price = oilChange.price,
                categoryId = oilChange.categoryId,
                note = "brake pads",
                photoUri = oilChange.photoUri,
                date = oilChange.date,
                updatedAt = oilChange.updatedAt + 1
            )
        )

        assertTrue(dao.searchCarsAndRecords("oil*", null, null, null).first().isEmpty())
        assertEquals(
            listOf(oilChange.id),
            dao.searchCarsAndRecords("brak*", null, null, null).first().map { it.recordId }
        )

        recordDao.delete(oilChange.id)

        assertTrue(dao.searchCarsAndRecords("brak*", null, null, null).first().isEmpty())
    }

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
        recordDao.insert(entity)
        return entity
    }
}
