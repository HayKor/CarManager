package ru.haykor.carmanager.core.data.local

import androidx.room3.Database
import androidx.room3.RoomDatabase
import ru.haykor.carmanager.core.data.local.dao.CarDao
import ru.haykor.carmanager.core.data.local.entity.car.CarEntity

@Database(
    entities = [CarEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun carDao(): CarDao
}
