package ru.haykor.carmanager.feature.car.data.local.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.feature.car.data.local.entity.CarEntity
import ru.haykor.carmanager.feature.car.domain.model.Car

@Dao
interface CarDao {
    @Query("SELECT * FROM cars WHERE id = :id")
    fun observeCar(id: CarId): Flow<CarEntity?>

    @Query("SELECT * FROM cars ORDER BY updatedAt")
    fun observeCars(): Flow<List<CarEntity>>

    @Upsert
    suspend fun upsert(entity: CarEntity)

    @Query("DELETE FROM cars WHERE id = :id")
    suspend fun delete(id: CarId)
}
