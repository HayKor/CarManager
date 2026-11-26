package ru.haykor.carmanager.photoviewer.component

import com.arkivanov.decompose.ComponentContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.haykor.carmanager.core.domain.repository.PhotoRepository
import ru.haykor.carmanager.core.presentation.component.createComponentScope
import ru.haykor.carmanager.core.result.DataResult

class DefaultPhotoViewerComponent(
    componentContext: ComponentContext,
    private val photoRepository: PhotoRepository,
    override val photoPath: String,
    private val onBack: () -> Unit
) : PhotoViewerComponent,
    ComponentContext by componentContext {

    private val scope = createComponentScope()

    private val _events = Channel<PhotoViewerEvent>(Channel.BUFFERED)
    override val events = _events.receiveAsFlow()

    override fun save() {
        scope.launch {
            when (val result = photoRepository.saveToGallery(photoPath)) {
                is DataResult.Success -> {
                    _events.send(PhotoViewerEvent.ImageSaved)
                }

                is DataResult.Failure -> {
                    _events.send(PhotoViewerEvent.ImageSaveFailed)
                }
            }
        }
    }

    override fun back() = onBack()
}

@ContributesBinding(AppScope::class)
class DefaultPhotoViewerFactory(private val photoRepository: PhotoRepository) : PhotoViewerComponent.Factory {
    override fun create(ctx: ComponentContext, photoPath: String, onBack: () -> Unit): PhotoViewerComponent =
        DefaultPhotoViewerComponent(
            componentContext = ctx,
            photoRepository = photoRepository,
            photoPath = photoPath,
            onBack = onBack
        )
}
