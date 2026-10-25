package ru.haykor.carmanager.core.data.local.entity.car

import androidx.room3.Entity
import androidx.room3.Fts5

@Fts5(contentEntity = CarEntity::class)
@Entity(tableName = "car_fts")
data class CarFtsEntity(val brand: String, val model: String, val plate: String?, val note: String?)
