package ru.haykor.carmanager.feature.car.domain

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class CarId(val value: String) {
    companion object {
        fun random() = CarId(Uuid.random().toString())
    }
}
