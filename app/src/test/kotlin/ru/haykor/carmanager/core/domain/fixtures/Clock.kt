package ru.haykor.carmanager.core.domain.fixtures

import kotlin.time.Clock
import kotlin.time.Instant

val clock = object : Clock {
    override fun now(): Instant = Instant.parse("2026-06-15T12:00:00Z")
}
