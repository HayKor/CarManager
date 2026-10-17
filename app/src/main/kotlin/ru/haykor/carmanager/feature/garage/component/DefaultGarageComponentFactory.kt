package ru.haykor.carmanager.feature.garage.component

import com.arkivanov.decompose.ComponentContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import ru.haykor.carmanager.core.domain.CarId

@ContributesBinding(AppScope::class)
@Inject
class DefaultGarageComponentFactory : GarageComponent.Factory {
    override fun create(ctx: ComponentContext, onOpenCar: (CarId) -> Unit): GarageComponent =
        DefaultGarageComponent(ctx, onOpenCar)
}
