package ru.haykor.carmanager.core.domain

import ru.haykor.carmanager.core.domain.model.FieldError
import ru.haykor.carmanager.core.domain.model.Invalid
import ru.haykor.carmanager.core.domain.model.Required
import ru.haykor.carmanager.core.domain.model.TooLong

fun interface Validator<out E : FieldError> {
    fun validate(input: String): E?
}

operator fun <E : FieldError> Validator<E>.plus(other: Validator<E>): Validator<E> =
    Validator { value -> this.validate(value) ?: other.validate(value) }

fun required() = Validator { if (it.isEmpty()) Required else null }

fun maxLength(max: Int) = Validator { if (it.length > max) TooLong(max) else null }

fun matches(predicate: (String) -> Boolean) = Validator { if (predicate(it)) null else Invalid }

fun <E : FieldError> optional(validator: Validator<E>) =
    Validator { if (it.isEmpty()) null else validator.validate(it) }
