package ru.haykor.carmanager.core.domain

import ru.haykor.carmanager.core.domain.model.FieldError

fun interface Validator {
    fun validate(input: String): FieldError?
}

operator fun Validator.plus(other: Validator): Validator = Validator { value ->
    this.validate(value) ?: other.validate(value)
}

fun required() = Validator { if (it.isEmpty()) FieldError.Required else null }

fun maxLength(max: Int) = Validator { if (it.length > max) FieldError.TooLong(max) else null }

fun matches(predicate: (String) -> Boolean) = Validator { if (predicate(it)) null else FieldError.Invalid }

fun optional(validator: Validator) = Validator { if (it.isEmpty()) null else validator.validate(it) }
