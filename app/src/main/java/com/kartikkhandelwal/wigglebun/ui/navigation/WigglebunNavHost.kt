package com.kartikkhandelwal.wigglebun.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.ui.breaksession.BreakSessionScreenRoute
import com.kartikkhandelwal.wigglebun.ui.collection.CollectionPreviewStates
import com.kartikkhandelwal.wigglebun.ui.collection.CollectionScreen
import com.kartikkhandelwal.wigglebun.ui.collection.CollectionScreenRoute
import com.kartikkhandelwal.wigglebun.ui.components.TabBarItem
import com.kartikkhandelwal.wigglebun.ui.debug.DebugHomeEntry
import com.kartikkhandelwal.wigglebun.ui.debug.debugDestinations
import com.kartikkhandelwal.wigglebun.ui.components.WigglebunTabBar
import com.kartikkhandelwal.wigglebun.ui.home.HomeNavigation
import com.kartikkhandelwal.wigglebun.ui.home.HomePreviewStates
import com.kartikkhandelwal.wigglebun.ui.home.HomeScreen
import com.kartikkhandelwal.wigglebun.ui.home.HomeScreenRoute
import com.kartikkhandelwal.wigglebun.ui.pet.displayName
import com.kartikkhandelwal.wigglebun.ui.pet.profile.PetProfilePreviewStates
import com.kartikkhandelwal.wigglebun.ui.pet.profile.PetProfileScreen
import com.kartikkhandelwal.wigglebun.ui.pet.profile.PetProfileScreenRoute
import com.kartikkhandelwal.wigglebun.ui.settings.SettingsPreviewStates
import com.kartikkhandelwal.wigglebun.ui.settings.SettingsScreen
import com.kartikkhandelwal.wigglebun.ui.settings.SettingsScreenRoute
import com.kartikkhandelwal.wigglebun.ui.setup.HomeSetupPromptRoute
import com.kartikkhandelwal.wigglebun.ui.theme.Cream
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme

@Composable
fun WigglebunNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    /** Overridable so previews/screenshot tests can show a static, Hilt-free Home. */
    homeContent: @Composable (HomeNavigation) -> Unit = { HomeScreenRoute(it) },
    collectionContent: @Composable () -> Unit = { CollectionScreenRoute() },
    petContent: @Composable () -> Unit = { PetProfileScreenRoute() },
    settingsContent: @Composable () -> Unit = { SettingsScreenRoute() },
    /** Names the pet tab; null (still loading) leaves the label blank rather than show the wrong pet. */
    activePet: ActivePet? = ActivePet.Default,
    /** A break to open straight away (from the widget); [onStartBreakHandled] clears it. */
    startBreak: BreakType? = null,
    onStartBreakHandled: () -> Unit = {},
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val selectedTab = TopLevelTab.entries.firstOrNull { tab ->
        backStackEntry?.destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
    }
    val currentTab = selectedTab ?: TopLevelTab.Home
    // Full-screen destinations (break session, debug tools) hide the tab bar.
    val showTabBar = backStackEntry == null || selectedTab != null

    fun openTab(tab: TopLevelTab) {
        navController.navigate(tab.route) {
            // Standard bottom-tab behaviour: one back stack per tab, state restored on return.
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openBreak(type: BreakType) = navController.navigate(BreakSessionRoute(type.name)) { launchSingleTop = true }

    LaunchedEffect(startBreak) {
        if (startBreak != null) {
            openBreak(startBreak)
            onStartBreakHandled()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Cream,
        bottomBar = {
            if (showTabBar) {
                val petName = activePet?.displayName().orEmpty()
                WigglebunTabBar(
                    items = TopLevelTab.entries.map {
                        // The pet tab is named after the pet that is with you (CLAUDE.md §6.6).
                        val label = if (it == TopLevelTab.Pet) petName else stringResource(it.label)
                        TabBarItem(label, it.icon, it.tintIcon)
                    },
                    selectedIndex = currentTab.ordinal,
                    onSelect = { index -> openTab(TopLevelTab.entries[index]) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            // Edge-to-edge: screens handle the status bar themselves (Home's scene runs under it).
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            composable<HomeRoute> {
                homeContent(
                    HomeNavigation(
                        onOpenMenu = { openTab(TopLevelTab.Settings) },
                        onStartBreak = ::openBreak,
                        debugEntry = { DebugHomeEntry(navController) },
                        setupPrompt = { HomeSetupPromptRoute() },
                    ),
                )
            }
            composable<CollectionRoute> { collectionContent() }
            composable<PetRoute> { petContent() }
            composable<SettingsRoute> { settingsContent() }
            composable<BreakSessionRoute> { BreakSessionScreenRoute(onExit = { navController.popBackStack() }) }
            debugDestinations(navController)
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun WigglebunNavHostPreview() {
    WigglebunTheme {
        WigglebunNavHost(
            homeContent = { HomeScreen(HomePreviewStates.Tired, debugEntry = it.debugEntry) },
            collectionContent = { CollectionScreen(CollectionPreviewStates.OnlyBun) },
            petContent = { PetProfileScreen(PetProfilePreviewStates.EmptyWeek) },
            settingsContent = { SettingsScreen(SettingsPreviewStates.AllOff, {}) },
        )
    }
}
