package com.kartikkhandelwal.wigglebun.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.kartikkhandelwal.wigglebun.R

/**
 * The hosted legal pages, in one place so Settings and the paywall (M10b) link the same URLs.
 * Play also wants the privacy URL in the store listing and the Data safety form: keep them equal.
 */
object LegalLinks {
    const val PRIVACY_URL = "https://wigglebun-legal.vercel.app/privacy"
    const val TERMS_URL = "https://wigglebun-legal.vercel.app/terms"

    /**
     * Opens [url] in the browser. A phone with no browser (or a disabled one) throws
     * [ActivityNotFoundException]; we say so in a toast instead of crashing.
     */
    fun open(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.legal_no_browser, Toast.LENGTH_LONG).show()
        }
    }
}
