package ru.haykor.carmanager.feature.car.component

import com.arkivanov.decompose.ComponentContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.repository.CarRepository

@ContributesBinding(AppScope::class)
@Inject
class DefaultCarComponentFactory(private val repository: CarRepository) : CarComponent.Factory {
    override fun create(ctx: ComponentContext, carId: CarId, onBack: () -> Unit): CarComponent =
        DefaultCarComponent(ctx, repository, carId, onBack)
}
