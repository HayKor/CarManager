package ru.haykor.carmanager.core.domain

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
@JvmInline
value class ServiceRecordId(val value: String) {
    companion object {
        fun random() = ServiceRecordId(Uuid.random().toString())
    }
}
