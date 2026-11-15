package ru.haykor.carmanager.feature.serviceEditor.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.Flow
import ru.haykor.carmanager.core.domain.model.EditorMode

interface ServiceEditorComponent {
    val editorMode: EditorMode
    val state: Value<ServiceEditorState>
    val events: Flow<ServiceEditorEvent>

    fun accept(intent: ServiceEditorIntent)

    fun interface Factory {
        fun create(ctx: ComponentContext, mode: EditorMode, onBack: () -> Unit): ServiceEditorComponent
    }
}
