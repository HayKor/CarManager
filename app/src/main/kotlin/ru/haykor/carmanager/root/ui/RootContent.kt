package ru.haykor.carmanager.root.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import ru.haykor.carmanager.feature.car.ui.CarScreen
import ru.haykor.carmanager.feature.carEditor.ui.CarEditorScreen
import ru.haykor.carmanager.feature.main.ui.MainScreen
import ru.haykor.carmanager.root.component.RootComponent

@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun RootContent(component: RootComponent, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
    ) {
        // Root surface so automatic colorscheme applied
        Children(
            stack = component.childStack,
            animation = predictiveBackAnimation(
                backHandler = component.backHandler,
                fallbackAnimation = stackAnimation(slide() + fade()),
                onBack = component::onBack
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
            }
        }
    }
}
