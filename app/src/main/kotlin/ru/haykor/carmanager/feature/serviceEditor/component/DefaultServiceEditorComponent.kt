package ru.haykor.carmanager.feature.serviceEditor.component

import com.arkivanov.decompose.ComponentContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

class DefaultServiceEditorComponent(
    componentContext: ComponentContext,
    override val editorMode: EditorMode,
    private val onBack: () -> Unit
) : ServiceEditorComponent,
    ComponentContext by componentContext

@Inject
@ContributesBinding(AppScope::class)
class DefaultServiceEditorComponentFactory : ServiceEditorComponent.Factory {

    override fun create(ctx: ComponentContext, mode: EditorMode, onBack: () -> Unit): ServiceEditorComponent =
        DefaultServiceEditorComponent(ctx, mode, onBack)
}
