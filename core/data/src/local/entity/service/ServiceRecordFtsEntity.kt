package ru.haykor.carmanager.core.data.local.entity.service

import androidx.room3.Entity
import androidx.room3.Fts5

@Fts5(contentEntity = ServiceRecordEntity::class)
@Entity(tableName = "records_fts")
data class ServiceRecordFtsEntity(val note: String?)
