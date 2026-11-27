package ru.haykor.carmanager.core.domain.search

import org.junit.Test
import kotlin.test.assertEquals

class SearchQueryTest {

    @Test
    fun `keeps unicode letters and digits`() {
        assertEquals(listOf("Лада", "Ves", "5W", "40"), SearchQuery.sanitizeWords("  Лада, Ves! 5W-40 "))
    }

    @Test
    fun `punctuation only yields empty list`() {
        assertEquals(emptyList(), SearchQuery.sanitizeWords("!!! ---"))
    }

    @Test
    fun `collapses any whitespace`() {
        assertEquals(listOf("Toyota", "Camry"), SearchQuery.sanitizeWords("Toyota\n\t  Camry"))
    }
}
