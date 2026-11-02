package ru.haykor.carmanager.feature.garage.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.haykor.carmanager.R
import ru.haykor.carmanager.core.presentation.ui.CarCard
import ru.haykor.carmanager.core.presentation.ui.rememberAnimatedCardShape
import ru.haykor.carmanager.feature.garage.component.GarageComponent
import ru.haykor.carmanager.feature.garage.component.GarageIntent
import ru.haykor.carmanager.feature.garage.component.GarageState
import ru.haykor.carmanager.ui.theme.AppTheme

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
                Icon(painterResource(R.drawable.plus), contentDescription = stringResource(R.string.garage_add_car))
            }
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        GarageBody(state = state, onIntent = onIntent, padding = padding)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    val isActive = isFocused || query.isNotEmpty()

    val horizontalPadding by animateDpAsState(
        targetValue = if (isActive) 4.dp else 16.dp,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "SearchPadding"
    )

    TopAppBar(
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background
        ),
        title = {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding)
                    .onFocusChanged { isFocused = it.isFocused },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isActive) {
                            IconButton(
                                onClick = {
                                    onClearQuery()
                                    focusManager.clearFocus()
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.chevron_left),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.search),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.width(4.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.garage_search_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }

                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = onClearQuery,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.close),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
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
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(content.items, key = { _, car -> car.id.value }) { index, car ->
                    val isFirst = index == 0
                    val isLast = index == content.items.lastIndex

                    val shape = rememberAnimatedCardShape(
                        isFirst = isFirst,
                        isLast = isLast,
                        smallRadius = 8.dp,
                        largeRadius = 32.dp
                    )

                    CarCard(
                        car = car,
                        shape = shape,
                        onClick = { onIntent(GarageIntent.CarClicked(car.id)) },
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .animateItem(
                                fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                placementSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
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
        Text(stringResource(R.string.garage_empty_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onAddClick) { Text(stringResource(R.string.garage_add_car)) }
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
        Text(stringResource(R.string.garage_empty_on_query, query), style = MaterialTheme.typography.titleLarge)
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
