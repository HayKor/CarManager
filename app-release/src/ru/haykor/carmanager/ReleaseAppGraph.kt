package ru.haykor.carmanager

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import ru.haykor.carmanager.core.StartupTask
import ru.haykor.carmanager.di.AppGraph

@DependencyGraph(AppScope::class)
interface ReleaseAppGraph : AppGraph {

    @Provides
    fun provideStartupTasks(): Set<StartupTask> = emptySet()

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides context: Context): ReleaseAppGraph
    }
}
