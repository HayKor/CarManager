package ru.haykor.carmanager.feature.car.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.Flow
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceRecordId

interface CarComponent {
    val state: Value<CarState>
    val events: Flow<CarEvent>

    fun accept(intent: CarIntent)

    fun interface Factory {
        fun create(
            ctx: ComponentContext,
            carId: CarId,
            onBack: () -> Unit,
            onOpenEdit: () -> Unit,
            onOpenRecord: (ServiceRecordId) -> Unit,
            onOpenCreateRecord: () -> Unit
        ): CarComponent
    }
}
