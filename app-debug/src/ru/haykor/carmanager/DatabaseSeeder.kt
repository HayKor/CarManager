package ru.haykor.carmanager

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import ru.haykor.carmanager.core.StartupTask
import ru.haykor.carmanager.core.data.local.dao.CarDao
import ru.haykor.carmanager.core.data.local.dao.ServiceCategoryDao
import ru.haykor.carmanager.core.data.local.dao.ServiceRecordDao
import ru.haykor.carmanager.core.data.local.entity.car.CarEntity
import ru.haykor.carmanager.core.data.local.entity.service.ServiceCategoryEntity
import ru.haykor.carmanager.core.data.local.entity.service.ServiceRecordEntity
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.PlateCountry
import ru.haykor.carmanager.core.domain.ServiceCategoryId
import ru.haykor.carmanager.core.domain.ServiceRecordId
import kotlin.time.Clock

@Inject
@ContributesIntoSet(AppScope::class)
class DatabaseSeeder(
    private val carDao: CarDao,
    private val recordDao: ServiceRecordDao,
    private val categoryDao: ServiceCategoryDao,
    private val clock: Clock
) : StartupTask {

    override suspend fun run() {
        if (carDao.hasAny()) return

        val now = clock.now().toEpochMilliseconds()
        val dayInMillis = 86_400_000L

        val maintenanceCat = ServiceCategoryEntity(id = ServiceCategoryId.random(), name = "Scheduled Maintenance")
        val oilChangeCat = ServiceCategoryEntity(id = ServiceCategoryId.random(), name = "Oil Change")
        val tireReplacementCat = ServiceCategoryEntity(id = ServiceCategoryId.random(), name = "Tire Replacement")
        val brakeServiceCat = ServiceCategoryEntity(id = ServiceCategoryId.random(), name = "Brake Service")
        val repairCat = ServiceCategoryEntity(id = ServiceCategoryId.random(), name = "Repair")

        val categories = listOf(
            maintenanceCat,
            oilChangeCat,
            tireReplacementCat,
            brakeServiceCat,
            repairCat
        )

        val nissan = CarEntity(
            id = CarId.random(),
            brand = "Nissan",
            model = "Qashqai",
            year = 2010,
            note = "My family car",
            vin = null,
            plateCountry = PlateCountry.RU,
            plate = "С123ОМ77",
            mileage = 111_123,
            photoUri = null,
            createdAt = now,
            updatedAt = now
        )

        val lada = CarEntity(
            id = CarId.random(),
            brand = "LADA",
            model = "Vesta",
            year = 2018,
            note = "Daily driver",
            vin = "XTA218000J0123456",
            plateCountry = PlateCountry.RU,
            plate = "А777АА199",
            mileage = 75_400,
            photoUri = null,
            createdAt = now,
            updatedAt = now
        )

        val toyota = CarEntity(
            id = CarId.random(),
            brand = "Toyota",
            model = "Camry",
            year = 2021,
            note = "Company car",
            vin = null,
            plateCountry = PlateCountry.RU,
            plate = "К543ХТ799",
            mileage = 42_000,
            photoUri = null,
            createdAt = now,
            updatedAt = now
        )

        val bmw = CarEntity(
            id = CarId.random(),
            brand = "BMW",
            model = "X5",
            year = 2015,
            note = "Project car",
            vin = null,
            plateCountry = null,
            plate = null,
            mileage = 180_000,
            photoUri = null,
            createdAt = now,
            updatedAt = now
        )

        val cars = listOf(nissan, lada, toyota, bmw)

        val records = listOf(
            // Nissan records
            ServiceRecordEntity(
                id = ServiceRecordId.random(),
                carId = nissan.id,
                atMileage = 100_000,
                price = 5_000,
                categoryId = oilChangeCat.id,
                note = "Engine oil & filter change",
                photoUri = null,
                date = now - 180 * dayInMillis,
                createdAt = now,
                updatedAt = now
            ),
            ServiceRecordEntity(
                id = ServiceRecordId.random(),
                carId = nissan.id,
                atMileage = 105_000,
                price = 12_000,
                categoryId = maintenanceCat.id,
                note = "105k km major service",
                photoUri = null,
                date = now - 90 * dayInMillis,
                createdAt = now,
                updatedAt = now
            ),
            ServiceRecordEntity(
                id = ServiceRecordId.random(),
                carId = nissan.id,
                atMileage = 110_000,
                price = 25_000,
                categoryId = tireReplacementCat.id,
                note = "Replaced winter tires",
                photoUri = null,
                date = now - 30 * dayInMillis,
                createdAt = now,
                updatedAt = now
            ),

            // LADA records
            ServiceRecordEntity(
                id = ServiceRecordId.random(),
                carId = lada.id,
                atMileage = 65_000,
                price = 4_000,
                categoryId = oilChangeCat.id,
                note = "Syntium 5W-40 oil change",
                photoUri = null,
                date = now - 200 * dayInMillis,
                createdAt = now,
                updatedAt = now
            ),
            ServiceRecordEntity(
                id = ServiceRecordId.random(),
                carId = lada.id,
                atMileage = 70_000,
                price = 8_500,
                categoryId = brakeServiceCat.id,
                note = "Front brake pads & discs replacement",
                photoUri = null,
                date = now - 100 * dayInMillis,
                createdAt = now,
                updatedAt = now
            ),
            ServiceRecordEntity(
                id = ServiceRecordId.random(),
                carId = lada.id,
                atMileage = 75_000,
                price = 4_500,
                categoryId = oilChangeCat.id,
                note = "Oil & cabin filter replacement",
                photoUri = null,
                date = now - 10 * dayInMillis,
                createdAt = now,
                updatedAt = now
            ),

            // Toyota records
            ServiceRecordEntity(
                id = ServiceRecordId.random(),
                carId = toyota.id,
                atMileage = 20_000,
                price = 15_000,
                categoryId = maintenanceCat.id,
                note = "Official dealer service #2",
                photoUri = null,
                date = now - 365 * dayInMillis,
                createdAt = now,
                updatedAt = now
            ),
            ServiceRecordEntity(
                id = ServiceRecordId.random(),
                carId = toyota.id,
                atMileage = 30_000,
                price = 18_000,
                categoryId = maintenanceCat.id,
                note = "Official dealer service #3",
                photoUri = null,
                date = now - 180 * dayInMillis,
                createdAt = now,
                updatedAt = now
            ),
            ServiceRecordEntity(
                id = ServiceRecordId.random(),
                carId = toyota.id,
                atMileage = 40_000,
                price = 20_000,
                categoryId = maintenanceCat.id,
                note = "Official dealer service #4",
                photoUri = null,
                date = now - 40 * dayInMillis,
                createdAt = now,
                updatedAt = now
            ),

            // BMW records
            ServiceRecordEntity(
                id = ServiceRecordId.random(),
                carId = bmw.id,
                atMileage = 170_000,
                price = 45_000,
                categoryId = repairCat.id,
                note = "Suspension arms replacement",
                photoUri = null,
                date = now - 120 * dayInMillis,
                createdAt = now,
                updatedAt = now
            ),
            ServiceRecordEntity(
                id = ServiceRecordId.random(),
                carId = bmw.id,
                atMileage = 175_000,
                price = 8_000,
                categoryId = oilChangeCat.id,
                note = "Motul 5W-30 oil & filter",
                photoUri = null,
                date = now - 60 * dayInMillis,
                createdAt = now,
                updatedAt = now
            ),
            ServiceRecordEntity(
                id = ServiceRecordId.random(),
                carId = bmw.id,
                atMileage = 178_000,
                price = 35_000,
                categoryId = tireReplacementCat.id,
                note = "New summer performance tires",
                photoUri = null,
                date = now - 20 * dayInMillis,
                createdAt = now,
                updatedAt = now
            )
        )

        categoryDao.insertAll(categories)
        carDao.insertAll(cars)
        recordDao.insertAll(records)
    }
}
