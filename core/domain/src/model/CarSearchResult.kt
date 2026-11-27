package ru.haykor.carmanager.core.domain.model

data class CarSearchResult(val car: Car, val records: List<ServiceRecordDetails>)
