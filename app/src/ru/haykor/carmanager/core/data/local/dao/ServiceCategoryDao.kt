package ru.haykor.carmanager.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow
import ru.haykor.carmanager.core.data.local.entity.service.ServiceCategoryEntity

@Dao
interface ServiceCategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<ServiceCategoryEntity>)

    @Query("SELECT * FROM service_categories ORDER BY name")
    fun observeAll(): Flow<List<ServiceCategoryEntity>>
}
