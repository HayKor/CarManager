package ru.haykor.carmanager.feature.search.component

import org.junit.Test
import ru.haykor.carmanager.core.domain.fixtures.aCar
import ru.haykor.carmanager.core.domain.model.CarSearchResult
import ru.haykor.carmanager.feature.search.component.SearchState.Content
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SearchReducerTest {

    private val result = CarSearchResult(car = aCar(), records = emptyList())

    @Test
    fun `query change updates query`() {
        val state = SearchReducer.reduce(SearchState(), SearchMutation.QueryChanged("Cam"))

        assertEquals("Cam", state.query)
        assertEquals(Content.Loading, state.content)
    }

    @Test
    fun `results replace content`() {
        val state = SearchReducer.reduce(SearchState(), SearchMutation.ResultsLoaded(listOf(result)))

        assertEquals(Content.Results(listOf(result)), state.content)
    }

    @Test
    fun `load failed from loading becomes empty results`() {
        val state = SearchReducer.reduce(SearchState(content = Content.Loading), SearchMutation.LoadFailed)

        assertEquals(Content.Results(emptyList()), state.content)
    }

    @Test
    fun `load failed keeps existing results`() {
        val content = Content.Results(listOf(result))
        val state = SearchReducer.reduce(SearchState(content = content), SearchMutation.LoadFailed)

        assertIs<Content.Results>(state.content)
        assertEquals(content, state.content)
    }
}
