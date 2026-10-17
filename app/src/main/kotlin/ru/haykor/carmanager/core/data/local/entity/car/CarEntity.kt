package ru.haykor.carmanager.core.data.local.entity.car

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.PlateCountry

@Entity(tableName = "cars")
data class CarEntity(
    @PrimaryKey val id: CarId,
    val brand: String,
    val model: String,
    val year: Int?,
    val vin: String?,
    val plateCountry: PlateCountry?,
    val plate: String?,
    val mileage: Long,
    val photoUri: String?,
    val createdAt: Long,
    val updatedAt: Long
)
