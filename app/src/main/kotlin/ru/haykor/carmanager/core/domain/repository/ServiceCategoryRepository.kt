package ru.haykor.carmanager.core.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.haykor.carmanager.core.domain.model.ServiceCategory

interface ServiceCategoryRepository {
    fun observeAll(): Flow<List<ServiceCategory>>
}
