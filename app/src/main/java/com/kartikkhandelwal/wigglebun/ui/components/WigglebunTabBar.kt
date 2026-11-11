package com.kartikkhandelwal.wigglebun.ui.components

import com.kartikkhandelwal.wigglebun.feedback.LocalFeel
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.ui.pet.PetSpec
import com.kartikkhandelwal.wigglebun.ui.pet.PetSpecs
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.TabCollectionBorder
import com.kartikkhandelwal.wigglebun.ui.theme.TabCollectionTint
import com.kartikkhandelwal.wigglebun.ui.theme.TabHomeBorder
import com.kartikkhandelwal.wigglebun.ui.theme.TabHomeTint
import com.kartikkhandelwal.wigglebun.ui.theme.TabPetBorder
import com.kartikkhandelwal.wigglebun.ui.theme.TabPetTint
import com.kartikkhandelwal.wigglebun.ui.theme.TabSettingsBorder
import com.kartikkhandelwal.wigglebun.ui.theme.TabSettingsTint
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme

/** A tab's colours: the selected pill, and the bar's top border while that tab is open. */
@Immutable
data class TabStyle(val tint: Color, val border: Color)

/** What a tab shows above its label. */
@Immutable
sealed interface TabIcon {
    /** A full-colour drawable, drawn untinted; [selected] swaps it while the tab is open. */
    data class Drawable(@DrawableRes val icon: Int, val size: Dp, @DrawableRes val selected: Int = icon) : TabIcon

    /** The pet that is with you, as a small still [PetView]; null while it loads (nothing drawn). */
    data class Pet(val spec: PetSpec?) : TabIcon
}

@Immutable
data class TabBarItem(val label: String, val icon: TabIcon, val style: TabStyle)

/** Bottom tab bar, matching the `<nav>` in design/ui_v2 (Main, Collection, Settings). */
@Composable
fun WigglebunTabBar(
    items: List<TabBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().background(Card)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(Dimens.TabBarBorder)
                .background(items[selectedIndex].style.border),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.TabBarHeight - Dimens.TabBarBorder)
                .padding(
                    start = Dimens.TabBarPaddingHorizontal,
                    end = Dimens.TabBarPaddingHorizontal,
                    top = Dimens.TabBarPaddingTop,
                    bottom = Dimens.TabBarPaddingBottom,
                )
                .selectableGroup(),
        ) {
            items.forEachIndexed { index, item ->
                TabBarButton(
                    item = item,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        // Edge-to-edge: the white bar continues under the system navigation bar.
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun TabBarButton(
    item: TabBarItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val feel = LocalFeel.current
    val indicator by animateColorAsState(
        targetValue = if (selected) item.style.tint else Color.Transparent,
        label = "tabIndicator",
    )
    Column(
        modifier = modifier
            .fillMaxHeight()
            .selectable(
                selected = selected,
                onClick = {
                    if (!selected) feel.segment() // re-tapping the open tab changes nothing
                    onClick()
                },
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.TabLabelGap),
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.TabIndicatorWidth, Dimens.TabIndicatorHeight)
                .background(indicator, WigglebunShapes.Pill)
                // The label below describes the tab.
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            when (val icon = item.icon) {
                is TabIcon.Drawable -> Image(
                    painter = painterResource(if (selected) icon.selected else icon.icon),
                    contentDescription = null,
                    modifier = Modifier.size(icon.size),
                )
                is TabIcon.Pet -> icon.spec?.let { spec ->
                    PetView(energy = 1f, mood = Mood.HAPPY, spec = spec, animated = false, modifier = Modifier.size(Dimens.TabPetIcon))
                }
            }
        }
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
            color = if (selected) Ink else InkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The four tabs' looks, shared by the app shell and the previews. */
object TabStyles {
    val Home = TabStyle(TabHomeTint, TabHomeBorder)
    val Collection = TabStyle(TabCollectionTint, TabCollectionBorder)
    val Pet = TabStyle(TabPetTint, TabPetBorder)
    val Settings = TabStyle(TabSettingsTint, TabSettingsBorder)
}

@Preview(showBackground = true, widthDp = 390)
@Composable
internal fun WigglebunTabBarPreview(pet: PetSpec = PetSpecs.Bun) {
    WigglebunTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.ListGap)) {
            val items = listOf(
                TabBarItem(stringResource(R.string.tab_home), TabIcon.Drawable(R.drawable.ic_tab_home, Dimens.TabHomeIcon), TabStyles.Home),
                TabBarItem(stringResource(R.string.tab_collection), TabIcon.Drawable(R.drawable.ic_tab_collection, Dimens.TabIcon), TabStyles.Collection),
                TabBarItem(stringResource(pet.defaultName), TabIcon.Pet(pet), TabStyles.Pet),
                TabBarItem(
                    stringResource(R.string.tab_settings),
                    TabIcon.Drawable(R.drawable.ic_tab_settings, Dimens.TabIcon, selected = R.drawable.ic_tab_settings_selected),
                    TabStyles.Settings,
                ),
            )
            items.indices.forEach { selected ->
                WigglebunTabBar(items = items, selectedIndex = selected, onSelect = {})
            }
        }
    }
}
