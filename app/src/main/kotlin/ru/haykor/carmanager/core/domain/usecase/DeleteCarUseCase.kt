package ru.haykor.carmanager.core.domain.usecase

import dev.zacsweers.metro.Inject
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.error.DataError
import ru.haykor.carmanager.core.domain.repository.CarRepository
import ru.haykor.carmanager.core.result.EmptyResult

@Inject
class DeleteCarUseCase(private val carRepository: CarRepository) {
    suspend operator fun invoke(id: CarId): EmptyResult<DataError> {
        // TODO: add deleting car's services when we add them
        return carRepository.delete(id)
    }
}
