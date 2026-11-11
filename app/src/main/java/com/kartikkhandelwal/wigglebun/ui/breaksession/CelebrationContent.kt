package com.kartikkhandelwal.wigglebun.ui.breaksession

import com.kartikkhandelwal.wigglebun.feedback.FeelSound
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.data.repo.BreakDoneResult
import com.kartikkhandelwal.wigglebun.domain.model.AccessoryId
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.rewards.Streak
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import com.kartikkhandelwal.wigglebun.ui.components.ChunkySurface
import com.kartikkhandelwal.wigglebun.ui.components.SparkIcon
import com.kartikkhandelwal.wigglebun.ui.pet.PetNameSentence
import com.kartikkhandelwal.wigglebun.ui.pet.PetSpec
import com.kartikkhandelwal.wigglebun.ui.pet.PetSpecs
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.CelebrateHalo
import com.kartikkhandelwal.wigglebun.ui.theme.CelebrateRays
import com.kartikkhandelwal.wigglebun.ui.theme.CelebrateSparkTile
import com.kartikkhandelwal.wigglebun.ui.theme.CelebrateText
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.DoneGreen
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.Rug
import com.kartikkhandelwal.wigglebun.ui.theme.RugEdge
import com.kartikkhandelwal.wigglebun.ui.theme.StreakBarIdle
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTextStyles
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

private val S = Dimens.BreakSession

// Celebrate.dc.html, in the artboard's 390 × 400 units: the halo and rays centre, the rug.
private const val CENTRE_X = 195f
private const val CENTRE_Y = 250f
private const val HALO_RADIUS = 118f
private const val RUG_Y = 365f
private const val RUG_RADIUS_X = 118f
private const val RUG_RADIUS_Y = 21f
private const val PET_FEET_Y = 352f
/** Where the feet are in the pet's 210-unit-tall box. */
private const val PET_FEET_IN_BOX = 192f
private const val PET_BOX_HEIGHT_FACTOR = 210f / 200f
private const val RAY_COUNT = 17
private const val RAY_WIDTH_DEGREES = 10.5f

/**
 * After Done (design/ui_v2/Celebrate.dc.html): the pet jumps on its rug under a ray burst, "You did
 * it!", the sparks this break paid (only if it paid), any streak bonus, and today's streak
 * progress (work days only). Continue goes back to Home.
 *
 * The scene is drawn for a phone as tall as the mockup. On a shorter phone, or with a bigger
 * font, it shrinks (never below [Dimens.BreakSession.SceneMinScale]) so the rewards stay on screen.
 * Above [Dimens.BreakSession.LargeFontScale] Continue scrolls with the content.
 */
@Composable
internal fun CelebrationContent(
    result: BreakDoneResult,
    pet: PetSpec,
    petName: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    nextPet: NextPetHint? = null,
    accessories: List<AccessoryId> = emptyList(),
) {
    val fontScale = LocalDensity.current.fontScale
    val buttonInContent = fontScale > S.CelebrateLargeFontScale
    val button: @Composable () -> Unit = {
        ChunkyButton(
            onClick = onContinue,
            sound = FeelSound.TapSoft,
            shape = WigglebunShapes.SessionButton,
            modifier = Modifier.fillMaxWidth().heightIn(min = S.ButtonHeight + Dimens.ButtonEdge),
        ) {
            Text(stringResource(R.string.celebrate_continue))
        }
    }
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    BoxWithConstraints(modifier.fillMaxSize()) {
        // What the pinned Continue takes; the scene gets what is left once the text and cards have theirs.
        val pinned = if (buttonInContent) 0.dp else S.ButtonHeight + Dimens.ButtonEdge + S.CelebrateGap + S.CelebrateButtonBottom + bottomInset
        val rewardCards = (if (result.sparksEarned > 0) 1 else 0) + result.bonuses.size
        val lower = S.LowerBase + S.RewardCardSlot * rewardCards + (if (result.streak.todayIsWorkDay) S.StreakCardSlot else 0.dp)
        val scale = ((maxHeight - pinned - topInset - lower * fontScale) / S.SceneHeight).coerceIn(S.SceneMinScale, 1f)
        // The rays fill the whole screen, behind everything, from the halo's centre.
        Canvas(Modifier.fillMaxSize()) { drawRays(scale, topInset.toPx()) }
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                CelebrationScene(scale, pet, petName, accessories)
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = S.CelebrateHorizontal),
                    verticalArrangement = Arrangement.spacedBy(S.CelebrateGap),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        // Announced once when the celebration appears.
                        modifier = Modifier.padding(horizontal = S.TextHorizontal - S.CelebrateHorizontal).semantics(mergeDescendants = true) {
                            liveRegion = LiveRegionMode.Polite
                        },
                        verticalArrangement = Arrangement.spacedBy(S.TitleGap),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            stringResource(R.string.celebrate_title),
                            style = WigglebunTextStyles.CelebrateTitle,
                            color = Ink,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.semantics { heading() },
                        )
                        PetNameSentence(
                            R.string.celebrate_subtitle,
                            petName,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold),
                            color = CelebrateText,
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (result.sparksEarned > 0) {
                        RewardCard(
                            title = pluralStringResource(R.plurals.celebrate_sparks, result.sparksEarned, result.sparksEarned),
                            sub = nextPet?.let { saveUpLine(it) },
                        )
                    }
                    result.bonuses.forEach { milestone ->
                        RewardCard(
                            title = pluralStringResource(R.plurals.celebrate_sparks, milestone.bonus, milestone.bonus),
                            sub = pluralStringResource(R.plurals.celebrate_bonus_reason, milestone.days, milestone.days),
                        )
                    }
                    if (result.streak.todayIsWorkDay) StreakCard(result.streak)
                    if (buttonInContent) Box(Modifier.padding(top = S.CelebrateGap, bottom = S.CelebrateButtonBottom).navigationBarsPadding()) { button() }
                }
                }
            }
            if (!buttonInContent) {
                Box(
                    Modifier
                        .navigationBarsPadding()
                        .padding(start = S.TextHorizontal, end = S.TextHorizontal, top = S.CelebrateGap, bottom = S.CelebrateButtonBottom),
                ) { button() }
            }
        }
    }
}

/** "Save up to adopt Pip at 75", or "You can adopt Pip!" once the sparks cover it. */
@Composable
private fun saveUpLine(hint: NextPetHint): String {
    val id = hint.id ?: return stringResource(R.string.celebrate_save_up_accessories)
    val name = stringResource(PetSpecs.of(id).defaultName)
    return if (hint.affordable) {
        stringResource(R.string.celebrate_can_adopt, name)
    } else {
        stringResource(R.string.celebrate_save_up, name, hint.price)
    }
}

/**
 * The rays, the halo, the rug and the jumping pet. Drawn in the mockup's units times [scale]; the
 * rays reach the edges of the screen whatever its width. The pet is a real [PetView], which brings
 * its own jump and confetti.
 */
@Composable
private fun CelebrationScene(scale: Float, pet: PetSpec, petName: String, accessories: List<AccessoryId>) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val sceneHeight = S.SceneHeight * scale
    Box(Modifier.fillMaxWidth().height(sceneHeight + topInset)) {
        Canvas(Modifier.fillMaxSize()) { drawScene(scale, topInset.toPx()) }
        PetView(
            energy = 1f,
            mood = Mood.CELEBRATE,
            spec = pet,
            name = petName,
            accessories = accessories,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topInset + ((PET_FEET_Y - PET_FEET_IN_BOX) * scale).dp)
                .size(S.PetSize * scale, S.PetSize * PET_BOX_HEIGHT_FACTOR * scale),
        )
    }
}

/** The ray burst from the halo's centre, long enough to leave the screen at any width or height. */
private fun DrawScope.drawRays(scale: Float, topInset: Float) {
    val unit = scale * density
    val centre = Offset(size.width / 2f, topInset + CENTRE_Y * unit)
    val reach = hypot(size.width, size.height) * 1.5f
    val step = 360f / RAY_COUNT
    val rays = Path()
    repeat(RAY_COUNT) { index ->
        val middle = -100f + index * step
        val from = (middle - RAY_WIDTH_DEGREES / 2f).toRadians()
        val to = (middle + RAY_WIDTH_DEGREES / 2f).toRadians()
        rays.moveTo(centre.x, centre.y)
        rays.lineTo(centre.x + reach * cos(from), centre.y + reach * sin(from))
        rays.lineTo(centre.x + reach * cos(to), centre.y + reach * sin(to))
        rays.close()
    }
    drawPath(rays, CelebrateRays)
}

private fun DrawScope.drawScene(scale: Float, topInset: Float) {
    val unit = scale * density
    val centre = Offset(size.width / 2f, topInset + CENTRE_Y * unit)
    drawCircle(CelebrateHalo, radius = HALO_RADIUS * unit, center = centre)
    sparkle(Offset(centre.x + (90f - CENTRE_X) * unit, centre.y + (160f - CENTRE_Y) * unit), 10f * unit)
    sparkle(Offset(centre.x + (300f - CENTRE_X) * unit, centre.y + (150f - CENTRE_Y) * unit), 8.6f * unit)

    // The rug: a shadow, the top, and a dotted stitch line.
    val rugCentre = Offset(centre.x, topInset + RUG_Y * unit)
    val rx = RUG_RADIUS_X * unit
    val ry = RUG_RADIUS_Y * unit
    drawOval(RugEdge, Offset(rugCentre.x - rx, rugCentre.y + 3f * unit - ry - unit), Size(rx * 2, (ry + unit) * 2))
    drawOval(Rug, Offset(rugCentre.x - rx, rugCentre.y - ry), Size(rx * 2, ry * 2))
    val stitchX = 96f * unit
    val stitchY = 14f * unit
    drawOval(
        Color.White,
        Offset(rugCentre.x - stitchX, rugCentre.y - stitchY),
        Size(stitchX * 2, stitchY * 2),
        style = Stroke(2.5f * unit, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, 8f * unit))),
    )
}

private fun Float.toRadians(): Float = (this * PI / 180.0).toFloat()

private fun DrawScope.sparkle(centre: Offset, arm: Float) {
    val waist = arm * 0.3f
    val path = Path().apply {
        moveTo(centre.x, centre.y - arm)
        lineTo(centre.x + waist, centre.y - waist)
        lineTo(centre.x + arm, centre.y)
        lineTo(centre.x + waist, centre.y + waist)
        lineTo(centre.x, centre.y + arm)
        lineTo(centre.x - waist, centre.y + waist)
        lineTo(centre.x - arm, centre.y)
        lineTo(centre.x - waist, centre.y - waist)
        close()
    }
    drawPath(path, Card)
}

/** A white card on the green wall: a Sun tile with the spark, a Fredoka title and an optional line under it. */
@Composable
private fun RewardCard(title: String, sub: String?, modifier: Modifier = Modifier) {
    ChunkySurface(
        shape = WigglebunShapes.RewardCard,
        faceColor = Card,
        edgeHeight = S.RewardCardEdge,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = S.RewardCardPaddingHorizontal, vertical = S.RewardCardPaddingVertical).semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(S.RewardGap),
        ) {
            Box(
                Modifier.size(S.RewardTile).clip(WigglebunShapes.IconTile).background(CelebrateSparkTile),
                contentAlignment = Alignment.Center,
            ) { SparkIcon(S.RewardTileIcon) }
            Column(Modifier.weight(1f)) {
                Text(title, style = WigglebunTextStyles.RewardTitle, color = Ink)
                if (sub != null) Text(sub, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = InkMuted)
            }
        }
    }
}

/** "2 of 3 breaks for your streak" + "today", with one bar per break needed; or "Today counts! 5-day streak". */
@Composable
private fun StreakCard(streak: Streak, modifier: Modifier = Modifier) {
    val text = if (streak.todayCounts) {
        pluralStringResource(R.plurals.streak_today_counts, streak.days, streak.days)
    } else {
        pluralStringResource(R.plurals.streak_progress, streak.goal, streak.todayDone, streak.goal)
    }
    ChunkySurface(
        shape = WigglebunShapes.RewardCard,
        faceColor = Card,
        edgeHeight = S.RewardCardEdge,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(horizontal = S.RewardCardPaddingHorizontal, vertical = S.RewardCardPaddingVertical),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(S.RewardGap), verticalAlignment = Alignment.Top) {
                Text(text, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp), color = Ink, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.celebrate_today), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = InkMuted)
            }
            val filled = min(streak.todayDone, streak.goal)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(S.StreakBarGap)) {
                repeat(streak.goal) { index ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(S.StreakBarHeight)
                            .clip(WigglebunShapes.Pill)
                            .background(if (index < filled) DoneGreen else StreakBarIdle),
                    )
                }
            }
        }
    }
}
