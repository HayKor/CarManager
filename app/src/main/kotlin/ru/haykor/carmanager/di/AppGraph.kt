package ru.haykor.carmanager.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import ru.haykor.carmanager.root.component.RootComponent

@DependencyGraph(AppScope::class)
interface AppGraph {
    val rootComponentFactory: RootComponent.Factory
}
