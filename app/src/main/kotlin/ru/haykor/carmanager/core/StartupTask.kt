package ru.haykor.carmanager.core

interface StartupTask {
    suspend fun run()
}
