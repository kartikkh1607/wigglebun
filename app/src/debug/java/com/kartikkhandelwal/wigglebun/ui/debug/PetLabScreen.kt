package com.kartikkhandelwal.wigglebun.ui.debug

import android.content.Intent
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingEngine
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButtonStyle
import com.kartikkhandelwal.wigglebun.ui.components.PillCard
import com.kartikkhandelwal.wigglebun.ui.pet.PetSpecs
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.Coral
import com.kartikkhandelwal.wigglebun.ui.theme.Cream
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.DoneGreen
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.PetYellowEdge
import com.kartikkhandelwal.wigglebun.ui.theme.Track
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE HH:mm:ss")

/** Everything the lab can do, as plain callbacks so the content is previewable. */
class PetLabActions(
    val onEnergy: (Float) -> Unit = {},
    val onMood: (Mood) -> Unit = {},
    val onMoodFollowsEnergy: (Boolean) -> Unit = {},
    val onAnimated: (Boolean) -> Unit = {},
    val onCelebrate: () -> Unit = {},
    val onFollowEngine: (Boolean) -> Unit = {},
    val onFastClock: (Boolean) -> Unit = {},
    val onAlwaysWorkHours: (Boolean) -> Unit = {},
    val onInterval: (steps: Int) -> Unit = {},
    val onDone: () -> Unit = {},
    val onSnooze: () -> Unit = {},
    val onSkip: () -> Unit = {},
    val onReset: () -> Unit = {},
)

/** Debug-only playground for PetView and the sitting engine. */
@Composable
fun PetLabScreen(viewModel: PetLabViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PetLabContent(
        state = state,
        actions = PetLabActions(
            onEnergy = viewModel::setEnergy,
            onMood = viewModel::setMood,
            onMoodFollowsEnergy = viewModel::setMoodFollowsEnergy,
            onAnimated = viewModel::setAnimated,
            onCelebrate = viewModel::celebrate,
            onFollowEngine = viewModel::setFollowEngine,
            onFastClock = { viewModel.setFastClock(it) },
            onAlwaysWorkHours = { viewModel.setAlwaysWorkHours(it) },
            onInterval = { viewModel.changeInterval(it) },
            onDone = { viewModel.done() },
            onSnooze = { viewModel.snooze() },
            onSkip = { viewModel.skip() },
            onReset = { viewModel.resetTimer() },
        ),
    )
}

@Composable
private fun PetLabContent(state: PetLabState, actions: PetLabActions) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.ListGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.pet_lab_title), style = MaterialTheme.typography.titleLarge)

        PetView(
            energy = state.petEnergy,
            mood = state.petMood,
            animated = state.animated,
            modifier = Modifier.size(220.dp, 231.dp),
        )

        state.engine?.let { EngineCard(it, state.followEngine, actions) }
        ManualCard(state, actions)
    }
}

@Composable
private fun EngineCard(engine: EngineInfo, followEngine: Boolean, actions: PetLabActions) {
    PillCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.pet_lab_engine), style = MaterialTheme.typography.titleMedium)
        InfoLine(stringResource(R.string.pet_lab_engine_now, engine.now.format(engine.zone)))
        InfoLine(
            stringResource(if (engine.isWorkTime) R.string.pet_lab_engine_working else R.string.pet_lab_engine_off),
        )
        InfoLine(
            stringResource(
                R.string.pet_lab_engine_sit,
                engine.sitSeconds / 60,
                engine.sitSeconds % 60,
                engine.intervalMinutes,
            ),
        )
        InfoLine(
            engine.nextReminder?.let { stringResource(R.string.pet_lab_engine_next, it.format(engine.zone)) }
                ?: stringResource(R.string.pet_lab_engine_next_none),
        )
        InfoLine(stringResource(R.string.pet_lab_engine_snoozes, engine.snoozeCount, SittingEngine.MAX_SNOOZES))
        InfoLine(stringResource(R.string.pet_lab_engine_mood, engine.mood.name, engine.energy))
        Text(
            text = stringResource(
                if (engine.notificationsAllowed) R.string.pet_lab_notifications_allowed else R.string.pet_lab_notifications_blocked,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = if (engine.notificationsAllowed) DoneGreen else Coral,
        )

        LabSwitch(R.string.pet_lab_follow_engine, followEngine, actions.onFollowEngine)
        LabSwitch(R.string.pet_lab_fast_clock, engine.fastClock, actions.onFastClock)
        LabSwitch(R.string.pet_lab_always_work, engine.alwaysWorkHours, actions.onAlwaysWorkHours)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap),
        ) {
            Text(
                text = stringResource(R.string.pet_lab_interval, engine.intervalMinutes),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            ChunkyButton(text = stringResource(R.string.pet_lab_interval_down), onClick = { actions.onInterval(-1) }, style = ChunkyButtonStyle.Secondary)
            ChunkyButton(text = stringResource(R.string.pet_lab_interval_up), onClick = { actions.onInterval(1) }, style = ChunkyButtonStyle.Secondary)
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap),
            verticalArrangement = Arrangement.spacedBy(Dimens.ListGap),
        ) {
            ChunkyButton(text = stringResource(R.string.pet_lab_done), onClick = actions.onDone, style = ChunkyButtonStyle.Done)
            ChunkyButton(text = stringResource(R.string.pet_lab_snooze), onClick = actions.onSnooze, style = ChunkyButtonStyle.Secondary)
            ChunkyButton(text = stringResource(R.string.pet_lab_skip), onClick = actions.onSkip, style = ChunkyButtonStyle.Secondary)
            ChunkyButton(text = stringResource(R.string.pet_lab_reset), onClick = actions.onReset, style = ChunkyButtonStyle.Secondary)
        }

        val context = LocalContext.current
        ChunkyButton(
            text = stringResource(R.string.pet_lab_open_notification_settings),
            onClick = {
                // Opens the system settings page; this is not a runtime permission request (that's M9).
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
            style = ChunkyButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ManualCard(state: PetLabState, actions: PetLabActions) {
    PillCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.pet_lab_manual), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(
                R.string.pet_lab_energy,
                state.manual.energy,
                PetSpecs.Bun.fillTop(state.manual.energy),
            ),
            style = MaterialTheme.typography.labelMedium,
            color = InkMuted,
        )
        Slider(
            value = state.manual.energy,
            onValueChange = {
                actions.onFollowEngine(false)
                actions.onEnergy(it)
            },
            colors = SliderDefaults.colors(
                thumbColor = Ink,
                activeTrackColor = PetYellowEdge,
                inactiveTrackColor = Track,
            ),
        )
        LabSwitch(R.string.pet_lab_mood_follows_energy, state.manual.moodFollowsEnergy, actions.onMoodFollowsEnergy)
        LabSwitch(R.string.pet_lab_animate, state.animated, actions.onAnimated)

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(Dimens.ListGap),
        ) {
            Mood.entries.forEach { mood ->
                val selected = !state.followEngine && mood == state.manual.mood
                ChunkyButton(
                    text = stringResource(mood.labelRes()),
                    onClick = { actions.onMood(mood) },
                    style = if (selected) ChunkyButtonStyle.Primary else ChunkyButtonStyle.Secondary,
                )
            }
        }

        ChunkyButton(
            text = stringResource(R.string.pet_lab_celebrate),
            onClick = actions.onCelebrate,
            style = ChunkyButtonStyle.Highlight,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun InfoLine(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
}

@Composable
private fun LabSwitch(@StringRes label: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Card,
                checkedTrackColor = DoneGreen,
                uncheckedThumbColor = InkMuted,
                uncheckedTrackColor = Track,
            ),
        )
    }
}

private fun Instant.format(zone: ZoneId): String = TimeFormat.format(atZone(zone))

@StringRes
private fun Mood.labelRes(): Int = when (this) {
    Mood.HAPPY -> R.string.pet_lab_mood_happy
    Mood.TIRED -> R.string.pet_lab_mood_tired
    Mood.STIFF -> R.string.pet_lab_mood_stiff
    Mood.STRETCH -> R.string.pet_lab_mood_stretch
    Mood.CELEBRATE -> R.string.pet_lab_mood_celebrate
    Mood.SLEEPING -> R.string.pet_lab_mood_sleeping
}

@Preview(widthDp = 390, heightDp = 1400)
@Composable
internal fun PetLabPreview() {
    val zone = ZoneId.of("Asia/Kolkata")
    val now = Instant.parse("2026-10-26T05:02:00Z")
    WigglebunTheme {
        PetLabContent(
            state = PetLabState(
                animated = false,
                engine = EngineInfo(
                    now = now,
                    zone = zone,
                    isWorkTime = true,
                    sitSeconds = 32 * 60 + 5,
                    intervalMinutes = 45,
                    nextReminder = now.plusSeconds(13 * 60),
                    snoozeCount = 1,
                    mood = Mood.TIRED,
                    energy = 0.41f,
                    notificationsAllowed = false,
                    fastClock = true,
                    alwaysWorkHours = true,
                ),
            ),
            actions = PetLabActions(),
        )
    }
}
