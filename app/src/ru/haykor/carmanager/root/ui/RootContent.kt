package ru.haykor.carmanager.root.ui

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.experimental.stack.ChildStack
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.*
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.androidPredictiveBackAnimatableV2
import io.github.vinceglb.filekit.coil.addPlatformFileSupport
import ru.haykor.carmanager.core.domain.model.EditorMode
import ru.haykor.carmanager.core.ui.LocalAnimatedVisibilityScope
import ru.haykor.carmanager.core.ui.LocalSharedTransitionScope
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

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            Surface(
                modifier = modifier
            ) {
                ChildStack(
                    stack = component.childStack,
                    animation = stackAnimation(
                        predictiveBackParams = {
                            PredictiveBackParams(
                                backHandler = component.backHandler,
                                onBack = component::onBack,
                                animatable = ::androidPredictiveBackAnimatableV2
                            )
                        },
                        selector = { child, otherChild, direction, isPredictiveBack ->
                            val childInstance = child.instance
                            val otherChildInstance = otherChild.instance

                            when {
                                childInstance is RootComponent.Child.PhotoViewer ||
                                    otherChildInstance is RootComponent.Child.PhotoViewer
                                -> fade()

                                childInstance is RootComponent.Child.CarEditor &&
                                    childInstance.component.id == null
                                -> slide(orientation = Orientation.Vertical) +
                                    fade()

                                childInstance is RootComponent.Child.ServiceEditor &&
                                    childInstance.component.editorMode is EditorMode.Create
                                -> slide(orientation = Orientation.Vertical) +
                                    fade()

                                otherChildInstance is RootComponent.Child.CarEditor &&
                                    otherChildInstance.component.id == null
                                -> fade()

                                otherChildInstance is RootComponent.Child.ServiceEditor &&
                                    otherChildInstance.component.editorMode is EditorMode.Create
                                -> fade()

                                else -> slide() + fade()
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this@ChildStack) {
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
        }
    }
}
