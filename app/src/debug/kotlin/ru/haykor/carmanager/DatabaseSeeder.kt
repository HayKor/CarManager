package ru.haykor.carmanager

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import ru.haykor.carmanager.core.StartupTask
import ru.haykor.carmanager.core.data.local.dao.CarDao
import ru.haykor.carmanager.core.data.local.entity.car.CarEntity
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.PlateCountry

@Inject
@ContributesIntoSet(AppScope::class)
class DatabaseSeeder(private val carDao: CarDao, private val clock: Clock) : StartupTask {

    override suspend fun run() {
        if (carDao.hasAny()) return

        val now = clock.now().toEpochMilliseconds()

        val cars = listOf(
            CarEntity(
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
            ),
            CarEntity(
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
            ),
            CarEntity(
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
            ),
            CarEntity(
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
        )

        carDao.insertAll(cars)
    }
}
