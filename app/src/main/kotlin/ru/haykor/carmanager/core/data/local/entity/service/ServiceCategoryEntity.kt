package ru.haykor.carmanager.core.data.local.entity.service

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import ru.haykor.carmanager.core.domain.ServiceCategoryId

@Entity(tableName = "service_categories")
data class ServiceCategoryEntity(@PrimaryKey val id: ServiceCategoryId, val name: String)
