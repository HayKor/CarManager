package ru.haykor.carmanager.feature.car.component

import com.arkivanov.decompose.ComponentContext
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.feature.car.domain.repository.CarRepository

class DefaultCarComponent(
    componentContext: ComponentContext,
    private val repository: CarRepository,
    override val id: CarId,
    val onBack: () -> Unit
) : CarComponent,
    ComponentContext by componentContext {

    override fun onBackClicked() = onBack()
}
