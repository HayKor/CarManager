package ru.haykor.carmanager.feature.car.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import ru.haykor.carmanager.core.domain.CarId

interface CarEditorComponent {
    val state: Value<CarEditorState>

    fun accept(intent: CarEditorIntent)

    fun interface Factory {
        fun create(ctx: ComponentContext, id: CarId?, onBack: () -> Unit): CarEditorComponent
    }
}
