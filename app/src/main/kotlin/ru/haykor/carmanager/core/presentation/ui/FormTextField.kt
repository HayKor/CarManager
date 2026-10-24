package ru.haykor.carmanager.core.presentation.ui

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged

/**
 * Outlined text field for forms: shows [error] instead of [hint] below the field
 * and reports [onFocusLost] only on a real focused → unfocused transition.
 */
@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onFocusLost: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    hint: String? = null,
    placeholder: String? = null,
    suffix: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var wasFocused by remember { mutableStateOf(false) }
    val supportingText = error ?: hint

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = supportingText?.let { { Text(it) } },
        suffix = suffix?.let { { Text(it) } },
        isError = error != null,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier.onFocusChanged { state ->
            if (wasFocused && !state.isFocused) onFocusLost()
            wasFocused = state.isFocused
        }
    )
}
