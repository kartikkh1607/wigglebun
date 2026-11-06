package com.kartikkhandelwal.wigglebun.ui.navigation

import androidx.annotation.StringRes
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.ui.components.TabIcon
import com.kartikkhandelwal.wigglebun.ui.components.TabStyle
import com.kartikkhandelwal.wigglebun.ui.components.TabStyles
import com.kartikkhandelwal.wigglebun.ui.pet.PetSpec
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
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
    val style: TabStyle,
) {
    Home(HomeRoute, R.string.tab_home, TabStyles.Home),
    Collection(CollectionRoute, R.string.tab_collection, TabStyles.Collection),

    // The label is replaced by the active pet's name (see WigglebunNavHost); this is the fallback.
    Pet(PetRoute, R.string.pet_default_name, TabStyles.Pet),
    Settings(SettingsRoute, R.string.tab_settings, TabStyles.Settings),
    ;

    /** The Pet tab shows the pet that is with you (CLAUDE.md §6.6), so Pip's tab looks like Pip. */
    fun icon(activePet: PetSpec?): TabIcon = when (this) {
        Home -> TabIcon.Drawable(R.drawable.ic_tab_home, Dimens.TabHomeIcon)
        Collection -> TabIcon.Drawable(R.drawable.ic_tab_collection, Dimens.TabIcon)
        Pet -> TabIcon.Pet(activePet)
        Settings -> TabIcon.Drawable(R.drawable.ic_tab_settings, Dimens.TabIcon, selected = R.drawable.ic_tab_settings_selected)
    }
}
