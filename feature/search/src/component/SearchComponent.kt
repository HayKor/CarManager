package ru.haykor.carmanager.feature.search.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.Flow
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.ServiceRecordId

interface SearchComponent {
    val state: Value<SearchState>
    val events: Flow<SearchEvent>

    fun accept(intent: SearchIntent)

    fun interface Factory {
        fun create(
            ctx: ComponentContext,
            onOpenCar: (CarId) -> Unit,
            onOpenRecord: (ServiceRecordId) -> Unit
        ): SearchComponent
    }
}
