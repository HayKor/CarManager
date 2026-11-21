package ru.haykor.carmanager.core.data.local.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.zacsweers.metro.*
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.filesDir
import kotlinx.coroutines.Dispatchers
import ru.haykor.carmanager.core.data.local.AppDatabase
import ru.haykor.carmanager.core.data.local.dao.CarDao
import ru.haykor.carmanager.core.data.local.dao.ServiceCategoryDao
import ru.haykor.carmanager.core.data.local.dao.ServiceRecordDao

@ContributesTo(AppScope::class)
interface DatabaseModule {

    @Provides
    @FilesDir
    fun provideFilesDir(): PlatformFile = FileKit.filesDir

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

    @Provides
    fun provideServiceRecordDao(database: AppDatabase): ServiceRecordDao = database.serviceRecordDao()

    @Provides
    fun provideServiceCategoryDao(database: AppDatabase): ServiceCategoryDao = database.serviceCategoryDao()
}

@Qualifier
annotation class FilesDir
