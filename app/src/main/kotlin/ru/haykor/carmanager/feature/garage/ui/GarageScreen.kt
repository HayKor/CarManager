package ru.haykor.carmanager.feature.garage.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.haykor.carmanager.R
import ru.haykor.carmanager.core.domain.model.Car
import ru.haykor.carmanager.feature.garage.component.GarageComponent
import ru.haykor.carmanager.feature.garage.component.GarageIntent
import ru.haykor.carmanager.feature.garage.component.GarageState

@Composable
fun GarageScreen(component: GarageComponent, modifier: Modifier = Modifier) {
    val state by component.state.subscribeAsState()

    GarageContent(
        state = state,
        onIntent = component::accept,
        modifier = modifier
    )
}

@Composable
internal fun GarageContent(state: GarageState, onIntent: (GarageIntent) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { onIntent(GarageIntent.AddCarClicked) }) {
                Icon(painterResource(R.drawable.plus), contentDescription = stringResource(R.string.garage_add_car))
            }
        },
        contentWindowInsets = WindowInsets(),
        modifier = modifier
    ) { padding ->
        when (val content = state.content) {
            GarageState.Content.Loading -> Unit

            GarageState.Content.Empty -> GarageEmpty(
                onAddClick = { onIntent(GarageIntent.AddCarClicked) },
                modifier = Modifier.padding(padding)
            )

            is GarageState.Content.Cars -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = padding,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(content.items, key = { it.id.value }) { car ->
                    CarCard(
                        car = car,
                        onClick = { onIntent(GarageIntent.CarClicked(car.id)) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CarCard(car: Car, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${car.brand} ${car.model}", style = MaterialTheme.typography.titleMedium)
            car.plate?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Text(
                stringResource(R.string.garage_mileage, car.mileage),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun GarageEmpty(onAddClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.garage_empty_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAddClick) { Text(stringResource(R.string.garage_add_car)) }
    }
}
