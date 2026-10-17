package ru.haykor.carmanager.core.presentation.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

fun ComponentContext.createComponentScope(): CoroutineScope =
    CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { scope ->
        lifecycle.doOnDestroy(scope::cancel)
    }
