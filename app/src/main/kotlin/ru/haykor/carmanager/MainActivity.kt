package ru.haykor.carmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.retainedComponent
import ru.haykor.carmanager.feature.car.component.DefaultCarComponent
import ru.haykor.carmanager.feature.garage.component.DefaultGarageComponent
import ru.haykor.carmanager.feature.main.component.DefaultMainComponent
import ru.haykor.carmanager.root.component.DefaultRootComponent
import ru.haykor.carmanager.root.component.RootComponent
import ru.haykor.carmanager.root.ui.RootContent
import ru.haykor.carmanager.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private val rootComponentFactory: RootComponent.Factory = { ctx ->
        DefaultRootComponent(
            ctx,
            mainFactory = { c, garage ->
                DefaultMainComponent(
                    c,
                    garage = garage
                )
            },
            garageFactory = { c, onOpenCar ->
                DefaultGarageComponent(c, onOpenCar)
            },
            carFactory = { c, carId, onBack ->
                DefaultCarComponent(c, carId, onBack)
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rootComponent = retainedComponent { ctx ->
            rootComponentFactory.create(ctx = ctx)
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
