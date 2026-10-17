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
