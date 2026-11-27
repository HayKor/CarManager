package ru.haykor.carmanager.core.domain.usecase

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import ru.haykor.carmanager.core.domain.model.Car
import ru.haykor.carmanager.core.domain.repository.CarRepository
import ru.haykor.carmanager.core.domain.search.SearchQuery

@Inject
class SearchCarsUseCase(private val carRepository: CarRepository) {

    operator fun invoke(query: String): Flow<List<Car>> {
        val words = SearchQuery.sanitizeWords(query)
        return if (words.isEmpty()) {
            carRepository.observeCars()
        } else {
            carRepository.searchCars(words.joinToString(separator = " ") { token -> "$token*" })
        }
    }
}
