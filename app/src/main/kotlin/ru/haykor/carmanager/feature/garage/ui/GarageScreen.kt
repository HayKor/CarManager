package ru.haykor.carmanager.feature.garage.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import ru.haykor.carmanager.R
import ru.haykor.carmanager.core.common.ComprehensionDebt
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.PlateCountry
import ru.haykor.carmanager.core.domain.model.Car
import ru.haykor.carmanager.core.presentation.ui.LicensePlate
import ru.haykor.carmanager.core.presentation.ui.formatMileage
import ru.haykor.carmanager.core.presentation.ui.title
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
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    Scaffold(
        topBar = {
            if (state.content !is GarageState.Content.EmptyGarage) {
                GarageSearchBar(
                    query = state.query,
                    onQueryChange = { onIntent(GarageIntent.QueryChanged(it)) },
                    onQueryCleared = { onIntent(GarageIntent.ClearQueryClicked) },
                    scrollBehavior = scrollBehavior,
                    results = { GarageBody(state, onIntent, PaddingValues()) }
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
fun GarageSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onQueryCleared: () -> Unit,
    scrollBehavior: SearchBarScrollBehavior,
    modifier: Modifier = Modifier,
    results: @Composable ColumnScope.() -> Unit
) {
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState(initialText = query)
    val scope = rememberCoroutineScope()

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .distinctUntilChanged()
            .collect(onQueryChange)
    }

    val inputField = @Composable {
        SearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = { scope.launch { searchBarState.animateToCollapsed() } },
            placeholder = { Text("Search") },
            leadingIcon = {
                if (searchBarState.currentValue == SearchBarValue.Expanded) {
                    IconButton(onClick = { scope.launch { searchBarState.animateToCollapsed() } }) {
                        Icon(painterResource(R.drawable.chevron_left), null)
                    }
                } else {
                    Icon(painterResource(R.drawable.search), contentDescription = null)
                }
            },
            trailingIcon = {
                if (textFieldState.text.isNotEmpty()) {
                    IconButton(onClick = {
                        textFieldState.clearText()
                        onQueryCleared()
                    }) {
                        Icon(painterResource(R.drawable.close), null)
                    }
                }
            }
        )
    }

    AppBarWithSearch(
        state = searchBarState,
        inputField = inputField,
        scrollBehavior = scrollBehavior,
        modifier = modifier
    )
    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = inputField,
        colors = SearchBarDefaults.colors(
            containerColor = MaterialTheme.colorScheme.background,
            dividerColor = Color.Transparent
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        content = results
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

@Composable
private fun CarCard(
    car: Car,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.elevatedShape
) {
    ElevatedCard(
        onClick = onClick,
        shape = shape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            CarAvatar()

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = car.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.mileage_km, formatMileage(car.mileage)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                car.plate?.let {
                    Spacer(Modifier.height(8.dp))
                    LicensePlate(plate = it)
                }
            }

            Icon(
                painterResource(R.drawable.keyboard_arrow_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CarAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(R.drawable.directions_car),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

// endregion

// region util

// Стандартная выразительная пружина M3 Expressive для изменения формы и размеров
@ComprehensionDebt
val ExpressiveShapeSpring = spring<Dp>(
    dampingRatio = Spring.DampingRatioMediumBouncy, // Дает фирменный легкий отскок M3
    stiffness = Spring.StiffnessMediumLow
)

@ComprehensionDebt
@Composable
fun rememberAnimatedCardShape(
    isFirst: Boolean,
    isLast: Boolean,
    smallRadius: Dp = 8.dp,
    largeRadius: Dp = 32.dp
): Shape {
    val targetTop = if (isFirst) largeRadius else smallRadius
    val targetBottom = if (isLast) largeRadius else smallRadius

    // Каждая пара углов интерполируется независимо по спецификации Expressive Spring
    val animatedTopStart by animateDpAsState(
        targetValue = targetTop,
        animationSpec = ExpressiveShapeSpring,
        label = "TopStartCorner"
    )
    val animatedTopEnd by animateDpAsState(
        targetValue = targetTop,
        animationSpec = ExpressiveShapeSpring,
        label = "TopEndCorner"
    )
    val animatedBottomEnd by animateDpAsState(
        targetValue = targetBottom,
        animationSpec = ExpressiveShapeSpring,
        label = "BottomEndCorner"
    )
    val animatedBottomStart by animateDpAsState(
        targetValue = targetBottom,
        animationSpec = ExpressiveShapeSpring,
        label = "BottomStartCorner"
    )

    return RoundedCornerShape(
        topStart = animatedTopStart,
        topEnd = animatedTopEnd,
        bottomEnd = animatedBottomEnd,
        bottomStart = animatedBottomStart
    )
}

// endregion

// region preview
private val previewCar = Car(
    id = CarId.random(),
    brand = "Nissan",
    model = "Qashqai",
    year = 2010,
    note = "My family car",
    vin = "SJNFAAJ10U1234567",
    plateCountry = PlateCountry.RU,
    plate = "С123ОМ77",
    mileage = 128_400,
    photoUri = null,
    createdAt = 0L,
    updatedAt = 0L
)

@Preview(showBackground = true)
@Composable
private fun CarCardPreview() = AppTheme {
    CarCard(
        car = previewCar,
        onClick = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun GarageEmptyPreview() = AppTheme {
    GarageEmpty(onAddClick = {}, modifier = Modifier.fillMaxSize())
}
