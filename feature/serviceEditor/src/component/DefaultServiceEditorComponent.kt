package ru.haykor.carmanager.feature.serviceEditor.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.doOnDestroy
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.filesDir
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.haykor.carmanager.core.domain.model.EditorMode
import ru.haykor.carmanager.core.domain.repository.PhotoRepository
import ru.haykor.carmanager.core.domain.repository.ServiceRecordRepository
import ru.haykor.carmanager.core.domain.usecase.GetInitialServiceEditorDataUseCase
import ru.haykor.carmanager.core.domain.usecase.MileageResolution
import ru.haykor.carmanager.core.domain.usecase.SaveRecordError
import ru.haykor.carmanager.core.domain.usecase.SaveServiceRecordUseCase
import ru.haykor.carmanager.core.presentation.component.createComponentScope
import ru.haykor.carmanager.core.result.DataResult
import ru.haykor.carmanager.feature.serviceEditor.component.ServiceEditorMutation.*

class DefaultServiceEditorComponent(
    componentContext: ComponentContext,
    override val editorMode: EditorMode,
    private val getInitialServiceEditorData: GetInitialServiceEditorDataUseCase,
    private val saveServiceRecord: SaveServiceRecordUseCase,
    private val recordRepo: ServiceRecordRepository,
    private val photoRepo: PhotoRepository,
    private val onBack: () -> Unit
) : ServiceEditorComponent,
    ComponentContext by componentContext {

    private val scope = createComponentScope()

    override val state: Value<ServiceEditorState>
        field = MutableValue<ServiceEditorState>(ServiceEditorState.Loading)

    private val _events = Channel<ServiceEditorEvent>(Channel.BUFFERED)
    override val events = _events.receiveAsFlow()

    private var isExitHandled = false

    private val backCallback = BackCallback(isEnabled = false) {
        // if we're inside of this lambda, condition is already met
        accept(ServiceEditorIntent.BackClicked)
    }

    /**
     * Contains keys, e.g. 'photo/example.jpg'
     */
    private val trashImages: MutableList<String> = mutableListOf()

    /**
     * Identifies the latest image pick; older imports finishing later are ignored.
     */
    private var imagePickGeneration = 0

    override fun accept(intent: ServiceEditorIntent): Unit = when (intent) {
        ServiceEditorIntent.BackClicked -> onBackClicked()

        is ServiceEditorIntent.CategoryPicked -> reduce(CategoryPicked(intent.categoryId))

        is ServiceEditorIntent.DatePicked -> reduce(DatePicked(intent.date))

        is ServiceEditorIntent.ImagePicked -> {
            copyImage(intent.imagePath)
        }

        is ServiceEditorIntent.FieldBlurred -> reduce(FieldBlurred(intent.field))

        is ServiceEditorIntent.FieldChanged -> reduce(FieldChanged(intent.field, intent.value))

        ServiceEditorIntent.SaveClicked -> {
            reduce(AllFieldsTouched)
            trySaveRecord()
        }

        ServiceEditorIntent.DeleteClicked -> {
            delete()
        }

        ServiceEditorIntent.UpdateCarMileageClicked -> {
            reduce(AllFieldsTouched)
            reduce(MileageConflictResolved)
            trySaveRecord(resolution = MileageResolution.UPDATE_CAR)
        }

        ServiceEditorIntent.KeepCarMileageClicked -> {
            reduce(AllFieldsTouched)
            reduce(MileageConflictResolved)
            trySaveRecord(resolution = MileageResolution.KEEP_CAR)
        }

        ServiceEditorIntent.DismissSaveClicked -> {
            reduce(MileageConflictResolved)
        }

        ServiceEditorIntent.DialogConfirmClicked -> {
            reduce(SetExitDialogVisible(false))
            reduce(AllFieldsTouched)
            trySaveRecord()
        }

        ServiceEditorIntent.DialogIgnoreClicked -> {
            reduce(SetExitDialogVisible(false))
            disposeAndExit(keep = null)
        }

        ServiceEditorIntent.DialogDismissed -> {
            reduce(SetExitDialogVisible(false))
        }
    }

    init {
        loadInitialForm()
        backHandler.register(backCallback)
        lifecycle.doOnDestroy { backHandler.unregister(backCallback) }
    }

    private fun disposeAndExit(keep: String?) {
        scope.launch {
            disposeImages(keep = keep)
            exit()
        }
    }

    /**
     * Back closes the mileage conflict first, then the exit confirmation is handled only when
     * there are unsaved changes and nothing is being saved or deleted.
     */
    private fun onBackClicked() {
        val form = state.value as? ServiceEditorState.Form
        if (form?.mileageConflict != null) {
            reduce(MileageConflictResolved)
            return
        }
        if (form != null && form.isDirty && !form.isBusy) {
            reduce(SetExitDialogVisible(true))
        } else {
            exit()
        }
    }

    /**
     * Leaves the screen at most once: the user may leave while a save or delete is in flight, and
     * the result must not pop the next screen afterwards. Does nothing if the component is gone.
     */
    private fun exit() {
        if (isExitHandled || lifecycle.state == Lifecycle.State.DESTROYED) return
        isExitHandled = true
        onBack()
    }

    private fun loadInitialForm() {
        scope.launch {
            when (val result = getInitialServiceEditorData(editorMode)) {
                null -> reduce(SetNotFound)

                else -> {
                    reduce(
                        InitialFormLoaded(
                            input = result.input,
                            car = result.car,
                            categories = result.categories
                        )
                    )
                }
            }
        }
    }

    private fun trySaveRecord(resolution: MileageResolution = MileageResolution.ASK) {
        val form = (state.value as? ServiceEditorState.Form) ?: return
        if (form.isBusy) return

        reduce(SetIsSaving(true))
        val commitedPhotoKey = form.input.photoUri
        scope.launch {
            when (val result = saveServiceRecord(editorMode, form.input, form.input.carId, resolution)) {
                is DataResult.Failure -> {
                    reduce(SetIsSaving(false))
                    when (val error = result.error) {
                        is SaveRecordError.Data -> {
                            _events.send(ServiceEditorEvent.StorageError(error.dataError))
                        }

                        is SaveRecordError.MileageMismatch -> {
                            reduce(
                                MileageConflictDetected(
                                    MileageConflict(
                                        error.carMileage,
                                        error.recordMileage
                                    )
                                )
                            )
                        }

                        is SaveRecordError.FieldErrors -> {
                            reduce(ErrorsUpdated(error.errors))
                        }
                    }
                }

                is DataResult.Success -> {
                    disposeAndExit(keep = commitedPhotoKey)
                }
            }
        }
    }

    private fun delete() {
        if (editorMode !is EditorMode.Edit) return // can only delete an existing record
        val form = (state.value as? ServiceEditorState.Form) ?: return
        if (form.isBusy) return

        reduce(SetIsDeleting(true))
        scope.launch {
            when (val result = recordRepo.delete(id = editorMode.recordId)) {
                is DataResult.Success -> disposeAndExit(keep = null)

                is DataResult.Failure -> {
                    reduce(SetIsDeleting(false))
                    _events.send(ServiceEditorEvent.DeleteFailed(result.error))
                }
            }
        }
    }

    /**
     * Images are copied asynchronously, so several picks may be in flight at once. Only the latest
     * pick may land in the form: a slower import of an earlier pick must not overwrite a newer one.
     * Files of stale picks still get saved, so they are kept in [trashImages] to be cleaned up.
     */
    private fun copyImage(path: String?) {
        if (path == null) return

        val generation = ++imagePickGeneration
        reduce(SetIsImporting(true))

        scope.launch {
            when (val result = photoRepo.save(path)) {
                is DataResult.Success -> {
                    trashImages.add(result.data)
                    if (generation == imagePickGeneration) {
                        reduce(ImagePicked(result.data))
                    }
                }

                is DataResult.Failure -> {
                    if (generation == imagePickGeneration) {
                        _events.send(ServiceEditorEvent.PhotoImportFailed(result.error))
                    }
                }
            }

            if (generation == imagePickGeneration) {
                reduce(SetIsImporting(false))
            }
        }
    }

    private suspend fun disposeImages(keep: String?) {
        photoRepo.delete(trashImages.filterNot { it == keep })
    }

    private fun reduce(mutation: ServiceEditorMutation) {
        state.update { ServiceEditorReducer.reduce(it, mutation) }
        updateBackCallback()
    }

    private fun updateBackCallback() {
        val form = state.value as? ServiceEditorState.Form
        // While busy, back leaves the screen and cancels the operation, so no dialog is needed
        backCallback.isEnabled = form != null && form.isDirty && !form.isBusy
    }
}

@Inject
@ContributesBinding(AppScope::class)
class DefaultServiceEditorComponentFactory(
    private val getInitialServiceEditorData: GetInitialServiceEditorDataUseCase,
    private val saveServiceRecord: SaveServiceRecordUseCase,
    private val recordRepo: ServiceRecordRepository,
    private val photoRepo: PhotoRepository
) : ServiceEditorComponent.Factory {

    override fun create(ctx: ComponentContext, mode: EditorMode, onBack: () -> Unit): ServiceEditorComponent =
        DefaultServiceEditorComponent(
            componentContext = ctx,
            editorMode = mode,
            getInitialServiceEditorData = getInitialServiceEditorData,
            saveServiceRecord = saveServiceRecord,
            recordRepo = recordRepo,
            photoRepo = photoRepo,
            onBack = onBack
        )
}
