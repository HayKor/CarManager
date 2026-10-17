package ru.haykor.carmanager.feature.car.component

import com.arkivanov.decompose.ComponentContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import ru.haykor.carmanager.core.domain.CarId

@ContributesBinding(AppScope::class)
@Inject
class DefaultCarComponentFactory : CarComponent.Factory {
    override fun create(ctx: ComponentContext, carId: CarId, onBack: () -> Unit): CarComponent =
        DefaultCarComponent(ctx, carId, onBack)
}
