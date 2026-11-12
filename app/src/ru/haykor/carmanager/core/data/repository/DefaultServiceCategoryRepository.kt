package ru.haykor.carmanager.core.data.repository

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.haykor.carmanager.core.data.local.dao.ServiceCategoryDao
import ru.haykor.carmanager.core.data.mapper.toDomain
import ru.haykor.carmanager.core.domain.model.ServiceCategory
import ru.haykor.carmanager.core.domain.repository.ServiceCategoryRepository

@Inject
@ContributesBinding(AppScope::class)
class DefaultServiceCategoryRepository(private val categoryDao: ServiceCategoryDao) : ServiceCategoryRepository {

    override fun observeAll(): Flow<List<ServiceCategory>> = categoryDao.observeAll().map { categoryEntities ->
        categoryEntities.map { it.toDomain() }
    }
}
