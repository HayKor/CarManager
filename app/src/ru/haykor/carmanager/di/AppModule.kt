package ru.haykor.carmanager.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import kotlin.time.Clock

@ContributesTo(AppScope::class)
interface AppModule {
    @Provides
    fun provideClock(): Clock = Clock.System
}
