package ru.haykor.carmanager.feature.main.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.pages.ChildPages
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ru.haykor.carmanager.Res
import ru.haykor.carmanager.core.ui.aboveSharedElements
import ru.haykor.carmanager.feature.garage.ui.GarageScreen
import ru.haykor.carmanager.feature.main.component.MainComponent
import ru.haykor.carmanager.feature.main.component.MainTab
import ru.haykor.carmanager.feature.search.ui.SearchScreen
import ru.haykor.carmanager.garage_home
import ru.haykor.carmanager.search
import ru.haykor.carmanager.title_garage
import ru.haykor.carmanager.title_search

@Composable
fun MainScreen(component: MainComponent, modifier: Modifier = Modifier) {
    val pages by component.pages.subscribeAsState()
    val selectedTab = pages.items.getOrNull(pages.selectedIndex)?.instance?.tab ?: MainTab.Garage

    Scaffold(
        bottomBar = {
            AppBottomBar(
                selected = selectedTab,
                onClick = component::onTabSelected
            )
        },
        contentWindowInsets = WindowInsets.navigationBars,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        ChildPages(
            pages = component.pages,
            onPageSelected = component::onPageSelected, // actually useless
            pager = { modifier, state, key, pageContent ->
                HorizontalPager(
                    state = state,
                    modifier = modifier,
                    key = key,
                    userScrollEnabled = false,
                    pageContent = pageContent
                )
            }, // no swipe but no manual state management. Win-win
            modifier = Modifier.padding(innerPadding)
        ) { _, child ->
            when (child) {
                is MainComponent.Child.Garage -> {
                    GarageScreen(child.component)
                }

                is MainComponent.Child.Search -> {
                    SearchScreen(child.component)
                }
            }
        }
    }
}

internal data class TopLevelDestination(val tab: MainTab, val title: StringResource, val icon: DrawableResource)

internal val topLevelDestinations: List<TopLevelDestination>
    @Composable
    get() = listOf(
        TopLevelDestination(
            tab = MainTab.Garage,
            title = Res.string.title_garage,
            icon = Res.drawable.garage_home
        ),

        TopLevelDestination(
            tab = MainTab.Search,
            title = Res.string.title_search,
            icon = Res.drawable.search
        )
    )

@Composable
internal fun AppBottomBar(selected: MainTab, onClick: (MainTab) -> Unit, modifier: Modifier = Modifier) {
    NavigationBar(
        modifier = modifier
    ) {
        topLevelDestinations.forEach { destination ->
            NavigationBarItem(
                selected = destination.tab == selected,
                onClick = { onClick(destination.tab) },
                icon = {
                    Icon(
                        painter = painterResource(destination.icon),
                        contentDescription = stringResource(destination.title)
                    )
                },
                label = {
                    Text(
                        text = stringResource(destination.title),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
        }
    }
}
