package ru.haykor.carmanager.core.domain

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
@JvmInline
value class CarId(val value: String) {
    companion object {
        fun random() = CarId(Uuid.random().toString())
    }
}
