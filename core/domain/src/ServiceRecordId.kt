package ru.haykor.carmanager.core.domain

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ServiceRecordId(val value: String) {
    companion object {
        fun random() = ServiceRecordId(Uuid.random().toString())
    }
}
