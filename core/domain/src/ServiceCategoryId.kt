package ru.haykor.carmanager.core.domain

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ServiceCategoryId(val value: String) {
    companion object {
        fun random() = ServiceCategoryId(Uuid.random().toString())
    }
}
