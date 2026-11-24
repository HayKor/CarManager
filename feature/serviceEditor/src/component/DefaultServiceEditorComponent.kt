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
    private val onBack: () -> Unit,
    private val onOpenImage: (photoPath: String) -> Unit
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
     * Keys of photos copied into app storage during this editing session, e.g. 'photo/example.jpg'.
     * No saved record references them until a save succeeds, so on exit they are deleted unless
     * the successful save committed one of them.
     */
    private val importedPhotoKeys: MutableSet<String> = mutableSetOf()

    /**
     * Identifies the latest image pick; older imports finishing later are ignored.
     */
    private var imagePickGeneration = 0

    override fun accept(intent: ServiceEditorIntent): Unit = when (intent) {
        ServiceEditorIntent.BackClicked -> onBackClicked()

        is ServiceEditorIntent.CategoryPicked -> reduce(CategoryPicked(intent.categoryId))

        is ServiceEditorIntent.DatePicked -> reduce(DatePicked(intent.date))

        ServiceEditorIntent.ImageClicked -> {
            val photoPath = (state.value as? ServiceEditorState.Form)?.input?.photoUri ?: return
            onOpenImage(photoPath)
        }

        is ServiceEditorIntent.ImagePicked -> {
            importPhoto(intent.imagePath)
        }

        ServiceEditorIntent.ImageUnpinned -> {
            // An import still in flight must not restore the photo the user has just removed.
            ++imagePickGeneration
            reduce(ImageUnpinned)
            reduce(SetIsImporting(false))
        }

        is ServiceEditorIntent.FieldBlurred -> reduce(FieldBlurred(intent.field))

        is ServiceEditorIntent.FieldChanged -> reduce(FieldChanged(intent.field, intent.value))

        ServiceEditorIntent.SaveClicked -> {
            reduce(AllFieldsTouched)
            trySaveRecord()
        }

        ServiceEditorIntent.DeleteClicked -> {
            deleteRecord()
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
            exitAfterDiscard()
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

    /**
     * After a successful save only the committed photo may survive. Stale imports and the photo
     * the record used to reference are orphans now.
     */
    private fun exitAfterSave(committedPhoto: String?) {
        scope.launch {
            deletePhotos(allPhotoKeys().filterNot { it == committedPhoto })
            exit()
        }
    }

    /**
     * A discarded edit was never written to the database, so only this session's imports are
     * orphans. The stored photo still belongs to the record.
     */
    private fun exitAfterDiscard() {
        scope.launch {
            deletePhotos(importedPhotoKeys)
            exit()
        }
    }

    /** The record is gone, so both its stored photo and every session import are orphans. */
    private fun exitAfterDelete() {
        scope.launch {
            deletePhotos(allPhotoKeys())
            exit()
        }
    }

    /**
     * Every photo file the record may own: the one loaded with the form plus all session imports.
     */
    private fun allPhotoKeys(): List<String> {
        val storedPhoto = (state.value as? ServiceEditorState.Form)?.initialInput?.photoUri
        return (importedPhotoKeys + listOfNotNull(storedPhoto)).toList()
    }

    private suspend fun deletePhotos(keys: Collection<String>) {
        photoRepo.delete(keys.toList())
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
        when {
            form != null && form.isDirty && !form.isBusy -> reduce(SetExitDialogVisible(true))

            // A save/delete may still commit; the coroutine cancellation cleans nothing up itself.
            form != null && form.isBusy -> exit()

            // Picking and then unpinning can leave the form clean while an import is already on disk.
            importedPhotoKeys.isNotEmpty() -> exitAfterDiscard()

            else -> exit()
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
        val committedPhotoKey = form.input.photoUri
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
                    exitAfterSave(committedPhotoKey)
                }
            }
        }
    }

    private fun deleteRecord() {
        if (editorMode !is EditorMode.Edit) return // can only delete an existing record
        val form = (state.value as? ServiceEditorState.Form) ?: return
        if (form.isBusy) return

        reduce(SetIsDeleting(true))
        scope.launch {
            when (val result = recordRepo.delete(id = editorMode.recordId)) {
                is DataResult.Success -> exitAfterDelete()

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
     * Files of stale picks still get saved, so they are kept in [importedPhotoKeys] to be cleaned up.
     */
    private fun importPhoto(path: String?) {
        if (path == null) return

        val generation = ++imagePickGeneration
        reduce(SetIsImporting(true))

        scope.launch {
            when (val result = photoRepo.save(path)) {
                is DataResult.Success -> {
                    importedPhotoKeys.add(result.data)
                    if (generation == imagePickGeneration) {
                        reduce(ImagePicked(result.data))
                    } else {
                        // The import is stale, but its file still needs a cleanup-worthy back press.
                        updateBackCallback()
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

    private fun reduce(mutation: ServiceEditorMutation) {
        state.update { ServiceEditorReducer.reduce(it, mutation) }
        updateBackCallback()
    }

    private fun updateBackCallback() {
        val form = state.value as? ServiceEditorState.Form
        // While busy, back leaves the screen and cancels the operation, so no dialog is needed.
        // Pending imports must be intercepted even when the form itself is clean.
        backCallback.isEnabled = form != null &&
            (form.isDirty || importedPhotoKeys.isNotEmpty()) &&
            !form.isBusy
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

    override fun create(
        ctx: ComponentContext,
        mode: EditorMode,
        onOpenImage: (photoPath: String) -> Unit,
        onBack: () -> Unit
    ): ServiceEditorComponent = DefaultServiceEditorComponent(
        componentContext = ctx,
        editorMode = mode,
        getInitialServiceEditorData = getInitialServiceEditorData,
        saveServiceRecord = saveServiceRecord,
        recordRepo = recordRepo,
        photoRepo = photoRepo,
        onBack = onBack,
        onOpenImage = onOpenImage
    )
}
