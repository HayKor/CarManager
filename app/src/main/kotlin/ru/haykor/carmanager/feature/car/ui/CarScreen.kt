package ru.haykor.carmanager.feature.car.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import ru.haykor.carmanager.R
import ru.haykor.carmanager.feature.car.component.CarComponent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarScreen(component: CarComponent, modifier: Modifier = Modifier) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "<Car Info>"
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = component::onBackClicked
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.chevron_left),
                            contentDescription = null
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Selected car id: ${component.id}")
        }
    }
}
