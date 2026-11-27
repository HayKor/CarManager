package ru.haykor.carmanager.feature.garage.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ru.haykor.carmanager.*
import ru.haykor.carmanager.core.designsystem.CmSegmentedGap
import ru.haykor.carmanager.core.designsystem.cmAnimateItem
import ru.haykor.carmanager.core.designsystem.cmSegmentShape
import ru.haykor.carmanager.core.ui.CarCard
import ru.haykor.carmanager.core.ui.SearchBar
import ru.haykor.carmanager.core.ui.sharedBounds
import ru.haykor.carmanager.core.ui.theme.AppTheme
import ru.haykor.carmanager.feature.garage.component.GarageComponent
import ru.haykor.carmanager.feature.garage.component.GarageIntent
import ru.haykor.carmanager.feature.garage.component.GarageState

// region composables
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
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            if (state.content !is GarageState.Content.EmptyGarage) {
                GarageTopBar(
                    query = state.query,
                    onQueryChange = { onIntent(GarageIntent.QueryChanged(it)) },
                    onClearQuery = { onIntent(GarageIntent.ClearQueryClicked) },
                    scrollBehavior = scrollBehavior
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onIntent(GarageIntent.AddCarClicked) }) {
                Icon(painterResource(Res.drawable.plus), contentDescription = stringResource(Res.string.garage_add_car))
            }
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        GarageBody(state = state, onIntent = onIntent, padding = padding)
    }
}

@Composable
fun GarageTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background
        ),
        title = {
            SearchBar(
                query = query,
                onQueryChange = onQueryChange,
                onClearQuery = onClearQuery,
                placeholder = stringResource(Res.string.search_hint)
            )
        },
        modifier = modifier
    )
}

@Composable
private fun GarageBody(
    state: GarageState,
    onIntent: (GarageIntent) -> Unit,
    padding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        when (val content = state.content) {
            GarageState.Content.Loading -> Unit

            is GarageState.Content.EmptySearch -> GarageEmptySearch(
                query = content.query,
                modifier = Modifier.padding(padding)
            )

            GarageState.Content.EmptyGarage -> GarageEmpty(
                onAddClick = { onIntent(GarageIntent.AddCarClicked) },
                modifier = Modifier.padding(padding)
            )

            is GarageState.Content.Cars -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = padding,
                verticalArrangement = Arrangement.spacedBy(CmSegmentedGap)
            ) {
                itemsIndexed(content.items, key = { _, car -> car.id.value }) { index, car ->
                    CarCard(
                        car = car,
                        shape = cmSegmentShape(index, content.items.size),
                        onClick = { onIntent(GarageIntent.CarClicked(car.id)) },
                        modifier = Modifier
                            .sharedBounds(key = car.id.value)
                            .padding(horizontal = 16.dp)
                            .cmAnimateItem()
                    )
                }
            }
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
        Text(stringResource(Res.string.garage_empty_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onAddClick) { Text(stringResource(Res.string.garage_add_car)) }
    }
}

@Composable
private fun GarageEmptySearch(query: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(Res.string.garage_empty_on_query, query), style = MaterialTheme.typography.titleLarge)
    }
}

// endregion

// region preview

@Preview(showBackground = true)
@Composable
private fun GarageEmptyPreview() = AppTheme {
    GarageEmpty(onAddClick = {}, modifier = Modifier.fillMaxSize())
}

// endregion
