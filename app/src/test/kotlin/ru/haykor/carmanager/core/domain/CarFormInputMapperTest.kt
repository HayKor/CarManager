package ru.haykor.carmanager.core.domain

import org.junit.Assert
import org.junit.Test
import ru.haykor.carmanager.core.domain.fixtures.aCar
import ru.haykor.carmanager.core.domain.mapper.toFormInput
import ru.haykor.carmanager.core.domain.model.Car
import ru.haykor.carmanager.core.domain.model.CarFormInput

class CarFormInputMapperTest {

    @Test
    fun `year & mileage mapped correctly`() {
        val car = aCar()
        val formInput = car.toFormInput()
        Assert.assertEquals("2010", formInput.year)
        Assert.assertEquals("100000", formInput.mileage)
    }

    @Test
    fun `null vin mapped to empty`() {
        val car = aCar().copy(vin = null)
        val formInput = car.toFormInput()
        Assert.assertEquals("", formInput.vin)
    }

    @Test
    fun `null plateCountry mapped to default`() {
        val car = aCar().copy(plateCountry = null)
        val formInput = car.toFormInput()
        Assert.assertEquals(PlateCountry.RU, formInput.plateCountry)
    }
}
