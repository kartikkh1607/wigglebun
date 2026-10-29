package com.kartikkhandelwal.wigglebun.ui.pet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kartikkhandelwal.wigglebun.domain.model.AccessoryId
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.ui.theme.BreakSky
import com.kartikkhandelwal.wigglebun.ui.theme.Cream
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme

private val AllPets = listOf(PetSpecs.Bun, PetSpecs.Pip, PetSpecs.Toast, PetSpecs.Nimbus)

/** One head, face and neck item worn together. */
private val Outfit = listOf(AccessoryId.BEANIE, AccessoryId.ROUND_GLASSES, AccessoryId.KNIT_SCARF)

/**
 * Art review grid: a row per pet (Bun, Pip, Toast, Nimbus); columns: nothing, each accessory
 * in AccessoryId order (flower clip, round glasses, knit scarf, beanie), then [Outfit].
 */
@Preview(name = "Accessories × pets", widthDp = 760)
@Composable
internal fun AccessoryGridPreview() {
    val columns = listOf(emptyList<AccessoryId>()) + AccessoryId.entries.map { listOf(it) } + listOf(Outfit)
    WigglebunTheme {
        Column(
            modifier = Modifier.background(Cream).padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ListGap),
        ) {
            AllPets.forEach { spec ->
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap)) {
                    columns.forEach { worn ->
                        PetView(energy = 1f, mood = Mood.HAPPY, spec = spec, accessories = worn, modifier = Modifier.size(100.dp, 105.dp))
                    }
                }
            }
        }
    }
}

/** The outfit through every mood, per pet: leaning, slumping, sleeping and jumping with the body. */
@Preview(name = "Accessories × moods", widthDp = 760)
@Composable
internal fun AccessoryMoodsPreview() {
    WigglebunTheme {
        Column(
            modifier = Modifier.background(Cream).padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ListGap),
        ) {
            AllPets.forEach { spec ->
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap)) {
                    Mood.entries.forEach { mood ->
                        PetView(
                            energy = mood.previewEnergy(),
                            mood = mood,
                            spec = spec,
                            accessories = Outfit,
                            modifier = Modifier.size(100.dp, 105.dp),
                        )
                    }
                }
            }
        }
    }
}

/** The outfit in break-session poses (the session screen shows accessories too). */
@Preview(name = "Accessories × poses", widthDp = 420)
@Composable
internal fun AccessoryPosesPreview() {
    val poses = listOf(Poses.NeckTiltRight, Poses.Sit, Poses.ReachUp, Poses.ShouldersBack, Poses.WristLeft, Poses.MarchB)
    WigglebunTheme {
        Column(
            modifier = Modifier.background(BreakSky).padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ListGap),
        ) {
            AllPets.forEach { spec ->
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap)) {
                    poses.forEach { pose ->
                        PetView(energy = 1f, mood = Mood.STRETCH, spec = spec, pose = pose, accessories = Outfit, modifier = Modifier.size(56.dp, 59.dp))
                    }
                }
            }
        }
    }
}
