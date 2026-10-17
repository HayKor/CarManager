package ru.haykor.carmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.retainedComponent
import dev.zacsweers.metro.createGraph
import ru.haykor.carmanager.di.AppGraph
import ru.haykor.carmanager.root.ui.RootContent
import ru.haykor.carmanager.ui.theme.AppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appGraph = createGraph<AppGraph>()
        val rootComponent = retainedComponent { ctx ->
            appGraph.rootComponentFactory.create(ctx)
        }
        enableEdgeToEdge()
        setContent {
            AppTheme {
                RootContent(
                    component = rootComponent,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
