package ru.haykor.carmanager.root.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.experimental.stack.ChildStack
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.*
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.androidPredictiveBackAnimatableV2
import io.github.vinceglb.filekit.coil.addPlatformFileSupport
import ru.haykor.carmanager.feature.car.ui.CarScreen
import ru.haykor.carmanager.feature.carEditor.ui.CarEditorScreen
import ru.haykor.carmanager.feature.main.ui.MainScreen
import ru.haykor.carmanager.feature.serviceEditor.ui.ServiceEditorScreen
import ru.haykor.carmanager.photoviewer.ui.PhotoViewerScreen
import ru.haykor.carmanager.root.component.RootComponent

@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun RootContent(component: RootComponent, modifier: Modifier = Modifier) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { addPlatformFileSupport() }
            .build()
    }

    Surface(
        modifier = modifier
    ) {
        // Root surface so automatic colorscheme applied
        ChildStack(
            stack = component.childStack,
            animation = stackAnimation(
                animator = slide() + fade(),
                predictiveBackParams = {
                    PredictiveBackParams(
                        backHandler = component.backHandler,
                        onBack = component::onBack,
                        animatable = ::androidPredictiveBackAnimatableV2
                    )
                }
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            when (val child = it.instance) {
                is RootComponent.Child.Main -> {
                    MainScreen(child.component)
                }

                is RootComponent.Child.Car -> {
                    CarScreen(child.component)
                }

                is RootComponent.Child.CarEditor -> {
                    CarEditorScreen(child.component)
                }

                is RootComponent.Child.ServiceEditor -> {
                    ServiceEditorScreen(child.component)
                }

                is RootComponent.Child.PhotoViewer -> {
                    PhotoViewerScreen(child.component)
                }
            }
        }
    }
}
