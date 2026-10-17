package ru.haykor.carmanager.feature.garage.component

import com.arkivanov.decompose.ComponentContext
import ru.haykor.carmanager.feature.car.domain.CarId

class DefaultGarageComponent(componentContext: ComponentContext, private val onOpenCar: (CarId) -> Unit) :
    GarageComponent,
    ComponentContext by componentContext {

    override fun onCarClick(carId: CarId) = onOpenCar(carId)
}
