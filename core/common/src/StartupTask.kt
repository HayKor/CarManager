package ru.haykor.carmanager.core.common

interface StartupTask {
    suspend fun run()
}
