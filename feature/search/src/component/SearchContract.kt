package ru.haykor.carmanager.feature.search.component

import androidx.compose.runtime.Stable
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceRecordId
import ru.haykor.carmanager.core.domain.error.SearchError
import ru.haykor.carmanager.core.domain.model.CarSearchResult
import ru.haykor.carmanager.feature.search.component.SearchState.Content

@Stable
data class SearchState(val query: String = "", val content: Content = Content.Loading) {
    sealed interface Content {
        data object Loading : Content
        data class Results(val groups: List<CarSearchResult>) : Content
    }
}

sealed interface SearchIntent {
    data class QueryChanged(val query: String) : SearchIntent
    data object ClearQueryClicked : SearchIntent
    data class CarClicked(val id: CarId) : SearchIntent
    data class RecordClicked(val id: ServiceRecordId) : SearchIntent
}

sealed interface SearchEvent {
    data class Failed(val error: SearchError) : SearchEvent
}

sealed interface SearchMutation {
    data object LoadingStarted : SearchMutation
    data class ResultsLoaded(val groups: List<CarSearchResult>) : SearchMutation
    data class QueryChanged(val query: String) : SearchMutation
    data object LoadFailed : SearchMutation
}

object SearchReducer {
    fun reduce(state: SearchState, mutation: SearchMutation): SearchState = when (mutation) {
        is SearchMutation.ResultsLoaded -> {
            state.copy(content = Content.Results(mutation.groups))
        }

        SearchMutation.LoadingStarted -> {
            state.copy(content = Content.Loading)
        }

        is SearchMutation.QueryChanged -> {
            state.copy(query = mutation.query)
        }

        SearchMutation.LoadFailed -> {
            when (state.content) {
                Content.Loading -> state.copy(content = Content.Results(emptyList()))
                is Content.Results -> state
            }
        }
    }
}
