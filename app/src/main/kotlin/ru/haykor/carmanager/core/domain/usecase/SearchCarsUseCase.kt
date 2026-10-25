package ru.haykor.carmanager.core.domain.usecase

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import ru.haykor.carmanager.core.common.ComprehensionDebt
import ru.haykor.carmanager.core.domain.model.Car
import ru.haykor.carmanager.core.domain.repository.CarRepository

@Inject
class SearchCarsUseCase(private val carRepository: CarRepository) {

    operator fun invoke(query: String): Flow<List<Car>> {
        val sanitizedQuery = sanitizeQuery(query)
        return if (sanitizedQuery.isEmpty()) {
            carRepository.observeCars()
        } else {
            carRepository.searchCars(sanitizedQuery)
        }
    }

    @ComprehensionDebt
    private fun sanitizeQuery(query: String): String {
        // Оставляет любые буквы (\p{L}), цифры (\p{N}) и пробелы
        val cleaned = query.replace(Regex("[^\\p{L}\\p{N} ]"), " ").trim()
        if (cleaned.isEmpty()) return ""

        return cleaned
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(separator = " ") { token -> "$token*" }
    }
}
