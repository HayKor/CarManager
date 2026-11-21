package ru.haykor.carmanager.core.data.local.dao

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.haykor.carmanager.core.data.local.AppDatabase
import ru.haykor.carmanager.core.data.local.BundledSqliteNative
import ru.haykor.carmanager.core.data.local.entity.car.CarEntity
import ru.haykor.carmanager.core.data.model.CarUpdate
import ru.haykor.carmanager.core.domain.CarId

class CarDaoSearchTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: CarDao

    @Before
    fun setUp() {
        BundledSqliteNative.install()
        database = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        dao = database.carDao()
    }

    @After
    fun tearDown() = runBlocking {
        database.close()
    }

    @Test
    fun searchFindsPrefixInBrandModelPlateAndNote() = runBlocking {
        val car = car(
            brand = "Toyota",
            model = "Corolla",
            plate = "A123BC",
            note = "Family vehicle"
        )
        dao.insert(car)

        assertEquals(listOf(car), dao.searchCars("Toy*").first())
        assertEquals(listOf(car), dao.searchCars("Cor*").first())
        assertEquals(listOf(car), dao.searchCars("A123*").first())
        assertEquals(listOf(car), dao.searchCars("Fam*").first())
    }

    @Test
    fun multipleSearchTermsMustMatchTheSameCar() = runBlocking {
        val matching = car(brand = "Toyota", model = "Corolla", id = CarId.random())
        val nonMatching = car(brand = "Honda", model = "Civic", id = CarId.random())
        dao.insert(matching)
        dao.insert(nonMatching)

        assertEquals(listOf(matching), dao.searchCars("Toy* Cor*").first())
        assertTrue(dao.searchCars("Toy* Civic*").first().isEmpty())
    }

    @Test
    fun externalContentIndexFollowsUpdatesAndDeletes() = runBlocking {
        val original = car(brand = "Toyota", model = "Corolla", note = "old note")
        dao.insert(original)

        assertEquals(listOf(original), dao.searchCars("old*").first())

        dao.update(
            CarUpdate(
                id = original.id,
                brand = "Honda",
                model = "Accord",
                year = original.year,
                note = "new note",
                vin = original.vin,
                plateCountry = original.plateCountry,
                plate = original.plate,
                mileage = original.mileage,
                photoUri = original.photoUri,
                updatedAt = original.updatedAt + 1
            )
        )

        assertTrue(dao.searchCars("old*").first().isEmpty())
        assertEquals("Honda", dao.searchCars("Hon*").first().single().brand)
        assertEquals("new note", dao.searchCars("new*").first().single().note)

        dao.delete(original.id)

        assertTrue(dao.searchCars("Hon*").first().isEmpty())
    }

    private fun car(
        id: CarId = CarId.random(),
        brand: String,
        model: String,
        plate: String? = null,
        note: String? = null
    ) = CarEntity(
        id = id,
        brand = brand,
        model = model,
        year = 2020,
        note = note,
        vin = null,
        plateCountry = null,
        plate = plate,
        mileage = 12_000,
        photoUri = null,
        createdAt = 1,
        updatedAt = 1
    )
}
