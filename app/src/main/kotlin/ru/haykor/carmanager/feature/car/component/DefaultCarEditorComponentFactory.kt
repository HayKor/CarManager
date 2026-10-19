package ru.haykor.carmanager.feature.car.component

import com.arkivanov.decompose.ComponentContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import ru.haykor.carmanager.core.domain.CarDraftParser
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.repository.CarRepository

@ContributesBinding(AppScope::class)
@Inject
class DefaultCarEditorComponentFactory(private val carRepository: CarRepository, private val parser: CarDraftParser) :
    CarEditorComponent.Factory {

    override fun create(ctx: ComponentContext, id: CarId?, onBack: () -> Unit) = DefaultCarEditorComponent(
        componentContext = ctx,
        id = id,
        carRepository = carRepository,
        parser = parser,
        onBack = onBack
    )
}
