package ru.haykor.carmanager.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import ru.haykor.carmanager.core.data.local.entity.car.CarEntity
import ru.haykor.carmanager.core.data.local.entity.car.CarWithRecords
import ru.haykor.carmanager.core.data.model.CarUpdate
import ru.haykor.carmanager.core.domain.CarId

@Dao
interface CarDao {
    @Query("SELECT * FROM cars WHERE id = :id")
    fun observeCar(id: CarId): Flow<CarEntity?>

    @Query("SELECT * FROM cars ORDER BY createdAt")
    fun observeCars(): Flow<List<CarEntity>>

    @Transaction
    @Query("SELECT * FROM cars WHERE id = :carId")
    fun observeCarWithRecords(carId: CarId): Flow<CarWithRecords?>

    @Transaction
    @Query("SELECT * FROM cars")
    fun observeAllWithRecords(): Flow<List<CarWithRecords>>

    @Query(
        """
        SELECT cars.*
        FROM cars
        JOIN cars_fts ON cars.rowid = cars_fts.rowid
        WHERE cars_fts MATCH :sanitizedQuery
        """
    )
    fun searchCars(sanitizedQuery: String): Flow<List<CarEntity>>

    @Insert
    suspend fun insert(entity: CarEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<CarEntity>)

    @Query("SELECT EXISTS(SELECT 1 FROM cars LIMIT 1)")
    suspend fun hasAny(): Boolean

    @Update(entity = CarEntity::class)
    suspend fun update(update: CarUpdate)

    @Query("DELETE FROM cars WHERE id = :id")
    suspend fun delete(id: CarId)
}
