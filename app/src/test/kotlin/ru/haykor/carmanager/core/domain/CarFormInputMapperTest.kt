package ru.haykor.carmanager.core.domain

import kotlin.test.assertEquals
import org.junit.Test
import ru.haykor.carmanager.core.domain.fixtures.aCar
import ru.haykor.carmanager.core.domain.mapper.toFormInput
import ru.haykor.carmanager.core.domain.model.Car
import ru.haykor.carmanager.core.domain.model.CarFormInput

class CarFormInputMapperTest {

    @Test
    fun `maps all fields when all populated`() {
        val car = Car(
            id = CarId.random(),
            brand = "BMW",
            model = "X5",
            year = 2020,
            note = "Special Note",
            vin = "WBA123456789",
            plateCountry = PlateCountry.RU,
            plate = "C123OM77",
            mileage = 45000L,
            photoUri = "file:///photos/1.jpg",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val expected = CarFormInput(
            brand = "BMW",
            model = "X5",
            year = "2020",
            note = "Special Note",
            vin = "WBA123456789",
            plateCountry = PlateCountry.RU,
            plate = "C123OM77",
            mileage = "45000",
            photoUri = "file:///photos/1.jpg"
        )

        assertEquals(expected, car.toFormInput())
    }

    @Test
    fun `year & mileage mapped correctly`() {
        val car = aCar()
        val formInput = car.toFormInput()
        assertEquals("2010", formInput.year)
        assertEquals("100000", formInput.mileage)
    }

    @Test
    fun `null vin mapped to empty`() {
        val car = aCar().copy(vin = null)
        val formInput = car.toFormInput()
        assertEquals("", formInput.vin)
    }

    @Test
    fun `null plateCountry mapped to default`() {
        val car = aCar().copy(plateCountry = null)
        val formInput = car.toFormInput()
        assertEquals(PlateCountry.RU, formInput.plateCountry)
    }
}
