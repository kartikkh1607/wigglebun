package com.kartikkhandelwal.wigglebun.ui.setup

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kartikkhandelwal.wigglebun.ui.collection.PetSheet
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.widget.WigglebunWidgetReceiver

private const val TAG = "Setup"

/**
 * The flow behind every setup card: tap → explanation sheet → the system's own dialog or screen.
 * [content] draws the cards and gets `open(card)`. [perform] is separate so a test can check that
 * nothing happens until the user has read the explanation.
 */
@Composable
fun SetupCardsSheetHost(
    state: SetupCardsUi,
    perform: (SetupCardUi) -> Unit,
    content: @Composable (open: (SetupCard) -> Unit) -> Unit,
) {
    var explaining by rememberSaveable { mutableStateOf<SetupCard?>(null) }
    content { explaining = it }
    state.cards.firstOrNull { it.card == explaining }?.let { ui ->
        PetSheet(onDismiss = { explaining = null }) {
            SetupExplainContent(
                ui = ui,
                onPrimary = {
                    explaining = null
                    perform(ui)
                },
                onClose = { explaining = null },
            )
        }
    }
}

/** Connects the sheet host to the view model and to Android. Used by Settings and by Home. */
@Composable
fun SetupCardsRoute(
    viewModel: SetupCardsViewModel,
    content: @Composable (state: SetupCardsUi, open: (SetupCard) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val shouldShowRationale: (String) -> Boolean = { permission ->
        activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
    // Back from the phone's settings or the system dialog lands here too.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh(shouldShowRationale) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.refresh(shouldShowRationale)
        if (granted) viewModel.onGranted()
    }
    val loaded = state ?: return
    SetupCardsSheetHost(
        state = loaded,
        perform = { ui ->
            when (val action = ui.action) {
                is SetupAction.RequestPermission -> {
                    viewModel.onAsking(ui.card)
                    requestPermission.launch(action.permission)
                }
                SetupAction.EnableMovement -> viewModel.enableMovement()
                SetupAction.PinWidget -> {
                    viewModel.onAsking(ui.card)
                    context.requestPinWidget()
                }
                SetupAction.OpenNotificationSettings -> context.openSettings(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                )
                SetupAction.OpenAppSettings -> context.openSettings(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                )
                SetupAction.None -> Unit
            }
        },
        content = { open -> content(loaded, open) },
    )
}

private fun Context.requestPinWidget() {
    AppWidgetManager.getInstance(this).requestPinAppWidget(ComponentName(this, WigglebunWidgetReceiver::class.java), null, null)
}

private fun Context.openSettings(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Some phones have no such screen. The card stays as it is, so nothing is lost.
        Log.w(TAG, "No settings screen for ${intent.action}", e)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Home's one card at a time, or nothing at all (so Home looks as it always has). */
@Composable
fun HomeSetupPromptRoute(viewModel: SetupCardsViewModel = hiltViewModel()) {
    SetupCardsRoute(viewModel) { state, open ->
        state.homePrompt?.let { prompt ->
            SetupHomePrompt(
                ui = prompt,
                onOpen = { open(prompt.card) },
                onNotNow = { viewModel.notNow(prompt.card) },
                modifier = Modifier.padding(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, top = Dimens.Setup.HomePromptTop),
            )
        }
    }
}
