package ru.haykor.carmanager.feature.garage.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.feature.garage.component.GarageComponent

@Composable
fun GarageScreen(component: GarageComponent, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = { component.onCarClick(CarId("101")) }
            ) {
                Text(
                    text = "Navigate to 101"
                )
            }
        }
    }
}
