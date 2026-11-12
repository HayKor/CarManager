package ru.haykor.carmanager.core.domain.fixtures

import ru.haykor.carmanager.core.domain.CarDraftParser
import ru.haykor.carmanager.core.domain.PlateFormats
import ru.haykor.carmanager.core.domain.RussianPlateFormat

internal val carParser = CarDraftParser(PlateFormats(setOf(RussianPlateFormat())), clock)
