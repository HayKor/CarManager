package ru.haykor.carmanager.feature.main.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.arkivanov.decompose.extensions.compose.pages.ChildPages
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.haykor.carmanager.R
import ru.haykor.carmanager.feature.garage.ui.GarageScreen
import ru.haykor.carmanager.feature.main.component.MainComponent
import ru.haykor.carmanager.feature.main.component.MainTab

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
            }
        }
    }
}

internal data class TopLevelDestination(
    val tab: MainTab,
    @field:StringRes val title: Int,
    @field:DrawableRes val icon: Int
)

internal val topLevelDestinations: List<TopLevelDestination>
    @Composable
    get() = listOf(
        TopLevelDestination(
            tab = MainTab.Garage,
            title = R.string.title_garage,
            icon = R.drawable.garage_home
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
