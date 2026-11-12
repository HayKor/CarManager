package ru.haykor.carmanager

import dev.zacsweers.metro.createGraphFactory
import ru.haykor.carmanager.di.AppGraph

class DebugApplication : BaseApp() {
    override val graph: AppGraph by lazy { createGraphFactory<DebugAppGraph.Factory>().create(this) }
}
