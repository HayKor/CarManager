package ru.haykor.carmanager.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import ru.haykor.carmanager.core.data.local.entity.service.ServiceRecordEntity
import ru.haykor.carmanager.core.data.local.entity.service.ServiceRecordUpdate
import ru.haykor.carmanager.core.data.local.entity.service.ServiceRecordWithCategory
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceRecordId

@Dao
interface ServiceRecordDao {

    @Insert
    suspend fun insert(record: ServiceRecordEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(records: List<ServiceRecordEntity>)

    @Update(entity = ServiceRecordEntity::class)
    suspend fun update(update: ServiceRecordUpdate)

    @Query("DELETE FROM service_records WHERE id = :id")
    suspend fun delete(id: ServiceRecordId)

    @Query("SELECT * FROM service_records WHERE id = :id")
    fun observeWithCategory(id: ServiceRecordId): Flow<ServiceRecordWithCategory?>

    @Query("SELECT * FROM service_records WHERE carId = :carId ORDER BY date DESC")
    fun observeByCar(carId: CarId): Flow<List<ServiceRecordWithCategory>>
}
