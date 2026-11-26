package ru.haykor.carmanager.feature.search.component

import com.arkivanov.decompose.ComponentContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding

class DefaultSearchComponent(componentContext: ComponentContext) :
    SearchComponent,
    ComponentContext by componentContext

@ContributesBinding(AppScope::class)
class DefaultSearchComponentFactory : SearchComponent.Factory {
    override fun create(ctx: ComponentContext): SearchComponent = DefaultSearchComponent(
        componentContext = ctx
    )
}
