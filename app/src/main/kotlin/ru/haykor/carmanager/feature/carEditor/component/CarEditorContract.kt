package ru.haykor.carmanager.feature.carEditor.component

import androidx.compose.runtime.Stable
import ru.haykor.carmanager.core.domain.PlateCountry
import ru.haykor.carmanager.core.domain.error.DataError
import ru.haykor.carmanager.core.domain.model.CarField
import ru.haykor.carmanager.core.domain.model.CarFormInput
import ru.haykor.carmanager.core.domain.model.FieldError

@Stable
sealed interface CarEditorState {
    data object Loading : CarEditorState
    data object NotFound : CarEditorState
    data class Form(
        val input: CarFormInput = CarFormInput(),
        val touched: Set<CarField> = emptySet(),
        val errors: Map<CarField, FieldError> = emptyMap(),
        val isSaving: Boolean = false,
        val isNew: Boolean = false
    ) : CarEditorState
}

sealed interface CarEditorIntent {
    data class FieldChanged(val field: CarField, val value: String) : CarEditorIntent
    data class FieldBlurred(val field: CarField) : CarEditorIntent
    data class PlateCountryChanged(val plateCountry: PlateCountry) : CarEditorIntent
    data object SaveClicked : CarEditorIntent
    data object BackClicked : CarEditorIntent
}

sealed interface CarEditorEvent {
    data class SaveFailed(val error: DataError) : CarEditorEvent
}

sealed interface CarEditorMutation {
    data class FormInputLoaded(val input: CarFormInput) : CarEditorMutation
    data object FormInputNew : CarEditorMutation
    data class FieldChanged(val field: CarField, val value: String) : CarEditorMutation
    data class FieldBlurred(val field: CarField) : CarEditorMutation
    data object AllFieldsTouched : CarEditorMutation
    data class ErrorsUpdated(val errors: Map<CarField, FieldError>) : CarEditorMutation
    data class PlateCountryChanged(val plateCountry: PlateCountry) : CarEditorMutation
    data class SetSaving(val value: Boolean) : CarEditorMutation
    data object SetLoading : CarEditorMutation
    data object SetNotFound : CarEditorMutation
}

object CarEditorReducer {
    fun reduce(state: CarEditorState, mutation: CarEditorMutation): CarEditorState = when (mutation) {
        is CarEditorMutation.FieldChanged -> {
            (state as? CarEditorState.Form)?.let { it.copy(input = it.input.change(mutation.field, mutation.value)) }
                ?: state
        }

        is CarEditorMutation.FieldBlurred -> {
            (state as? CarEditorState.Form)?.let { it.copy(touched = it.touched + mutation.field) } ?: state
        }

        CarEditorMutation.AllFieldsTouched -> {
            (state as? CarEditorState.Form)?.copy(touched = CarField.entries.toSet()) ?: state
        }

        is CarEditorMutation.PlateCountryChanged -> {
            (state as? CarEditorState.Form)?.let {
                it.copy(input = it.input.copy(plateCountry = mutation.plateCountry))
            }
                ?: state
        }

        is CarEditorMutation.SetSaving -> {
            (state as? CarEditorState.Form)?.copy(isSaving = mutation.value) ?: state
        }

        CarEditorMutation.SetLoading -> CarEditorState.Loading

        CarEditorMutation.SetNotFound -> CarEditorState.NotFound

        is CarEditorMutation.FormInputLoaded -> {
            CarEditorState.Form(input = mutation.input)
        }

        CarEditorMutation.FormInputNew -> {
            CarEditorState.Form(isNew = true)
        }

        is CarEditorMutation.ErrorsUpdated -> {
            (state as? CarEditorState.Form)?.copy(errors = mutation.errors) ?: state
        }
    }
}

private fun CarFormInput.change(field: CarField, value: String): CarFormInput = when (field) {
    CarField.BRAND -> copy(brand = value)
    CarField.MODEL -> copy(model = value)
    CarField.YEAR -> copy(year = value)
    CarField.NOTE -> copy(note = value)
    CarField.MILEAGE -> copy(mileage = value)
    CarField.VIN -> copy(vin = value)
    CarField.PLATE -> copy(plate = value)
}
