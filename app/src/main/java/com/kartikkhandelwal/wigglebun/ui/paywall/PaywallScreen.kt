package com.kartikkhandelwal.wigglebun.ui.paywall

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.billing.FakePremiumRepository
import com.kartikkhandelwal.wigglebun.billing.PaywallOptions
import com.kartikkhandelwal.wigglebun.billing.PlanKind
import com.kartikkhandelwal.wigglebun.billing.PlanOption
import com.kartikkhandelwal.wigglebun.billing.Trial
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.feedback.ConsumeFeel
import com.kartikkhandelwal.wigglebun.feedback.FeelSound
import com.kartikkhandelwal.wigglebun.feedback.tickClickable
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButtonStyle
import com.kartikkhandelwal.wigglebun.ui.components.ChunkySurface
import com.kartikkhandelwal.wigglebun.ui.components.LegalLinks
import com.kartikkhandelwal.wigglebun.ui.components.SheetPanel
import com.kartikkhandelwal.wigglebun.ui.components.TopIcon
import com.kartikkhandelwal.wigglebun.ui.components.TopIconButton
import com.kartikkhandelwal.wigglebun.ui.pet.PetNameSentence
import com.kartikkhandelwal.wigglebun.ui.pet.PetSpecs
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.theme.BreakNeckTint
import com.kartikkhandelwal.wigglebun.ui.theme.BreakSky
import com.kartikkhandelwal.wigglebun.ui.theme.BreakWater
import com.kartikkhandelwal.wigglebun.ui.theme.ButtonBorder
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.Coral
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.DoneGreen
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.LocalSurface
import com.kartikkhandelwal.wigglebun.ui.theme.MoodHappyChip
import com.kartikkhandelwal.wigglebun.ui.theme.OnSurface
import com.kartikkhandelwal.wigglebun.ui.theme.OnboardingCard
import com.kartikkhandelwal.wigglebun.ui.theme.PaywallHalo
import com.kartikkhandelwal.wigglebun.ui.theme.PaywallPlanSelected
import com.kartikkhandelwal.wigglebun.ui.theme.StatusOnText
import com.kartikkhandelwal.wigglebun.ui.theme.StepDotIdle
import com.kartikkhandelwal.wigglebun.ui.theme.Sun
import com.kartikkhandelwal.wigglebun.ui.theme.Surfaces
import com.kartikkhandelwal.wigglebun.ui.theme.TabPetTint
import com.kartikkhandelwal.wigglebun.ui.theme.Track
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTextStyles
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import com.kartikkhandelwal.wigglebun.ui.theme.balanced
import kotlinx.coroutines.flow.first

private val D = Dimens.Paywall

/** What the paywall's buttons do. */
class PaywallActions(
    val onClose: () -> Unit = {},
    val onRestore: () -> Unit = {},
    val onPick: (PlanKind) -> Unit = {},
    val onPurchase: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onOpenTerms: () -> Unit = {},
    val onOpenPrivacy: () -> Unit = {},
)

/**
 * The paywall with its ViewModel. [onEntitled] runs once premium is active (bought here, restored,
 * or already owned when the screen opens); [onClose] is the Close button and Back.
 */
@Composable
fun PaywallScreenRoute(
    petName: String,
    onClose: () -> Unit,
    onEntitled: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaywallViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    ConsumeFeel(viewModel.feel)
    val entitled by rememberUpdatedState(onEntitled)
    LaunchedEffect(viewModel) {
        viewModel.entitled.first { it }
        entitled()
    }
    PaywallScreen(
        state = state,
        petName = petName,
        actions = PaywallActions(
            onClose = onClose,
            onRestore = viewModel::onRestore,
            onPick = viewModel::onPick,
            // Play's purchase sheet needs the Activity; there is always one under a screen.
            onPurchase = { context.findActivity()?.let(viewModel::onPurchase) },
            onRetry = viewModel::loadPlans,
            onOpenTerms = { LegalLinks.open(context, LegalLinks.TERMS_URL) },
            onOpenPrivacy = { LegalLinks.open(context, LegalLinks.PRIVACY_URL) },
        ),
        modifier = modifier,
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * The paywall (design/ui_v2/Paywall.dc.html; CLAUDE.md §5.5, §6.8): the pet on a sky, then a
 * white panel with the three benefits, the trial's timeline and the plans; the button, the small
 * print (price after the trial, billing period, how to cancel) and the Terms and Privacy links
 * stay at the bottom. Close is top-left and always works; Restore is top-right.
 *
 * The sky shrinks on a short phone or with a bigger font (never below [Dimens.Paywall.MinScale])
 * so the plans stay on screen; above [Dimens.Paywall.LargeFontScale] the button and small print
 * scroll with the panel.
 */
@Composable
fun PaywallScreen(state: PaywallUiState, petName: String, actions: PaywallActions, modifier: Modifier = Modifier) {
    BackHandler(onBack = actions.onClose)
    val fontScale = LocalDensity.current.fontScale
    val footerInPanel = fontScale > D.LargeFontScale
    OnSurface(Surfaces.Profile) {
        Box(modifier.fillMaxSize().background(Card)) {
            Column(Modifier.fillMaxSize()) {
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val viewport = maxHeight
                    val scale = ((viewport - D.PanelNeed * fontScale) / D.SceneHeight).coerceIn(D.MinScale, 1f)
                    val sceneHeight = D.SceneHeight * scale
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Scene(petName, scale, Modifier.fillMaxWidth().height(sceneHeight))
                        SheetPanel(Modifier.fillMaxWidth().heightIn(min = viewport - sceneHeight)) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                PetNameSentence(
                                    template = R.string.paywall_title,
                                    name = petName,
                                    style = MaterialTheme.typography.displayMedium.copy(fontSize = D.TitleSize).balanced(),
                                    color = Ink,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.semantics { heading() },
                                )
                            }
                            Benefits()
                            PlansSection(state, actions)
                            if (footerInPanel) {
                                Footer(state, actions, Modifier.padding(bottom = D.FooterBottom))
                            } else {
                                Spacer(Modifier.height(D.PanelBottom))
                            }
                        }
                    }
                }
                if (!footerInPanel) {
                    // Part of the panel: white to the bottom of the screen, under the system bar.
                    Footer(
                        state,
                        actions,
                        Modifier
                            .background(Card)
                            .navigationBarsPadding()
                            .padding(start = D.FooterHorizontal, end = D.FooterHorizontal, top = D.FooterTop, bottom = D.FooterBottom),
                    )
                }
            }
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(top = D.TopBarTop, start = D.TopBarStart, end = D.TopBarEnd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TopIconButton(TopIcon.Close, onClick = actions.onClose)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .defaultMinSize(minWidth = D.RestoreMinSize, minHeight = D.RestoreMinSize)
                        .clip(WigglebunShapes.Pill)
                        .tickClickable(onClickLabel = stringResource(R.string.paywall_restore_label), onClick = actions.onRestore)
                        .padding(horizontal = D.RestorePaddingHorizontal),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.paywall_restore),
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = D.RestoreSize),
                        color = Ink,
                    )
                }
            }
        }
    }
}

/** The sky: a soft halo, a few sparkles and the pet, whose feet the panel's rounded top covers. */
@Composable
private fun Scene(petName: String, scale: Float, modifier: Modifier) {
    val sky = LocalSurface.current.background
    Box(modifier.background(sky)) {
        Canvas(Modifier.fillMaxSize().clearAndSetSemantics {}) {
            val unit = 1.dp.toPx() * scale
            // Positions are the mockup's, measured from the artboard's centre line (x = 195).
            fun at(x: Float, y: Float) = Offset(size.width / 2 + (x - MOCKUP_CENTRE_X) * unit, y * unit)
            drawCircle(PaywallHalo, radius = D.HaloOuter.toPx() * scale, center = at(MOCKUP_CENTRE_X, D.HaloY.value))
            drawCircle(BreakSky, radius = D.HaloInner.toPx() * scale, center = at(MOCKUP_CENTRE_X, D.HaloY.value))
            sparkle(at(74f, 106f), 10f * unit, Card)
            sparkle(at(318f, 126.6f), 8.6f * unit, Card)
            sparkle(at(300f, 66f), 8f * unit, Sun)
            sparkle(at(86f, 58.6f), 6.6f * unit, BreakWater)
            drawCircle(Card, radius = 3f * unit, center = at(104f, 160f))
            drawCircle(Card, radius = 3.4f * unit, center = at(292f, 78f))
        }
        PetView(
            energy = 1f,
            mood = Mood.HAPPY,
            spec = PetSpecs.Bun,
            name = petName,
            modifier = Modifier
                .align(Alignment.TopCenter)
                // Offset, not padding: the pet reaches below the sky and the panel covers its feet.
                .offset(y = D.PetTop * scale)
                .size(D.PetWidth * scale, D.PetHeight * scale),
        )
    }
}

private const val MOCKUP_CENTRE_X = 195f

/** A four-point sparkle: arms [radius] long with a waist of 0.3. */
private fun DrawScope.sparkle(centre: Offset, radius: Float, color: Color) {
    val waist = radius * 0.3f
    val path = Path().apply {
        moveTo(centre.x, centre.y - radius)
        lineTo(centre.x + waist, centre.y - waist)
        lineTo(centre.x + radius, centre.y)
        lineTo(centre.x + waist, centre.y + waist)
        lineTo(centre.x, centre.y + radius)
        lineTo(centre.x - waist, centre.y + waist)
        lineTo(centre.x - radius, centre.y)
        lineTo(centre.x - waist, centre.y - waist)
        close()
    }
    drawPath(path, color)
}

/** The three approved benefit lines (CLAUDE.md §6.8), in this order. */
@Composable
private fun Benefits() {
    Column(verticalArrangement = Arrangement.spacedBy(D.BenefitGap)) {
        Benefit(TabPetTint, stringResource(R.string.paywall_benefit_pet)) {
            PetView(
                energy = D.BenefitPetEnergy,
                mood = Mood.TIRED,
                animated = false,
                modifier = Modifier.size(D.BenefitPetWidth, D.BenefitPetHeight).clearAndSetSemantics {},
            )
        }
        Benefit(BreakNeckTint, stringResource(R.string.paywall_benefit_nudges)) {
            Image(painterResource(R.drawable.ic_break_neck), contentDescription = null, modifier = Modifier.size(D.BenefitNeckIcon))
        }
        Benefit(MoodHappyChip, stringResource(R.string.paywall_benefit_sparks)) {
            Image(painterResource(R.drawable.ic_spark), contentDescription = null, modifier = Modifier.size(D.BenefitSparkIcon))
        }
    }
}

@Composable
private fun Benefit(tile: Color, text: String, icon: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(D.BenefitTile).background(tile, RoundedCornerShape(D.BenefitTileRadius)),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Spacer(Modifier.width(D.BenefitTextStart))
        Text(text, style = WigglebunTextStyles.FocusSub, color = Ink)
    }
}

/** The trial timeline (for a plan with a trial long enough to remind about) and the plan cards. */
@Composable
private fun PlansSection(state: PaywallUiState, actions: PaywallActions) {
    when (val plans = state.plans) {
        PlansState.Loading -> PlanPlaceholders()
        PlansState.Problem -> PlansProblem(actions.onRetry)
        is PlansState.Ready -> {
            val trialDays = state.selectedOption?.freeTrial?.days
            if (trialDays != null && trialDays > Trial.END_REMINDER_DAYS_BEFORE) TrialTimeline(trialDays)
            Plans(plans.options, state.selected, enabled = !state.busy, onPick = actions.onPick)
        }
    }
}

/**
 * Today, the day we remind, and the day of the first charge, counting today as day 1: a 7-day
 * trial reminds on day 6 and charges on day 8 (CLAUDE.md §5.5).
 */
@Composable
private fun TrialTimeline(trialDays: Int) {
    val reminderDay = trialDays - Trial.END_REMINDER_DAYS_BEFORE + 1
    val chargeDay = trialDays + 1
    Row(
        Modifier
            .fillMaxWidth()
            .background(OnboardingCard, RoundedCornerShape(D.TimelineRadius))
            .drawBehind {
                // The line joins the three dots: from the middle of the first column to the middle of the last.
                val y = (D.TimelinePaddingTop + D.TimelineDot / 2).toPx()
                val inset = D.TimelinePaddingHorizontal.toPx()
                val columns = size.width - 2 * inset
                drawLine(StepDotIdle, Offset(inset + columns / 6, y), Offset(inset + columns * 5 / 6, y), strokeWidth = D.TimelineLine.toPx())
            }
            .padding(
                start = D.TimelinePaddingHorizontal,
                end = D.TimelinePaddingHorizontal,
                top = D.TimelinePaddingTop,
                bottom = D.TimelinePaddingBottom,
            ),
    ) {
        TimelineStep(DoneGreen, stringResource(R.string.paywall_timeline_today), stringResource(R.string.paywall_timeline_today_sub), Modifier.weight(1f))
        TimelineStep(Sun, stringResource(R.string.paywall_timeline_day, reminderDay), stringResource(R.string.paywall_timeline_remind), Modifier.weight(1f))
        TimelineStep(Ink, stringResource(R.string.paywall_timeline_day, chargeDay), stringResource(R.string.paywall_timeline_charge), Modifier.weight(1f))
    }
}

@Composable
private fun TimelineStep(dot: Color, title: String, sub: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(D.TimelineTextGap)) {
        Box(
            Modifier.size(D.TimelineDot).drawBehind {
                // A ring in the card's colour keeps the line from touching the dot.
                drawCircle(OnboardingCard, radius = size.minDimension / 2 + D.TimelineDotRing.toPx())
                drawCircle(dot)
            },
        )
        Text(title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold), color = Ink, textAlign = TextAlign.Center)
        Text(sub, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = InkMuted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Plans(options: PaywallOptions, selected: PlanKind?, enabled: Boolean, onPick: (PlanKind) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = D.PlansTop).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(D.PlanGap),
    ) {
        options.all.forEach { option ->
            PlanCard(option, selected = option.kind == selected, enabled = enabled, onPick = { onPick(option.kind) }, modifier = Modifier.weight(1f))
        }
    }
}

/** One plan: its name, the store's price and the billing period; a trial shows as a badge on top. */
@Composable
private fun PlanCard(option: PlanOption, selected: Boolean, enabled: Boolean, onPick: () -> Unit, modifier: Modifier) {
    val (label, sub) = when (option.kind) {
        PlanKind.YEARLY -> R.string.paywall_plan_yearly to R.string.paywall_plan_yearly_sub
        PlanKind.MONTHLY -> R.string.paywall_plan_monthly to R.string.paywall_plan_monthly_sub
        PlanKind.LIFETIME -> R.string.paywall_plan_lifetime to R.string.paywall_plan_lifetime_sub
    }
    val outline = if (selected) Ink else ButtonBorder
    Box(modifier) {
        ChunkySurface(
            shape = RoundedCornerShape(D.PlanRadius),
            faceColor = if (selected) PaywallPlanSelected else Card,
            edgeHeight = D.PlanEdge,
            edgeColor = outline,
            border = BorderStroke(if (selected) D.PlanBorderSelected else D.PlanBorder, outline),
            onClick = onPick,
            enabled = enabled,
            role = Role.RadioButton,
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().heightIn(min = D.PlanHeight + D.PlanEdge).semantics { this.selected = selected },
        ) {
            Column(
                Modifier.padding(horizontal = D.PlanPaddingHorizontal, vertical = D.PlanPaddingVertical),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(D.PlanTextGap, Alignment.CenterVertically),
            ) {
                Text(
                    stringResource(label),
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = D.PlanLabelSize),
                    color = Ink,
                    textAlign = TextAlign.Center,
                )
                // The store's own price text: other currencies are longer, so it shrinks to fit one line.
                Text(
                    option.price,
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = D.PlanPriceSize),
                    color = Ink,
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = D.PlanPriceMinSize, maxFontSize = D.PlanPriceSize),
                )
                Text(
                    stringResource(sub),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = D.PlanSubSize),
                    color = InkMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
        option.freeTrial?.let { trial ->
            Text(
                pluralStringResource(R.plurals.paywall_days_free, trial.days, trial.days),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = D.BadgeSize),
                color = Card,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = -D.BadgeLift)
                    .background(StatusOnText, WigglebunShapes.Pill)
                    .padding(horizontal = D.BadgePaddingHorizontal, vertical = D.BadgePaddingVertical),
            )
        }
    }
}

/** Where the plans will be while the store answers. */
@Composable
private fun PlanPlaceholders() {
    Row(
        Modifier.fillMaxWidth().padding(top = D.PlansTop).clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(D.PlanGap),
    ) {
        repeat(PlanKind.entries.size) {
            Box(Modifier.weight(1f).height(D.PlanHeight + D.PlanEdge).background(Track, RoundedCornerShape(D.PlanRadius)))
        }
    }
}

@Composable
private fun PlansProblem(onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(OnboardingCard, RoundedCornerShape(D.TimelineRadius)).padding(D.ProblemPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(D.ProblemGap),
    ) {
        Text(
            stringResource(R.string.paywall_plans_problem),
            style = MaterialTheme.typography.bodyMedium.balanced(),
            color = Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        ChunkyButton(text = stringResource(R.string.paywall_retry), onClick = onRetry, style = ChunkyButtonStyle.Secondary)
    }
}

/** The button's label and the small print for one plan, from what the store says about it. */
private class PaywallCopy(val cta: String, val finePrint: String)

@Composable
private fun paywallCopy(option: PlanOption): PaywallCopy {
    val priceLine = when (option.kind) {
        PlanKind.YEARLY -> stringResource(R.string.paywall_price_yearly, option.price)
        PlanKind.MONTHLY -> stringResource(R.string.paywall_price_monthly, option.price)
        PlanKind.LIFETIME -> option.price
    }
    val trial = option.freeTrial
    return when {
        option.kind == PlanKind.LIFETIME ->
            PaywallCopy(stringResource(R.string.paywall_cta_lifetime), stringResource(R.string.paywall_fine_lifetime, option.price))
        // "Start free trial" only when the store's offer really has a free phase (CLAUDE.md §5.5).
        trial != null -> {
            val daysFree = pluralStringResource(R.plurals.paywall_days_free, trial.days, trial.days)
            val finePrint = if (trial.days > Trial.END_REMINDER_DAYS_BEFORE) {
                stringResource(R.string.paywall_fine_trial, daysFree, priceLine, Trial.END_REMINDER_DAYS_BEFORE)
            } else {
                stringResource(R.string.paywall_fine_trial_short, daysFree, priceLine)
            }
            PaywallCopy(stringResource(R.string.paywall_cta_trial, trial.days), finePrint)
        }
        else -> PaywallCopy(
            stringResource(if (option.kind == PlanKind.YEARLY) R.string.paywall_cta_yearly else R.string.paywall_cta_monthly),
            stringResource(R.string.paywall_fine_subscription, priceLine),
        )
    }
}

/** A notice if the last try didn't work, the button, the small print and the legal links. */
@Composable
private fun Footer(state: PaywallUiState, actions: PaywallActions, modifier: Modifier = Modifier) {
    val option = state.selectedOption
    val copy = option?.let { paywallCopy(it) }
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(D.FooterGap),
    ) {
        state.notice?.let { notice ->
            Text(
                stringResource(notice.text),
                style = MaterialTheme.typography.bodyMedium,
                color = Coral,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        // With no plans there is nothing to buy: "Try again" in the panel is the way on.
        if (state.plans != PlansState.Problem) {
            ChunkyButton(
                text = copy?.cta ?: stringResource(R.string.paywall_cta_loading),
                onClick = actions.onPurchase,
                enabled = option != null && !state.busy,
                wrap = true,
                sound = FeelSound.TapSoft,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (copy != null) {
            Text(
                copy.finePrint,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = D.FinePrintSize,
                    lineHeight = D.FinePrintLineHeight,
                ),
                color = InkMuted,
                textAlign = TextAlign.Center,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(D.LinkGap)) {
            LegalLink(stringResource(R.string.paywall_terms), actions.onOpenTerms)
            LegalLink(stringResource(R.string.paywall_privacy), actions.onOpenPrivacy)
        }
    }
}

@Composable
private fun LegalLink(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .defaultMinSize(minWidth = D.LinkMinSize, minHeight = D.LinkMinSize)
            .clip(WigglebunShapes.Pill)
            .tickClickable(onClick = onClick)
            .padding(horizontal = D.LinkPaddingHorizontal),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall.copy(fontSize = D.LinkSize), color = Ink)
    }
}

/** States for previews and screenshot tests. Prices are sample data, as Play would format them in the US. */
internal object PaywallPreviewStates {
    private val options = FakePremiumRepository.SAMPLE_OPTIONS
    val yearly = PaywallUiState(PlansState.Ready(options), selected = PlanKind.YEARLY)
    val monthly = yearly.copy(selected = PlanKind.MONTHLY)
    val lifetime = yearly.copy(selected = PlanKind.LIFETIME)

    /** The yearly offer without a free phase: no badge, no timeline, no "free trial" on the button. */
    val yearlyNoTrial = PaywallUiState(
        PlansState.Ready(options.copy(yearly = options.yearly?.copy(freeTrial = null))),
        selected = PlanKind.YEARLY,
    )

    /** Long price texts, as in currencies with big numbers. */
    val longPrices = PaywallUiState(
        PlansState.Ready(
            PaywallOptions(
                yearly = options.yearly?.copy(price = "Rp 499.000,00"),
                monthly = options.monthly?.copy(price = "Rp 79.000,00"),
                lifetime = options.lifetime?.copy(price = "Rp 999.000,00"),
            ),
        ),
        selected = PlanKind.YEARLY,
    )
    val loading = PaywallUiState()
    val problem = PaywallUiState(PlansState.Problem)
    val notice = yearly.copy(notice = PaywallNotice.Network)
    val busy = yearly.copy(busy = true)
}

@Composable
private fun PreviewPaywall(state: PaywallUiState, petName: String = "Bun") {
    WigglebunTheme { PaywallScreen(state, petName, PaywallActions()) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun PaywallYearlyPreview() = PreviewPaywall(PaywallPreviewStates.yearly)

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun PaywallMonthlyPreview() = PreviewPaywall(PaywallPreviewStates.monthly)

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun PaywallLifetimePreview() = PreviewPaywall(PaywallPreviewStates.lifetime)

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun PaywallYearlyNoTrialPreview() = PreviewPaywall(PaywallPreviewStates.yearlyNoTrial)

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun PaywallLongPricesPreview() = PreviewPaywall(PaywallPreviewStates.longPrices)

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun PaywallLoadingPreview() = PreviewPaywall(PaywallPreviewStates.loading)

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun PaywallProblemPreview() = PreviewPaywall(PaywallPreviewStates.problem)

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun PaywallNoticePreview() = PreviewPaywall(PaywallPreviewStates.notice)

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun PaywallLongNamePreview() = PreviewPaywall(PaywallPreviewStates.yearly, petName = "WWWWWWWWWWWW")

@Preview(widthDp = 360, heightDp = 640)
@Composable
internal fun PaywallSmallPhonePreview() = PreviewPaywall(PaywallPreviewStates.yearly)

@Preview(widthDp = 360, heightDp = 640, fontScale = 1.3f)
@Composable
internal fun PaywallSmallLargeFontPreview() = PreviewPaywall(PaywallPreviewStates.yearly)

@Preview(widthDp = 360, heightDp = 640, fontScale = 2.0f)
@Composable
internal fun PaywallSmallHugeFontPreview() = PreviewPaywall(PaywallPreviewStates.yearly)
