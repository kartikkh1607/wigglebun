package com.kartikkhandelwal.wigglebun

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.ui.navigation.WigglebunNavHost
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** A break the widget asked to start; cleared once the nav host has opened it. */
    private var pendingBreak by mutableStateOf<BreakType?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only on a fresh launch: after rotation/process restore the session is already on the back stack.
        if (savedInstanceState == null) pendingBreak = intent.startBreak()
        // The app is light-only, so force dark system-bar icons even when the phone is in dark mode.
        val lightBars = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = lightBars, navigationBarStyle = lightBars)
        setContent {
            WigglebunTheme {
                WigglebunNavHost(startBreak = pendingBreak, onStartBreakHandled = { pendingBreak = null })
            }
        }
    }

    /** The app was already open: the widget's intent arrives here (singleTop + CLEAR_TOP). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.startBreak()?.let { pendingBreak = it }
    }

    companion object {
        private const val EXTRA_START_BREAK = "start_break"

        private fun Intent.startBreak(): BreakType? =
            getStringExtra(EXTRA_START_BREAK)?.let { name -> BreakType.entries.firstOrNull { it.name == name } }

        /**
         * Opens the break session for [type] (the medium widget's "Take a break").
         * CLEAR_TOP + SINGLE_TOP reuse a running MainActivity and deliver this via onNewIntent;
         * NEW_TASK alone would only bring the old task forward and drop the extra.
         */
        fun startBreakIntent(context: Context, type: BreakType): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_START_BREAK, type.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
