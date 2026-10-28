package com.kartikkhandelwal.wigglebun.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object CollectionRoute

@Serializable
data object PetRoute

@Serializable
data object SettingsRoute

/** Full-screen guided break (no tab bar). [type] is a BreakType name. */
@Serializable
data class BreakSessionRoute(val type: String) {
    val breakType: BreakType get() = BreakType.fromName(type) ?: BreakType.STAND_UP
}

/** The four bottom tabs, in display order. */
enum class TopLevelTab(
    val route: Any,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    val tintIcon: Boolean = true,
) {
    Home(HomeRoute, R.string.tab_home, R.drawable.ic_tab_home),
    Collection(CollectionRoute, R.string.tab_collection, R.drawable.ic_tab_collection),

    // The label is replaced by the active pet's name (see WigglebunNavHost); this is the fallback.
    Pet(PetRoute, R.string.pet_default_name, R.drawable.ic_tab_pet, tintIcon = false),
    Settings(SettingsRoute, R.string.tab_settings, R.drawable.ic_tab_settings),
}
