package ru.haykor.carmanager.core.domain

import dev.zacsweers.metro.Inject

enum class PlateCountry { RU }

interface PlateFormat {
    val country: PlateCountry
    fun normalize(raw: String): String
    fun isValid(normalized: String): Boolean
}

@Inject
class PlateFormats(formats: Set<PlateFormat>) {
    private val byCountry = formats.associateBy { it.country }
    fun of(country: PlateCountry): PlateFormat = byCountry.getValue(country)
}
