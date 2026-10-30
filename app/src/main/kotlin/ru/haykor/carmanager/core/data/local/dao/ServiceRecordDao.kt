package ru.haykor.carmanager.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import ru.haykor.carmanager.core.data.local.entity.service.ServiceRecordEntity
import ru.haykor.carmanager.core.data.local.entity.service.ServiceRecordUpdate
import ru.haykor.carmanager.core.data.local.entity.service.ServiceRecordWithCategory
import ru.haykor.carmanager.core.data.model.MileageBoundsRow
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceRecordId

@Dao
abstract class ServiceRecordDao {

    @Insert
    abstract suspend fun insert(record: ServiceRecordEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertAll(records: List<ServiceRecordEntity>)

    @Update(entity = ServiceRecordEntity::class)
    abstract suspend fun update(update: ServiceRecordUpdate)

    @Query("DELETE FROM service_records WHERE id = :id")
    abstract suspend fun delete(id: ServiceRecordId)

    @Query("UPDATE cars SET mileage = :mileage WHERE id = :carId")
    abstract suspend fun setCarMileage(carId: CarId, mileage: Long): Int

    @Transaction
    open suspend fun insertWithCarMileage(
        record: ServiceRecordEntity,
        newCarMileage: Long? // null = leave the car as is
    ) {
        insert(record)
        if (newCarMileage != null) {
            check(setCarMileage(record.carId, newCarMileage) == 1) { "Car ${record.carId} not found" }
        }
    }

    @Transaction
    open suspend fun updateWithCarMileage(
        recordUpdate: ServiceRecordUpdate,
        newCarMileage: Long? // null = leave the car as is
    ) {
        update(recordUpdate)
        if (newCarMileage != null) {
            check(setCarMileage(recordUpdate.carId, newCarMileage) == 1) { "Car ${recordUpdate.carId} not found" }
        }
    }

    @Query(
        """
    SELECT
      (SELECT MAX(atMileage) FROM service_records
        WHERE carId = :carId AND date < :date
          AND (:excludeId IS NULL OR id != :excludeId)) AS lower,
      (SELECT MIN(atMileage) FROM service_records
        WHERE carId = :carId AND date > :date
          AND (:excludeId IS NULL OR id != :excludeId)) AS upper
    """
    )
    abstract suspend fun mileageBounds(carId: CarId, date: Long, excludeId: ServiceRecordId?): MileageBoundsRow

    // excludeId нужен для Edit: запись не должна сравниваться сама с собой
    @Query(
        """
    SELECT MAX(date) FROM service_records
    WHERE carId = :carId AND (:excludeId IS NULL OR id != :excludeId)
    """
    )
    abstract suspend fun latestDate(carId: CarId, excludeId: ServiceRecordId?): Long?

    @Query("SELECT * FROM service_records WHERE id = :id")
    abstract fun observeWithCategory(id: ServiceRecordId): Flow<ServiceRecordWithCategory?>

    @Query("SELECT * FROM service_records WHERE carId = :carId ORDER BY date DESC")
    abstract fun observeByCar(carId: CarId): Flow<List<ServiceRecordWithCategory>>
}
