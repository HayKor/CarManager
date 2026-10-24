package ru.haykor.carmanager.feature.main.component

import com.arkivanov.decompose.ComponentContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import ru.haykor.carmanager.feature.garage.component.GarageComponent

@ContributesBinding(AppScope::class)
@Inject
class DefaultMainComponentFactory : MainComponent.Factory {
    override fun create(ctx: ComponentContext, garage: (ComponentContext) -> GarageComponent): MainComponent =
        DefaultMainComponent(
            componentContext = ctx,
            garage = garage
        )
}
