package ru.haykor.carmanager

import dev.zacsweers.metro.createGraphFactory
import ru.haykor.carmanager.di.AppGraph

class ReleaseApplication : BaseApp() {
    override val graph: AppGraph by lazy { createGraphFactory<ReleaseAppGraph.Factory>().create(this) }
}
