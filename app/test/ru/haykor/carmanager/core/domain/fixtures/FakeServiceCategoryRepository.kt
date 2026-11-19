package ru.haykor.carmanager.core.domain.fixtures

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import ru.haykor.carmanager.core.domain.model.ServiceCategory
import ru.haykor.carmanager.core.domain.repository.ServiceCategoryRepository

internal class FakeServiceCategoryRepository(private val categories: List<ServiceCategory>) :
    ServiceCategoryRepository {

    override fun observeAll(): Flow<List<ServiceCategory>> = flowOf(categories)
}
