package ru.haykor.carmanager

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import ru.haykor.carmanager.core.common.StartupTask
import ru.haykor.carmanager.core.data.local.dao.ServiceCategoryDao
import ru.haykor.carmanager.core.data.local.entity.service.ServiceCategoryEntity
import ru.haykor.carmanager.core.domain.ServiceCategoryId

@Inject
@ContributesIntoSet(AppScope::class)
class ReleaseDatabaseSeeder(private val categoryDao: ServiceCategoryDao) : StartupTask {

    override suspend fun run() {
        if (categoryDao.observeAll().first().isNotEmpty()) return

        val categories = listOf(
            ServiceCategoryEntity(id = ServiceCategoryId.random(), name = "Scheduled Maintenance"),
            ServiceCategoryEntity(id = ServiceCategoryId.random(), name = "Oil Change"),
            ServiceCategoryEntity(id = ServiceCategoryId.random(), name = "Tire Replacement"),
            ServiceCategoryEntity(id = ServiceCategoryId.random(), name = "Brake Service"),
            ServiceCategoryEntity(id = ServiceCategoryId.random(), name = "Repair")
        )

        categoryDao.insertAll(categories)
    }
}
