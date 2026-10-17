package ru.haykor.carmanager.feature.car.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import ru.haykor.carmanager.feature.car.domain.CarId

@Entity(tableName = "cars")
data class CarEntity(
    @PrimaryKey val id: CarId,
    val brand: String,
    val model: String,
    val year: Int?,
    val vin: String?,
    val plates: String?,
    val mileage: Long,
    val photoUri: String?,
    val createdAt: Long,
    val updatedAt: Long
)
