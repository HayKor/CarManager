package ru.haykor.carmanager.core.domain.search

object SearchQuery {

    private val disallowed = Regex("[^\\p{L}\\p{N} ]")
    private val whitespace = Regex("\\s+")

    fun sanitizeWords(query: String): List<String> = query
        .replace(disallowed, " ")
        .trim()
        .split(whitespace)
        .filter(String::isNotBlank)
}
