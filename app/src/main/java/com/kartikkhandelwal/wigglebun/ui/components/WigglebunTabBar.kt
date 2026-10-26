package com.kartikkhandelwal.wigglebun.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.Coral
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.TabActive
import com.kartikkhandelwal.wigglebun.ui.theme.TabBarBorder
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme

@Immutable
data class TabBarItem(
    val label: String,
    @DrawableRes val icon: Int,
    /** Single-colour icons are tinted Coral/InkMuted; the pet icon keeps its own colours. */
    val tintIcon: Boolean = true,
)

/** Bottom tab bar, matching the `<nav>` in design/mockups_source/Main.dc.html. */
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
                .background(TabBarBorder),
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
    val indicator by animateColorAsState(
        targetValue = if (selected) TabActive else Color.Transparent,
        label = "tabIndicator",
    )
    Column(
        modifier = modifier
            .fillMaxHeight()
            .selectable(
                selected = selected,
                onClick = onClick,
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
                .background(indicator, WigglebunShapes.Pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(item.icon),
                contentDescription = null, // the label below describes the tab
                tint = when {
                    !item.tintIcon -> Color.Unspecified
                    selected -> Coral
                    else -> InkMuted
                },
                modifier = Modifier.size(if (item.tintIcon) Dimens.TabIcon else Dimens.TabPetIcon),
            )
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

@Preview(showBackground = true, widthDp = 390)
@Composable
internal fun WigglebunTabBarPreview() {
    WigglebunTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.ListGap)) {
            val items = listOf(
                TabBarItem(stringResource(R.string.tab_home), R.drawable.ic_tab_home),
                TabBarItem(stringResource(R.string.tab_collection), R.drawable.ic_tab_collection),
                TabBarItem(stringResource(R.string.pet_default_name), R.drawable.ic_tab_pet, tintIcon = false),
                TabBarItem(stringResource(R.string.tab_settings), R.drawable.ic_tab_settings),
            )
            items.indices.forEach { selected ->
                WigglebunTabBar(items = items, selectedIndex = selected, onSelect = {})
            }
        }
    }
}
