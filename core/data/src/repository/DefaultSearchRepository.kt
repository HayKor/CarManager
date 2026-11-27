package ru.haykor.carmanager.core.data.repository

import androidx.sqlite.SQLiteException
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import ru.haykor.carmanager.core.data.local.dao.CarDao
import ru.haykor.carmanager.core.data.mapper.toDomainGroup
import ru.haykor.carmanager.core.data.mapper.toDomainGroups
import ru.haykor.carmanager.core.domain.error.DataError
import ru.haykor.carmanager.core.domain.model.CarSearchResult
import ru.haykor.carmanager.core.domain.repository.SearchRepository
import ru.haykor.carmanager.core.result.DataResult

@Inject
@ContributesBinding(AppScope::class)
class DefaultSearchRepository(private val carDao: CarDao) : SearchRepository {

    override fun search(words: List<String>): Flow<DataResult<List<CarSearchResult>, DataError>> {
        val groups: Flow<List<CarSearchResult>> = if (words.isEmpty()) {
            carDao.observeAllWithRecords().map { cars ->
                cars.map { it.toDomainGroup() }
                    .sortedWith(compareBy({ it.car.createdAt }, { it.car.id.value }))
            }
        } else {
            carDao.searchCarsAndRecords(
                t1 = words[0] + "*",
                t2 = words.getOrNull(1)?.plus("*"),
                t3 = words.getOrNull(2)?.plus("*"),
                t4 = words.getOrNull(3)?.plus("*")
            ).map { rows -> rows.toDomainGroups() }
        }
        return groups
            .map<List<CarSearchResult>, DataResult<List<CarSearchResult>, DataError>> { DataResult.Success(it) }
            .catch { throwable ->
                if (throwable is SQLiteException) {
                    emit(DataResult.Failure(DataError.Local.Unknown))
                } else {
                    throw throwable
                }
            }
    }
}
