package ru.haykor.carmanager.photoviewer.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow

interface PhotoViewerComponent {
    val photoPath: String
    val events: Flow<PhotoViewerEvent>

    fun save()
    fun back()

    fun interface Factory {
        fun create(ctx: ComponentContext, photoPath: String, onBack: () -> Unit): PhotoViewerComponent
    }
}

sealed interface PhotoViewerEvent {
    data object ImageSaved : PhotoViewerEvent
    data object ImageSaveFailed : PhotoViewerEvent
}
