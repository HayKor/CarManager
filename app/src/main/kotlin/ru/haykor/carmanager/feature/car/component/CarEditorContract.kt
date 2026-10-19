package ru.haykor.carmanager.feature.car.component

import androidx.compose.runtime.Stable
import ru.haykor.carmanager.core.domain.PlateCountry
import ru.haykor.carmanager.core.domain.model.CarFormInput
import ru.haykor.carmanager.core.domain.model.Field
import ru.haykor.carmanager.core.domain.model.FieldError
import ru.haykor.carmanager.feature.car.component.CarEditorState.Form

@Stable
sealed interface CarEditorState {
    data object Loading : CarEditorState
    data object NotFound : CarEditorState
    data class Form(
        val input: CarFormInput = CarFormInput(),
        val touched: Set<Field> = emptySet(),
        val errors: Map<Field, FieldError> = emptyMap(),
        val isSaving: Boolean = false,
        val isNew: Boolean = false
    ) : CarEditorState
}

sealed interface CarEditorIntent {
    data class FieldChanged(val field: Field, val value: String) : CarEditorIntent
    data class FieldBlurred(val field: Field) : CarEditorIntent
    data class PlateCountryChanged(val plateCountry: PlateCountry) : CarEditorIntent
    data object SaveClicked : CarEditorIntent
    data object BackClicked : CarEditorIntent
}

sealed interface CarEditorMutation {
    data class FormInputLoaded(val input: CarFormInput) : CarEditorMutation
    data object FormInputNew : CarEditorMutation
    data class FieldChanged(val field: Field, val value: String) : CarEditorMutation
    data class FieldBlurred(val field: Field) : CarEditorMutation
    data object AllFieldsTouched : CarEditorMutation
    data class ErrorsUpdated(val errors: Map<Field, FieldError>) : CarEditorMutation
    data class PlateCountryChanged(val plateCountry: PlateCountry) : CarEditorMutation
    data class SetSaving(val value: Boolean) : CarEditorMutation
    data object SetLoading : CarEditorMutation
    data object SetNotFound : CarEditorMutation
}

object CarEditorReducer {
    fun reduce(state: CarEditorState, mutation: CarEditorMutation): CarEditorState = when (mutation) {
        is CarEditorMutation.FieldChanged -> {
            (state as? Form)?.let { it.copy(input = it.input.change(mutation.field, mutation.value)) }
                ?: state
        }

        is CarEditorMutation.FieldBlurred -> {
            (state as? Form)?.let { it.copy(touched = it.touched + mutation.field) } ?: state
        }

        CarEditorMutation.AllFieldsTouched -> {
            (state as? Form)?.copy(touched = Field.entries.toSet()) ?: state
        }

        is CarEditorMutation.PlateCountryChanged -> {
            (state as? Form)?.let {
                it.copy(input = it.input.copy(plateCountry = mutation.plateCountry))
            }
                ?: state
        }

        is CarEditorMutation.SetSaving -> {
            (state as? Form)?.copy(isSaving = mutation.value) ?: state
        }

        CarEditorMutation.SetLoading -> CarEditorState.Loading

        CarEditorMutation.SetNotFound -> CarEditorState.NotFound

        is CarEditorMutation.FormInputLoaded -> {
            Form(input = mutation.input)
        }

        CarEditorMutation.FormInputNew -> {
            Form(isNew = true)
        }

        is CarEditorMutation.ErrorsUpdated -> {
            (state as? Form)?.copy(errors = mutation.errors) ?: state
        }
    }
}

private fun CarFormInput.change(field: Field, value: String): CarFormInput = when (field) {
    Field.BRAND -> copy(brand = value)
    Field.MODEL -> copy(model = value)
    Field.YEAR -> copy(year = value)
    Field.MILEAGE -> copy(mileage = value)
    Field.VIN -> copy(vin = value)
    Field.PLATE -> copy(plate = value)
}
