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
        // Strip characters that break SQLite FTS tokenizers
        val cleaned = query.replace(Regex("[^a-zA-Z0-9가-힣äöüÄÖÜß ]"), " ").trim()
        if (cleaned.isEmpty()) return ""

        // Append '*' to each token so "cor" matches "Corolla" or "Corvette"
        return cleaned
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(separator = " ") { token -> "$token*" }
    }
}
