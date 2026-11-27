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
import ru.haykor.carmanager.core.data.model.CarRecordSearchRow
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

    @Query(
        """
        SELECT
          c.id AS car_id, c.brand AS car_brand, c.model AS car_model, c.year AS car_year,
          c.note AS car_note, c.vin AS car_vin, c.plateCountry AS car_plateCountry,
          c.plate AS car_plate, c.mileage AS car_mileage, c.photoUri AS car_photoUri,
          c.createdAt AS car_createdAt, c.updatedAt AS car_updatedAt,
          r.id AS record_id, r.carId AS record_carId, r.atMileage AS record_atMileage,
          r.price AS record_price, r.categoryId AS record_categoryId, r.note AS record_note,
          r.photoUri AS record_photoUri, r.date AS record_date,
          r.createdAt AS record_createdAt, r.updatedAt AS record_updatedAt,
          sc.name AS record_categoryName
        FROM cars c
        LEFT JOIN service_records r
          ON r.carId = c.id
          AND (
            r.rowid IN (SELECT rowid FROM records_fts WHERE records_fts MATCH :t1)
            OR (:t2 IS NOT NULL AND r.rowid IN (SELECT rowid FROM records_fts WHERE records_fts MATCH :t2))
            OR (:t3 IS NOT NULL AND r.rowid IN (SELECT rowid FROM records_fts WHERE records_fts MATCH :t3))
            OR (:t4 IS NOT NULL AND r.rowid IN (SELECT rowid FROM records_fts WHERE records_fts MATCH :t4))
          )
        LEFT JOIN service_categories sc ON sc.id = r.categoryId
        WHERE (
          c.rowid IN (SELECT rowid FROM cars_fts WHERE cars_fts MATCH :t1)
          OR EXISTS (
            SELECT 1 FROM service_records r1
            WHERE r1.carId = c.id
              AND r1.rowid IN (SELECT rowid FROM records_fts WHERE records_fts MATCH :t1)
          )
        )
        AND (:t2 IS NULL OR (
          c.rowid IN (SELECT rowid FROM cars_fts WHERE cars_fts MATCH :t2)
          OR EXISTS (
            SELECT 1 FROM service_records r2
            WHERE r2.carId = c.id
              AND r2.rowid IN (SELECT rowid FROM records_fts WHERE records_fts MATCH :t2)
          )
        ))
        AND (:t3 IS NULL OR (
          c.rowid IN (SELECT rowid FROM cars_fts WHERE cars_fts MATCH :t3)
          OR EXISTS (
            SELECT 1 FROM service_records r3
            WHERE r3.carId = c.id
              AND r3.rowid IN (SELECT rowid FROM records_fts WHERE records_fts MATCH :t3)
          )
        ))
        AND (:t4 IS NULL OR (
          c.rowid IN (SELECT rowid FROM cars_fts WHERE cars_fts MATCH :t4)
          OR EXISTS (
            SELECT 1 FROM service_records r4
            WHERE r4.carId = c.id
              AND r4.rowid IN (SELECT rowid FROM records_fts WHERE records_fts MATCH :t4)
          )
        ))
        ORDER BY c.createdAt, c.id, r.date DESC
        """
    )
    fun searchCarsAndRecords(t1: String, t2: String?, t3: String?, t4: String?): Flow<List<CarRecordSearchRow>>

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
