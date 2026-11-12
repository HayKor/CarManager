package ru.haykor.carmanager.core.data.local

import androidx.room3.Database
import androidx.room3.RoomDatabase
import ru.haykor.carmanager.core.data.local.dao.CarDao
import ru.haykor.carmanager.core.data.local.dao.ServiceCategoryDao
import ru.haykor.carmanager.core.data.local.dao.ServiceRecordDao
import ru.haykor.carmanager.core.data.local.entity.car.CarEntity
import ru.haykor.carmanager.core.data.local.entity.car.CarFtsEntity
import ru.haykor.carmanager.core.data.local.entity.service.ServiceCategoryEntity
import ru.haykor.carmanager.core.data.local.entity.service.ServiceRecordEntity

@Database(
    entities = [CarEntity::class, CarFtsEntity::class, ServiceRecordEntity::class, ServiceCategoryEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun carDao(): CarDao
    abstract fun serviceRecordDao(): ServiceRecordDao
    abstract fun serviceCategoryDao(): ServiceCategoryDao
}
