package ru.haykor.carmanager.feature.main.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.router.pages.Pages
import com.arkivanov.decompose.router.pages.PagesNavigation
import com.arkivanov.decompose.router.pages.childPages
import com.arkivanov.decompose.router.pages.select
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable
import ru.haykor.carmanager.feature.garage.component.GarageComponent

enum class MainTab {
    Garage
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
    }

    fun interface Factory {
        fun create(ctx: ComponentContext, garage: (ComponentContext) -> GarageComponent): MainComponent
    }
}

class DefaultMainComponent(
    componentContext: ComponentContext,
    private val garage: (ComponentContext) -> GarageComponent
) : MainComponent,
    ComponentContext by componentContext {

    private val pagesNavigation = PagesNavigation<Config>()

    private val childPagesValue: Value<ChildPages<Config, MainComponent.Child>> =
        childPages(
            source = pagesNavigation,
            serializer = Config.serializer(),
            initialPages = {
                Pages(
                    items = MainTab.entries.map { it.toConfig() },
                    selectedIndex = MainTab.Garage.ordinal
                )
            },
            handleBackButton = false,
            childFactory = ::child
        )

    override val pages: Value<ChildPages<*, MainComponent.Child>> = childPagesValue

    override fun onTabSelected(tab: MainTab) {
        selectPage(tab.ordinal)
    }

    override fun onPageSelected(index: Int) {
        selectPage(index)
    }

    private fun selectPage(index: Int) {
        val pages = childPagesValue.value
        if (index in pages.items.indices && index != pages.selectedIndex) {
            pagesNavigation.select(index)
        }
    }

    private fun child(config: Config, ctx: ComponentContext) = when (config) {
        Config.Garage -> MainComponent.Child.Garage(garage(ctx))
    }

    private fun MainTab.toConfig() = when (this) {
        MainTab.Garage -> Config.Garage
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Garage : Config
    }
}
