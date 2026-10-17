package ru.haykor.carmanager.root.component

import com.arkivanov.decompose.ComponentContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import ru.haykor.carmanager.feature.car.component.CarComponent
import ru.haykor.carmanager.feature.car.component.CarEditorComponent
import ru.haykor.carmanager.feature.garage.component.GarageComponent
import ru.haykor.carmanager.feature.main.component.MainComponent

@ContributesBinding(AppScope::class)
@Inject
class DefaultRootComponentFactory(
    private val mainFactory: MainComponent.Factory,
    private val garageFactory: GarageComponent.Factory,
    private val carFactory: CarComponent.Factory,
    private val carEditorFactory: CarEditorComponent.Factory
) : RootComponent.Factory {
    override fun create(ctx: ComponentContext): RootComponent = DefaultRootComponent(
        componentContext = ctx,
        mainFactory = mainFactory,
        garageFactory = garageFactory,
        carFactory = carFactory,
        carEditorFactory = carEditorFactory
    )
}
