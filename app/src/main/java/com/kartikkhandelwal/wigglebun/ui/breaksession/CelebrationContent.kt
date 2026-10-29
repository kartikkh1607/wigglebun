package com.kartikkhandelwal.wigglebun.ui.breaksession

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.data.repo.BreakDoneResult
import com.kartikkhandelwal.wigglebun.domain.model.AccessoryId
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.rewards.Milestone
import com.kartikkhandelwal.wigglebun.domain.rewards.Streak
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import com.kartikkhandelwal.wigglebun.ui.components.ChunkySurface
import com.kartikkhandelwal.wigglebun.ui.components.SparkIcon
import com.kartikkhandelwal.wigglebun.ui.pet.PetNameSentence
import com.kartikkhandelwal.wigglebun.ui.pet.PetSpec
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.DoneGreen
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.SessionAccent
import com.kartikkhandelwal.wigglebun.ui.theme.SessionEdge
import com.kartikkhandelwal.wigglebun.ui.theme.SessionMuted
import com.kartikkhandelwal.wigglebun.ui.theme.Sun
import com.kartikkhandelwal.wigglebun.ui.theme.SunEdge
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTextStyles

private val S = Dimens.BreakSession

/**
 * After Done: the pet jumps with confetti, "+N sparks" (only if this break paid), any streak
 * bonus, and today's streak progress (work days only). Continue goes back to Home.
 */
@Composable
internal fun CelebrationContent(
    result: BreakDoneResult,
    pet: PetSpec,
    petName: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    accessories: List<AccessoryId> = emptyList(),
) {
    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = S.CardHorizontal),
            verticalArrangement = Arrangement.spacedBy(S.CelebrateGap, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The pet gives way first on short screens, so the rewards always stay visible.
            BoxWithConstraints(Modifier.weight(1f, fill = false), contentAlignment = Alignment.Center) {
                val petSize = minOf(S.CelebratePetSize, maxHeight)
                PetView(
                    energy = 1f,
                    mood = Mood.CELEBRATE,
                    spec = pet,
                    name = petName,
                    accessories = accessories,
                    modifier = Modifier.size(petSize),
                )
            }
            Column(
                // Announced once when the celebration appears.
                modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.celebrate_title), style = MaterialTheme.typography.displayMedium, color = Ink, textAlign = TextAlign.Center)
                PetNameSentence(
                    R.string.celebrate_subtitle,
                    petName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = SessionMuted,
                    textAlign = TextAlign.Center,
                )
                if (result.sparksEarned > 0) SparksBadge(result.sparksEarned, Modifier.padding(top = S.CelebrateGap))
            }
            result.bonuses.forEach { BonusBadge(it) }
            if (result.streak.todayIsWorkDay) StreakCard(result.streak, Modifier.padding(top = S.StreakCardTop))
        }
        ChunkyButton(
            text = stringResource(R.string.celebrate_continue),
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = S.ButtonsHorizontal, end = S.ButtonsHorizontal, top = S.ButtonsTop, bottom = S.ButtonsBottom)
                .heightIn(min = S.ButtonHeight + Dimens.ButtonEdge),
        )
    }
}

/** "+10 sparks" on a white chunky pill, like the sparks counter on Home. */
@Composable
private fun SparksBadge(sparks: Int, modifier: Modifier = Modifier) {
    ChunkySurface(shape = WigglebunShapes.Pill, faceColor = Card, edgeColor = SessionEdge, edgeHeight = S.BadgeEdge, modifier = modifier) {
        Row(
            modifier = Modifier.padding(start = S.BadgePaddingStart, end = S.BadgePaddingEnd, top = S.BadgePaddingVertical, bottom = S.BadgePaddingVertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(S.BadgeGap),
        ) {
            SparkIcon(S.BadgeIcon)
            Text(pluralStringResource(R.plurals.celebrate_sparks, sparks, sparks), style = MaterialTheme.typography.labelLarge, color = Ink)
        }
    }
}

/** "3-day streak bonus +20" on a Sun pill. */
@Composable
private fun BonusBadge(milestone: Milestone) {
    ChunkySurface(shape = WigglebunShapes.Pill, faceColor = Sun, edgeColor = SunEdge, edgeHeight = S.BadgeEdge) {
        Row(
            modifier = Modifier.padding(start = S.BadgePaddingStart, end = S.BadgePaddingEnd, top = S.BadgePaddingVertical, bottom = S.BadgePaddingVertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(S.BadgeGap),
        ) {
            SparkIcon(S.BadgeIcon)
            Text(
                pluralStringResource(R.plurals.celebrate_streak_bonus, milestone.days, milestone.days, milestone.bonus),
                style = WigglebunTextStyles.Sparks,
                color = Ink,
            )
        }
    }
}

/** "3 of 4 breaks for your streak" with one bar per break needed, or "Today counts! 5-day streak". */
@Composable
private fun StreakCard(streak: Streak, modifier: Modifier = Modifier) {
    val text = if (streak.todayCounts) {
        pluralStringResource(R.plurals.streak_today_counts, streak.days, streak.days)
    } else {
        pluralStringResource(R.plurals.streak_progress, streak.goal, streak.todayDistinct, streak.goal)
    }
    ChunkySurface(
        shape = WigglebunShapes.Card,
        faceColor = Card,
        edgeColor = SessionEdge,
        edgeHeight = S.CardEdge,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(S.StreakCardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(S.CelebrateGap),
        ) {
            Text(text, style = MaterialTheme.typography.titleMedium, color = Ink, textAlign = TextAlign.Center)
            Segments(
                count = streak.goal,
                filled = minOf(streak.todayDistinct, streak.goal),
                filledColor = if (streak.todayCounts) DoneGreen else SessionAccent,
            )
        }
    }
}
