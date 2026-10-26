package ru.haykor.carmanager

import android.app.Application
import android.content.Context
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.haykor.carmanager.di.AppGraph

class CarManagerApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var appGraph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        appGraph = createGraphFactory<AppGraph.Factory>().create(this)
        appScope.launch {
            appGraph.startupTasks.forEach { it.run() }
        }
    }
}

val Context.appGraph: AppGraph
    get() = (applicationContext as CarManagerApp).appGraph
