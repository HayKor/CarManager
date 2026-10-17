package ru.haykor.carmanager.feature.garage.component

import com.arkivanov.decompose.ComponentContext
import ru.haykor.carmanager.feature.car.domain.CarId

interface GarageComponent {
    fun onCarClick(carId: CarId)

    fun interface Factory {
        fun create(ctx: ComponentContext, onOpenCar: (CarId) -> Unit): GarageComponent
    }
}
