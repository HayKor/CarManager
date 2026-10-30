package ru.haykor.carmanager.root.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import ru.haykor.carmanager.feature.car.component.CarComponent
import ru.haykor.carmanager.feature.carEditor.component.CarEditorComponent
import ru.haykor.carmanager.feature.main.component.MainComponent
import ru.haykor.carmanager.feature.serviceEditor.component.ServiceEditorComponent

interface RootComponent : BackHandlerOwner {
    val childStack: Value<ChildStack<*, Child>>

    fun onBack()

    sealed interface Child {
        data class Main(val component: MainComponent) : Child
        data class Car(val component: CarComponent) : Child
        data class CarEditor(val component: CarEditorComponent) : Child
        data class ServiceEditor(val component: ServiceEditorComponent) : Child
    }

    fun interface Factory {
        fun create(ctx: ComponentContext): RootComponent
    }
}
