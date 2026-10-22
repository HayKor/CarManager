package ru.haykor.carmanager.feature.car.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import ru.haykor.carmanager.core.domain.CarId

interface CarComponent {
    val id: CarId
    val state: Value<CarState>

    fun accept(intent: CarIntent)

    fun interface Factory {
        fun create(ctx: ComponentContext, carId: CarId, onBack: () -> Unit, onOpenEdit: (CarId) -> Unit): CarComponent
    }
}
