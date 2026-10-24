package ru.haykor.carmanager.core.domain

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

@ContributesIntoSet(AppScope::class)
@Inject
class RussianPlateFormat : PlateFormat {

    override val country = PlateCountry.RU

    // Х000ХХ + 2–3 digit region; only the 12 Cyrillic letters that have Latin lookalikes
    private val regex = Regex("^[АВЕКМНОРСТУХ]\\d{3}[АВЕКМНОРСТУХ]{2}\\d{2,3}$")

    private val latinToCyrillic = "ABEKMHOPCTYX".zip("АВЕКМНОРСТУХ").toMap()

    override fun normalize(raw: String): String = buildString {
        for (c in raw) {
            if (c.isWhitespace() || c == '-') continue
            val upper = c.uppercaseChar()
            append(latinToCyrillic[upper] ?: upper)
        }
    }

    override fun isValid(normalized: String): Boolean = regex.matches(normalized)
}
