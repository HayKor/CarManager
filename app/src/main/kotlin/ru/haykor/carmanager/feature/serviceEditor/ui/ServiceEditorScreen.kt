package ru.haykor.carmanager.feature.serviceEditor.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.haykor.carmanager.feature.serviceEditor.component.ServiceEditorComponent

@Composable
fun ServiceEditorScreen(component: ServiceEditorComponent, modifier: Modifier = Modifier) {
    ServiceEditorContent(
        modifier = modifier
    )
}

@Composable
private fun ServiceEditorContent(modifier: Modifier = Modifier) {
}
