package ru.haykor.carmanager.feature.serviceEditor.component

import com.arkivanov.decompose.ComponentContext
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceRecordId

sealed interface EditorMode {
    data class Create(val carId: CarId) : EditorMode
    data class Edit(val recordId: ServiceRecordId) : EditorMode
}

interface ServiceEditorComponent {
    val editorMode: EditorMode

    fun interface Factory {
        fun create(ctx: ComponentContext, mode: EditorMode, onBack: () -> Unit): ServiceEditorComponent
    }
}
