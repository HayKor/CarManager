package ru.haykor.carmanager.core.domain.model

import kotlinx.serialization.Serializable
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceRecordId

@Serializable
sealed interface EditorMode {
    @Serializable
    data class Create(val carId: CarId) : EditorMode

    @Serializable
    data class Edit(val recordId: ServiceRecordId) : EditorMode
}
