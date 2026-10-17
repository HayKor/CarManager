package ru.haykor.carmanager.feature.garage.component

import com.arkivanov.decompose.ComponentContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.repository.CarRepository

@ContributesBinding(AppScope::class)
@Inject
class DefaultGarageComponentFactory(private val carRepository: CarRepository) : GarageComponent.Factory {
    override fun create(ctx: ComponentContext, onOpenCar: (CarId) -> Unit, onAddCar: () -> Unit): GarageComponent =
        DefaultGarageComponent(ctx, carRepository, onOpenCar, onAddCar)
}
