package ru.haykor.carmanager.core.domain.usecase

import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlinx.coroutines.flow.first
import ru.haykor.carmanager.core.domain.model.Car
import ru.haykor.carmanager.core.domain.model.ServiceCategory
import ru.haykor.carmanager.core.domain.model.ServiceRecord
import ru.haykor.carmanager.core.domain.model.ServiceRecordFormInput
import ru.haykor.carmanager.core.domain.repository.CarRepository
import ru.haykor.carmanager.core.domain.repository.ServiceCategoryRepository
import ru.haykor.carmanager.core.domain.repository.ServiceRecordRepository
import ru.haykor.carmanager.feature.serviceEditor.component.EditorMode

data class ServiceEditorData(val input: ServiceRecordFormInput, val car: Car, val categories: List<ServiceCategory>)

@Inject
class GetInitialServiceEditorDataUseCase(
    private val cars: CarRepository,
    private val records: ServiceRecordRepository,
    private val categories: ServiceCategoryRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(mode: EditorMode): ServiceEditorData? {
        val allCategories = categories.observeAll().first()

        return when (mode) {
            is EditorMode.Create -> {
                val car = cars.observeCar(mode.carId).first() ?: return null
                ServiceEditorData(
                    input = ServiceRecordFormInput(
                        carId = car.id,
                        categoryId = allCategories.first().id,
                        date = clock.now().toEpochMilliseconds()
                    ),
                    car = car,
                    categories = allCategories
                )
            }

            is EditorMode.Edit -> {
                val recordDetails = records.observeById(mode.recordId).first() ?: return null
                val car = cars.observeCar(recordDetails.record.carId).first() ?: return null
                ServiceEditorData(
                    input = recordDetails.record.toFormInput(),
                    car = car,
                    categories = allCategories
                )
            }
        }
    }

    private fun ServiceRecord.toFormInput() = ServiceRecordFormInput(
        carId = carId,
        categoryId = categoryId,
        atMileage = atMileage.toString(),
        price = price?.toString() ?: "",
        note = note.orEmpty(),
        photoUri = photoUri,
        date = date
    )
}
