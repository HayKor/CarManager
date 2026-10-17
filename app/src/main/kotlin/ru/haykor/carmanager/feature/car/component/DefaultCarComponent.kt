package ru.haykor.carmanager.feature.car.component

import com.arkivanov.decompose.ComponentContext
import ru.haykor.carmanager.core.domain.CarId

class DefaultCarComponent(componentContext: ComponentContext, override val id: CarId, val onBack: () -> Unit) :
    CarComponent,
    ComponentContext by componentContext {

    override fun onBackClicked() = onBack()
}
