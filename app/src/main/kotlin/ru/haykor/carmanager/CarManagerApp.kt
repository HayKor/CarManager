package ru.haykor.carmanager

import android.app.Application
import android.content.Context
import dev.zacsweers.metro.createGraph
import dev.zacsweers.metro.createGraphFactory
import ru.haykor.carmanager.di.AppGraph

class CarManagerApp : Application() {
    lateinit var appGraph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        appGraph = createGraphFactory<AppGraph.Factory>().create(this)
    }
}

val Context.appGraph: AppGraph
    get() = (applicationContext as CarManagerApp).appGraph
