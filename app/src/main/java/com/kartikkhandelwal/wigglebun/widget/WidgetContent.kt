package com.kartikkhandelwal.wigglebun.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.color.ColorProvider as DayNightColor
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.kartikkhandelwal.wigglebun.MainActivity
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted

/**
 * Widget UI (CLAUDE.md §8). Stateless, so previews and screenshot tests can render it.
 * Small (2×2): pet + minutes, tap opens the app. Medium (4×2): adds a "Take a break" button.
 */
@Composable
fun WidgetContent(state: WidgetUiState, medium: Boolean) {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            // A shape drawable, not cornerRadius(): that one is ignored below Android 12.
            .background(ImageProvider(R.drawable.widget_background))
            .padding(Dimens.Widget.Padding)
            .clickable(openApp(context)),
        contentAlignment = Alignment.Center,
    ) {
        if (medium) MediumContent(state) else SmallContent(state)
    }
}

@Composable
private fun SmallContent(state: WidgetUiState) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PetImage(state, GlanceModifier.fillMaxWidth().defaultWeight())
        Text(
            text = timeText(LocalContext.current, state),
            style = labelStyle(Ink, FontWeight.Bold, TextAlign.Center),
            maxLines = 1,
        )
    }
}

@Composable
private fun MediumContent(state: WidgetUiState) {
    val context = LocalContext.current
    Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        PetImage(state, GlanceModifier.width(Dimens.Widget.MediumPetWidth).fillMaxHeight())
        Spacer(GlanceModifier.width(Dimens.Widget.Gap))
        Column(modifier = GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
            if (state.isSleeping) {
                Text(
                    text = context.getString(R.string.widget_asleep, state.petName),
                    style = titleStyle(),
                    maxLines = 2,
                )
                Text(text = context.getString(R.string.widget_asleep_detail), style = labelStyle(InkMuted), maxLines = 2)
            } else {
                Text(text = timeText(context, state), style = titleStyle(), maxLines = 1)
                Text(text = context.getString(R.string.widget_since_break), style = labelStyle(InkMuted), maxLines = 1)
                Spacer(GlanceModifier.height(Dimens.Widget.Gap))
                BreakButton(state.suggestedBreak)
            }
        }
    }
}

@Composable
private fun PetImage(state: WidgetUiState, modifier: GlanceModifier) {
    val context = LocalContext.current
    Image(
        provider = ImageProvider(state.petImage),
        contentDescription = context.getString(R.string.widget_cd_pet, state.petName, context.getString(moodLabel(state.mood))),
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

@Composable
private fun BreakButton(type: BreakType) {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .height(Dimens.Widget.ButtonHeight)
            .background(ImageProvider(R.drawable.widget_button_background))
            .padding(horizontal = Dimens.Widget.ButtonHorizontalPadding)
            .clickable(startBreak(context, type)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = context.getString(R.string.widget_take_break),
            style = TextStyle(color = fixed(Ink), fontSize = Dimens.Widget.ButtonTextSize, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
    }
}

private fun timeText(context: Context, state: WidgetUiState): String {
    val minutes = state.minutes ?: return context.getString(R.string.widget_sleeping_short)
    return context.resources.getQuantityString(R.plurals.home_minutes, minutes, minutes)
}

private fun moodLabel(mood: Mood): Int = when (mood) {
    Mood.HAPPY, Mood.CELEBRATE, Mood.STRETCH -> R.string.widget_mood_happy
    Mood.TIRED -> R.string.widget_mood_tired
    Mood.STIFF -> R.string.widget_mood_stiff
    Mood.SLEEPING -> R.string.widget_mood_sleeping
}

private fun titleStyle() = TextStyle(color = fixed(Ink), fontSize = Dimens.Widget.TitleSize, fontWeight = FontWeight.Bold)

private fun labelStyle(color: Color, weight: FontWeight = FontWeight.Medium, align: TextAlign = TextAlign.Start) =
    TextStyle(color = fixed(color), fontSize = Dimens.Widget.LabelSize, fontWeight = weight, textAlign = align)

/** The app is light-only, so the widget uses the same colour in dark mode. */
private fun fixed(color: Color): ColorProvider = DayNightColor(day = color, night = color)

private fun openApp(context: Context): Action = actionStartActivity(Intent(context, MainActivity::class.java))

private fun startBreak(context: Context, type: BreakType): Action =
    actionStartActivity(MainActivity.startBreakIntent(context, type))

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 110, heightDp = 110)
@Composable
internal fun WidgetSmallPreview() {
    WidgetContent(WidgetPreviewStates.Tired, medium = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 110)
@Composable
internal fun WidgetMediumPreview() {
    WidgetContent(WidgetPreviewStates.Tired, medium = true)
}

internal object WidgetPreviewStates {
    val Happy = WidgetUiState(Mood.HAPPY, minutes = 8, petName = "Bun", suggestedBreak = BreakType.NECK)
    val Tired = WidgetUiState(Mood.TIRED, minutes = 32, petName = "Bun", suggestedBreak = BreakType.NECK)
    val Stiff = WidgetUiState(Mood.STIFF, minutes = 52, petName = "Bun", suggestedBreak = BreakType.STAND_UP)
    val Sleeping = WidgetUiState(Mood.SLEEPING, minutes = null, petName = "Bun", suggestedBreak = BreakType.STAND_UP)
}
