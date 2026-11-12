package ru.haykor.carmanager.core.domain

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import ru.haykor.carmanager.core.domain.fixtures.allRequiredErrors
import ru.haykor.carmanager.core.domain.fixtures.carParser
import ru.haykor.carmanager.core.domain.model.CarField
import ru.haykor.carmanager.core.domain.model.CarFormInput
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
        val result = carParser.parse(validInput.copy(brand = "  Toyota ", plate = "a123bc 77"))

        result as CarParseResult.Valid
        assertEquals("Toyota", result.draft.brand)
        assertEquals(2018, result.draft.year)
        assertEquals(120_000L, result.draft.mileage)
        assertEquals("А123ВС77", result.draft.plate)
        assertEquals(null, result.draft.vin)
    }

    @Test
    fun `empty form reports required fields only`() {
        val errors = carParser.validate(CarFormInput())

        assertEquals(
            allRequiredErrors,
            errors
        )
    }

    @Test
    fun `whitespace only brand is required`() {
        assertEquals(
            FieldError.Required,
            carParser.validate(validInput.copy(brand = "   "))[CarField.BRAND]
        )
    }

    @Test
    fun `too long brand`() {
        assertEquals(
            FieldError.TooLong(30),
            carParser.validate(validInput.copy(brand = "x".repeat(31)))[CarField.BRAND]
        )
    }

    @Test
    fun `year range depends on clock`() {
        assertTrue(CarField.YEAR !in carParser.validate(validInput.copy(year = "2027")))
        assertEquals(
            FieldError.Invalid,
            carParser.validate(validInput.copy(year = "2028"))[CarField.YEAR]
        )
        assertEquals(
            FieldError.Invalid,
            carParser.validate(validInput.copy(year = "1899"))[CarField.YEAR]
        )
        assertEquals(
            FieldError.Invalid,
            carParser.validate(validInput.copy(year = "abc"))[CarField.YEAR]
        )
    }

    @Test
    fun `negative mileage is invalid`() {
        assertEquals(
            FieldError.Invalid,
            carParser.validate(validInput.copy(mileage = "-1"))[CarField.MILEAGE]
        )
    }

    @Test
    fun `vin rejects I O Q`() {
        assertTrue(CarField.VIN !in carParser.validate(validInput.copy(vin = "jtdbe32k920123456")))
        assertEquals(
            FieldError.Invalid,
            carParser.validate(validInput.copy(vin = "JTDBE32K92012345O"))[CarField.VIN]
        )
        assertEquals(
            FieldError.Invalid,
            carParser.validate(validInput.copy(vin = "JTDBE32K"))[CarField.VIN]
        )
    }

    @Test
    fun `plate accepts latin lookalikes and 3 digit region`() {
        assertTrue(CarField.PLATE !in carParser.validate(validInput.copy(plate = "X777XX 777")))
        assertTrue(CarField.PLATE !in carParser.validate(validInput.copy(plate = "о001ор-77")))
    }

    @Test
    fun `plate rejects letters without lookalikes`() {
        assertEquals(
            FieldError.Invalid,
            carParser.validate(validInput.copy(plate = "Д123ВС77"))[CarField.PLATE]
        )
        assertEquals(
            FieldError.Invalid,
            carParser.validate(validInput.copy(plate = "A12BC77"))[CarField.PLATE]
        )
    }
}
