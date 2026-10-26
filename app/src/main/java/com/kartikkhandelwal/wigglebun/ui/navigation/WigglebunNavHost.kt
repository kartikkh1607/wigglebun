package com.kartikkhandelwal.wigglebun.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kartikkhandelwal.wigglebun.ui.collection.CollectionScreen
import com.kartikkhandelwal.wigglebun.ui.components.TabBarItem
import com.kartikkhandelwal.wigglebun.ui.debug.DebugHomeEntry
import com.kartikkhandelwal.wigglebun.ui.debug.debugDestinations
import com.kartikkhandelwal.wigglebun.ui.components.WigglebunTabBar
import com.kartikkhandelwal.wigglebun.ui.home.HomePreviewStates
import com.kartikkhandelwal.wigglebun.ui.home.HomeScreen
import com.kartikkhandelwal.wigglebun.ui.home.HomeScreenRoute
import com.kartikkhandelwal.wigglebun.ui.pet.profile.PetProfileScreen
import com.kartikkhandelwal.wigglebun.ui.settings.SettingsScreen
import com.kartikkhandelwal.wigglebun.ui.theme.Cream
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme

@Composable
fun WigglebunNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    /** Overridable so previews/screenshot tests can show a static, Hilt-free Home. */
    homeContent: @Composable (debugEntry: @Composable () -> Unit) -> Unit = { debugEntry ->
        HomeScreenRoute(debugEntry = debugEntry)
    },
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = TopLevelTab.entries.firstOrNull { tab ->
        backStackEntry?.destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
    } ?: TopLevelTab.Home

    Scaffold(
        modifier = modifier,
        containerColor = Cream,
        bottomBar = {
            WigglebunTabBar(
                items = TopLevelTab.entries.map {
                    TabBarItem(stringResource(it.label), it.icon, it.tintIcon)
                },
                selectedIndex = currentTab.ordinal,
                onSelect = { index ->
                    navController.navigate(TopLevelTab.entries[index].route) {
                        // Standard bottom-tab behaviour: one back stack per tab, state restored on return.
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<HomeRoute> { homeContent { DebugHomeEntry(navController) } }
            composable<CollectionRoute> { CollectionScreen() }
            composable<PetRoute> { PetProfileScreen() }
            composable<SettingsRoute> { SettingsScreen() }
            debugDestinations(navController)
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun WigglebunNavHostPreview() {
    WigglebunTheme {
        WigglebunNavHost(homeContent = { HomeScreen(HomePreviewStates.Tired, debugEntry = it) })
    }
}
