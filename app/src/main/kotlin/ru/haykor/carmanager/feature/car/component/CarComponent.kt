package ru.haykor.carmanager.feature.car.component

import com.arkivanov.decompose.ComponentContext
import ru.haykor.carmanager.feature.car.domain.CarId

interface CarComponent {
    val id: CarId

    fun onBackClicked()

    fun interface Factory {
        fun create(ctx: ComponentContext, carId: CarId, onBack: () -> Unit): CarComponent
    }
}

class DefaultCarComponent(componentContext: ComponentContext, override val id: CarId, val onBack: () -> Unit) :
    CarComponent,
    ComponentContext by componentContext {

    override fun onBackClicked() = onBack()
}
