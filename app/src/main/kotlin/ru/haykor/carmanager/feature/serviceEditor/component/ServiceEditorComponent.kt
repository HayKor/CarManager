package ru.haykor.carmanager.feature.serviceEditor.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceRecordId

@Serializable
sealed interface EditorMode {
    @Serializable
    data class Create(val carId: CarId) : EditorMode

    @Serializable
    data class Edit(val recordId: ServiceRecordId) : EditorMode
}

interface ServiceEditorComponent {
    val editorMode: EditorMode
    val state: Value<ServiceEditorState>
    val events: Flow<ServiceEditorEvent>

    fun accept(intent: ServiceEditorIntent)

    fun interface Factory {
        fun create(ctx: ComponentContext, mode: EditorMode, onBack: () -> Unit): ServiceEditorComponent
    }
}
