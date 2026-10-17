package ru.haykor.carmanager.core.domain

import kotlin.time.Clock
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.haykor.carmanager.core.domain.model.CarFormInput
import ru.haykor.carmanager.core.domain.model.Field
import ru.haykor.carmanager.core.domain.model.FieldError

class CarDraftParserTest {

    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-06-15T12:00:00Z")
    }

    private val parser = CarDraftParser(PlateFormats(setOf(RussianPlateFormat())), clock)

    private val validInput = CarFormInput(
        brand = "Toyota",
        model = "Camry",
        year = "2018",
        mileage = "120000"
    )

    @Test
    fun `valid input is parsed into trimmed draft`() {
        val result = parser.parse(validInput.copy(brand = "  Toyota ", plate = "a123bc 77"))

        result as CarParseResult.Valid
        assertEquals("Toyota", result.data.brand)
        assertEquals(2018, result.data.year)
        assertEquals(120_000L, result.data.mileage)
        assertEquals("А123ВС77", result.data.plate)
        assertEquals(null, result.data.vin)
    }

    @Test
    fun `empty form reports required fields only`() {
        val errors = parser.validate(CarFormInput())

        assertEquals(
            mapOf(
                Field.BRAND to FieldError.Required,
                Field.MODEL to FieldError.Required,
                Field.YEAR to FieldError.Required,
                Field.MILEAGE to FieldError.Required
            ),
            errors
        )
    }

    @Test
    fun `whitespace only brand is required`() {
        assertEquals(FieldError.Required, parser.validate(validInput.copy(brand = "   "))[Field.BRAND])
    }

    @Test
    fun `too long brand`() {
        assertEquals(FieldError.TooLong(30), parser.validate(validInput.copy(brand = "x".repeat(31)))[Field.BRAND])
    }

    @Test
    fun `year range depends on clock`() {
        assertTrue(Field.YEAR !in parser.validate(validInput.copy(year = "2027")))
        assertEquals(FieldError.Invalid, parser.validate(validInput.copy(year = "2028"))[Field.YEAR])
        assertEquals(FieldError.Invalid, parser.validate(validInput.copy(year = "1899"))[Field.YEAR])
        assertEquals(FieldError.Invalid, parser.validate(validInput.copy(year = "abc"))[Field.YEAR])
    }

    @Test
    fun `negative mileage is invalid`() {
        assertEquals(FieldError.Invalid, parser.validate(validInput.copy(mileage = "-1"))[Field.MILEAGE])
    }

    @Test
    fun `vin rejects I O Q`() {
        assertTrue(Field.VIN !in parser.validate(validInput.copy(vin = "jtdbe32k920123456")))
        assertEquals(FieldError.Invalid, parser.validate(validInput.copy(vin = "JTDBE32K92012345O"))[Field.VIN])
        assertEquals(FieldError.Invalid, parser.validate(validInput.copy(vin = "JTDBE32K"))[Field.VIN])
    }

    @Test
    fun `plate accepts latin lookalikes and 3 digit region`() {
        assertTrue(Field.PLATE !in parser.validate(validInput.copy(plate = "X777XX 777")))
        assertTrue(Field.PLATE !in parser.validate(validInput.copy(plate = "о001ор-77")))
    }

    @Test
    fun `plate rejects letters without lookalikes`() {
        assertEquals(FieldError.Invalid, parser.validate(validInput.copy(plate = "Д123ВС77"))[Field.PLATE])
        assertEquals(FieldError.Invalid, parser.validate(validInput.copy(plate = "A12BC77"))[Field.PLATE])
    }
}
