package ru.haykor.carmanager.di

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import ru.haykor.carmanager.core.StartupTask
import ru.haykor.carmanager.root.component.RootComponent

@DependencyGraph(AppScope::class)
interface AppGraph {
    val rootComponentFactory: RootComponent.Factory
    val startupTasks: Set<StartupTask>

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides context: Context): AppGraph
    }
}
