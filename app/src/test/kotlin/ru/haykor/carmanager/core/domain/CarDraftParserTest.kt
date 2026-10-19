package ru.haykor.carmanager.core.domain

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import ru.haykor.carmanager.core.domain.fixtures.allRequiredErrors
import ru.haykor.carmanager.core.domain.fixtures.parser
import ru.haykor.carmanager.core.domain.model.CarFormInput
import ru.haykor.carmanager.core.domain.model.Field
import ru.haykor.carmanager.core.domain.model.FieldError

class CarDraftParserTest {

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
        assertEquals("Toyota", result.draft.brand)
        assertEquals(2018, result.draft.year)
        assertEquals(120_000L, result.draft.mileage)
        assertEquals("А123ВС77", result.draft.plate)
        assertEquals(null, result.draft.vin)
    }

    @Test
    fun `empty form reports required fields only`() {
        val errors = parser.validate(CarFormInput())

        assertEquals(
            allRequiredErrors,
            errors
        )
    }

    @Test
    fun `whitespace only brand is required`() {
        assertEquals(
            FieldError.Required,
            parser.validate(validInput.copy(brand = "   "))[Field.BRAND]
        )
    }

    @Test
    fun `too long brand`() {
        assertEquals(
            FieldError.TooLong(30),
            parser.validate(validInput.copy(brand = "x".repeat(31)))[Field.BRAND]
        )
    }

    @Test
    fun `year range depends on clock`() {
        assertTrue(Field.YEAR !in parser.validate(validInput.copy(year = "2027")))
        assertEquals(
            FieldError.Invalid,
            parser.validate(validInput.copy(year = "2028"))[Field.YEAR]
        )
        assertEquals(
            FieldError.Invalid,
            parser.validate(validInput.copy(year = "1899"))[Field.YEAR]
        )
        assertEquals(
            FieldError.Invalid,
            parser.validate(validInput.copy(year = "abc"))[Field.YEAR]
        )
    }

    @Test
    fun `negative mileage is invalid`() {
        assertEquals(
            FieldError.Invalid,
            parser.validate(validInput.copy(mileage = "-1"))[Field.MILEAGE]
        )
    }

    @Test
    fun `vin rejects I O Q`() {
        assertTrue(Field.VIN !in parser.validate(validInput.copy(vin = "jtdbe32k920123456")))
        assertEquals(
            FieldError.Invalid,
            parser.validate(validInput.copy(vin = "JTDBE32K92012345O"))[Field.VIN]
        )
        assertEquals(
            FieldError.Invalid,
            parser.validate(validInput.copy(vin = "JTDBE32K"))[Field.VIN]
        )
    }

    @Test
    fun `plate accepts latin lookalikes and 3 digit region`() {
        assertTrue(Field.PLATE !in parser.validate(validInput.copy(plate = "X777XX 777")))
        assertTrue(Field.PLATE !in parser.validate(validInput.copy(plate = "о001ор-77")))
    }

    @Test
    fun `plate rejects letters without lookalikes`() {
        assertEquals(
            FieldError.Invalid,
            parser.validate(validInput.copy(plate = "Д123ВС77"))[Field.PLATE]
        )
        assertEquals(
            FieldError.Invalid,
            parser.validate(validInput.copy(plate = "A12BC77"))[Field.PLATE]
        )
    }
}
