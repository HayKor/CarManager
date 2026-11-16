package ru.haykor.carmanager.core.domain

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
@JvmInline
value class ServiceCategoryId(val value: String) {
    companion object {
        fun random() = ServiceCategoryId(Uuid.random().toString())
    }
}
