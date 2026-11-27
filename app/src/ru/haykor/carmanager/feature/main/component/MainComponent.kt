package ru.haykor.carmanager.feature.main.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.value.Value
import ru.haykor.carmanager.feature.garage.component.GarageComponent
import ru.haykor.carmanager.feature.search.component.SearchComponent

enum class MainTab {
    Garage,
    Search
}

interface MainComponent {
    val pages: Value<ChildPages<*, Child>>

    fun onTabSelected(tab: MainTab)
    fun onPageSelected(index: Int)

    sealed interface Child {
        val tab: MainTab

        data class Garage(val component: GarageComponent) : Child {
            override val tab = MainTab.Garage
        }

        data class Search(val component: SearchComponent) : Child {
            override val tab = MainTab.Search
        }
    }

    fun interface Factory {
        fun create(
            ctx: ComponentContext,
            garage: (ComponentContext) -> GarageComponent,
            search: (ComponentContext) -> SearchComponent
        ): MainComponent
    }
}
