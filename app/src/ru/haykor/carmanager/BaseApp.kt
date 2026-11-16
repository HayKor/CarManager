package ru.haykor.carmanager

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.haykor.carmanager.di.AppGraph

abstract class BaseApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    abstract val graph: AppGraph

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            appGraph.startupTasks.forEach { it.run() }
        }
    }
}

val Context.appGraph: AppGraph
    get() = (applicationContext as BaseApp).graph
