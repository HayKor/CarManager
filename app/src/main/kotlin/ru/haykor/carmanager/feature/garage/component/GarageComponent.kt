package ru.haykor.carmanager.feature.garage.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import ru.haykor.carmanager.core.domain.CarId

interface GarageComponent {
    val state: Value<GarageState>

    fun accept(intent: GarageIntent)

    fun interface Factory {
        fun create(ctx: ComponentContext, onOpenCar: (CarId) -> Unit, onAddCar: () -> Unit): GarageComponent
    }
}
