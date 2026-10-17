package ru.haykor.carmanager.core.data.local.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import ru.haykor.carmanager.core.data.local.AppDatabase
import ru.haykor.carmanager.core.data.local.dao.CarDao

@ContributesTo(AppScope::class)
interface DatabaseModule {
    @Provides
    @SingleIn(AppScope::class)
    fun provideAppDatabase(context: Context): AppDatabase {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath("app.db")
        return Room.databaseBuilder<AppDatabase>(
            context = appContext,
            name = dbFile.absolutePath
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    @Provides
    fun provideCarDao(database: AppDatabase): CarDao = database.carDao()
}
