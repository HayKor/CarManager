package ru.haykor.carmanager.core.designsystem

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import ru.haykor.carmanager.Res
import ru.haykor.carmanager.cancel
import ru.haykor.carmanager.dont_save
import ru.haykor.carmanager.save
import ru.haykor.carmanager.unsaved_changes_text
import ru.haykor.carmanager.unsaved_changes_title

/** Confirmation shown when leaving a form with unsaved changes. */
@Composable
fun UnsavedChangesDialog(
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(Res.string.unsaved_changes_title))
        },
        text = {
            Text(stringResource(Res.string.unsaved_changes_text))
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.cancel))
                }

                TextButton(onClick = onDiscard) {
                    Text(
                        text = stringResource(Res.string.dont_save),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        modifier = modifier
    )
}
