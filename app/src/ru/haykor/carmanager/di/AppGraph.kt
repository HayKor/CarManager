package ru.haykor.carmanager.di

import ru.haykor.carmanager.core.StartupTask
import ru.haykor.carmanager.root.component.RootComponent

interface AppGraph {
    val rootComponentFactory: RootComponent.Factory
    val startupTasks: Set<StartupTask>
}
