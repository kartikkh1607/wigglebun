package com.kartikkhandelwal.wigglebun.activity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovementPermissions @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Whether the runtime permission is needed on this device (Android 10 / API 29+). */
    val needsRuntimePermission: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /**
     * API 29+: android.permission.ACTIVITY_RECOGNITION is a runtime permission and gates both
     * the Transition API and TYPE_STEP_COUNTER. API 28 and below: the Play services
     * permission in the manifest is granted at install, and the step counter needs nothing.
     */
    fun granted(): Boolean =
        !needsRuntimePermission ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    /** Activity Recognition lives in Google Play services; without it we fall back to steps + manual Done. */
    fun playServicesAvailable(): Boolean =
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
}
