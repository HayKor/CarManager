package ru.haykor.carmanager.feature.search.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import ru.haykor.carmanager.Res
import ru.haykor.carmanager.core.designsystem.CmSegmentedGap
import ru.haykor.carmanager.core.designsystem.cmSegmentShape
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.PlateCountry
import ru.haykor.carmanager.core.domain.ServiceCategoryId
import ru.haykor.carmanager.core.domain.ServiceRecordId
import ru.haykor.carmanager.core.domain.model.Car
import ru.haykor.carmanager.core.domain.model.CarSearchResult
import ru.haykor.carmanager.core.domain.model.ServiceCategory
import ru.haykor.carmanager.core.domain.model.ServiceRecord
import ru.haykor.carmanager.core.domain.model.ServiceRecordDetails
import ru.haykor.carmanager.core.presentation.coroutines.ObserveAsEvents
import ru.haykor.carmanager.core.ui.CarCard
import ru.haykor.carmanager.core.ui.SearchBar
import ru.haykor.carmanager.core.ui.ServiceRecordRow
import ru.haykor.carmanager.core.ui.theme.AppTheme
import ru.haykor.carmanager.core.ui.toUiText
import ru.haykor.carmanager.feature.search.component.SearchComponent
import ru.haykor.carmanager.feature.search.component.SearchEvent
import ru.haykor.carmanager.feature.search.component.SearchIntent
import ru.haykor.carmanager.feature.search.component.SearchState
import ru.haykor.carmanager.feature.search.component.SearchState.Content
import ru.haykor.carmanager.search_empty_history
import ru.haykor.carmanager.search_empty_on_query
import ru.haykor.carmanager.search_hint

// region composables
@Composable
fun SearchScreen(component: SearchComponent, modifier: Modifier = Modifier) {
    val state by component.state.subscribeAsState()

    val context = LocalContext.current
    ObserveAsEvents(component.events) { event ->
        when (event) {
            is SearchEvent.Failed -> {
                Toast.makeText(context, getString(event.error.toUiText()), Toast.LENGTH_SHORT).show()
            }
        }
    }

    SearchContent(
        state = state,
        onIntent = component::accept,
        modifier = modifier
    )
}

@Composable
internal fun SearchContent(state: SearchState, onIntent: (SearchIntent) -> Unit, modifier: Modifier = Modifier) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    SearchBar(
                        query = state.query,
                        onQueryChange = { onIntent(SearchIntent.QueryChanged(it)) },
                        onClearQuery = { onIntent(SearchIntent.ClearQueryClicked) },
                        placeholder = stringResource(Res.string.search_hint)
                    )
                }
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val content = state.content) {
                Content.Loading -> Unit

                is Content.Results -> SearchBody(
                    query = state.query,
                    groups = content.groups,
                    onIntent = onIntent
                )
            }
        }
    }
}

@Composable
private fun SearchBody(
    query: String,
    groups: List<CarSearchResult>,
    onIntent: (SearchIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        groups.isEmpty() && query.isBlank() -> SearchEmpty(
            text = stringResource(Res.string.search_empty_history),
            modifier = modifier
        )

        groups.isEmpty() -> SearchEmpty(
            text = stringResource(Res.string.search_empty_on_query, query.trim()),
            modifier = modifier
        )

        else -> LazyColumn(modifier = modifier.fillMaxSize()) {
            groups.forEach { group ->
                item(key = "car_${group.car.id.value}") {
                    CarCard(
                        car = group.car,
                        onClick = { onIntent(SearchIntent.CarClicked(group.car.id)) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                itemsIndexed(group.records, key = { _, record -> record.record.id.value }) { index, record ->
                    ServiceRecordRow(
                        record = record,
                        shape = cmSegmentShape(index, group.records.size),
                        onClick = { onIntent(SearchIntent.RecordClicked(record.record.id)) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = CmSegmentedGap / 2)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchEmpty(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
    }
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

private val previewRecord = ServiceRecordDetails(
    record = ServiceRecord(
        id = ServiceRecordId.random(),
        carId = previewCar.id,
        atMileage = 120_000,
        price = 1500,
        categoryId = ServiceCategoryId.random(),
        note = "Replaced oil and oil filter",
        photoUri = null,
        date = 0L,
        createdAt = 0L,
        updatedAt = 0L
    ),
    category = ServiceCategory(
        id = ServiceCategoryId.random(),
        name = "Oil Change"
    )
)

private val previewRecordOther = ServiceRecordDetails(
    record = previewRecord.record.copy(
        id = ServiceRecordId.random(),
        atMileage = 128_000,
        price = 4200,
        note = "Replaced brake pads"
    ),
    category = ServiceCategory(
        id = ServiceCategoryId.random(),
        name = "Brake Service"
    )
)

private val previewResults = Content.Results(
    groups = listOf(
        CarSearchResult(car = previewCar, records = listOf(previewRecord, previewRecordOther))
    )
)

@Preview(showBackground = true)
@Composable
private fun SearchContentPreview() = AppTheme {
    SearchContent(
        state = SearchState(query = "oil", content = previewResults),
        onIntent = {}
    )
}
// endregion
