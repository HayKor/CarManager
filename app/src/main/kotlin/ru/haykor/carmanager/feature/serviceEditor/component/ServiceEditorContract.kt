package ru.haykor.carmanager.feature.serviceEditor.component

import ru.haykor.carmanager.core.domain.ServiceCategoryId
import ru.haykor.carmanager.core.domain.model.Car
import ru.haykor.carmanager.core.domain.model.FieldError
import ru.haykor.carmanager.core.domain.model.RecordField
import ru.haykor.carmanager.core.domain.model.ServiceCategory
import ru.haykor.carmanager.core.domain.model.ServiceRecordFormInput
import ru.haykor.carmanager.feature.serviceEditor.component.ServiceEditorState.Form

data class MileageConflict(val carMileage: Long, val recordMileage: Long)

sealed interface ServiceEditorState {
    data object Loading : ServiceEditorState
    data object NotFound : ServiceEditorState
    data class Form(
        val input: ServiceRecordFormInput,
        val car: Car,
        val categories: List<ServiceCategory>,
        val touched: Set<RecordField> = emptySet(),
        val errors: Map<RecordField, FieldError> = emptyMap(),
        val isSaving: Boolean = false,
        val mileageConflict: MileageConflict? = null
    ) : ServiceEditorState
}

sealed interface ServiceEditorIntent {
    data class FieldChanged(val field: RecordField, val value: String) : ServiceEditorIntent
    data class FieldBlurred(val field: RecordField) : ServiceEditorIntent

    data class CategoryPicked(val categoryId: ServiceCategoryId) : ServiceEditorIntent
    data class DatePicked(val date: Long) : ServiceEditorIntent

    data object SaveClicked : ServiceEditorIntent
    data object UpdateCarMileageClicked : ServiceEditorIntent
    data object KeepCarMileageClicked : ServiceEditorIntent
    data object DismissSaveClicked : ServiceEditorIntent

    data object BackClicked : ServiceEditorIntent
}

sealed interface ServiceEditorMutation {
    data class InitialFormLoaded(
        val input: ServiceRecordFormInput,
        val car: Car,
        val categories: List<ServiceCategory>
    ) : ServiceEditorMutation

    data class FieldChanged(val field: RecordField, val value: String) : ServiceEditorMutation
    data class FieldBlurred(val field: RecordField) : ServiceEditorMutation
    data object AllFieldsTouched : ServiceEditorMutation
    data class ErrorsUpdated(val errors: Map<RecordField, FieldError>) : ServiceEditorMutation

    data class CategoryPicked(val categoryId: ServiceCategoryId) : ServiceEditorMutation
    data class DatePicked(val date: Long) : ServiceEditorMutation

    data class SetIsSaving(val value: Boolean) : ServiceEditorMutation
    data object SetNotFound : ServiceEditorMutation

    data class MileageConflictDetected(val conflict: MileageConflict) : ServiceEditorMutation
    data object MileageConflictResolved : ServiceEditorMutation
}

object ServiceEditorReducer {
    fun reduce(state: ServiceEditorState, mutation: ServiceEditorMutation): ServiceEditorState = when (mutation) {
        is ServiceEditorMutation.InitialFormLoaded -> {
            Form(input = mutation.input, car = mutation.car, categories = mutation.categories)
        }

        is ServiceEditorMutation.FieldChanged -> {
            (state as? Form)?.let {
                it.copy(
                    input = it.input.change(
                        mutation.field,
                        mutation.value
                    )
                )
            } ?: state
        }

        ServiceEditorMutation.AllFieldsTouched -> {
            (state as? Form)?.copy(touched = RecordField.entries.toSet()) ?: state
        }

        is ServiceEditorMutation.FieldBlurred -> {
            (state as? Form)?.let { it.copy(touched = it.touched + mutation.field) } ?: state
        }

        is ServiceEditorMutation.ErrorsUpdated -> {
            (state as? Form)?.copy(errors = mutation.errors) ?: state
        }

        is ServiceEditorMutation.CategoryPicked -> {
            (state as? Form)?.let {
                it.copy(input = it.input.copy(categoryId = mutation.categoryId))
            } ?: state
        }

        is ServiceEditorMutation.DatePicked -> {
            (state as? Form)?.let {
                it.copy(input = it.input.copy(date = mutation.date))
            } ?: state
        }

        is ServiceEditorMutation.SetIsSaving -> {
            (state as? Form)?.copy(isSaving = mutation.value) ?: state
        }

        ServiceEditorMutation.SetNotFound -> ServiceEditorState.NotFound

        is ServiceEditorMutation.MileageConflictDetected ->
            (state as? Form)?.copy(mileageConflict = mutation.conflict) ?: state

        ServiceEditorMutation.MileageConflictResolved ->
            (state as? Form)?.copy(mileageConflict = null) ?: state
    }
}

private fun ServiceRecordFormInput.change(field: RecordField, value: String) = when (field) {
    RecordField.AT_MILEAGE -> copy(atMileage = value)
    RecordField.NOTE -> copy(note = value)
    RecordField.PRICE -> copy(price = value)
}
