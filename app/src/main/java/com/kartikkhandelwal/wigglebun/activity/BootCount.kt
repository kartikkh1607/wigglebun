package com.kartikkhandelwal.wigglebun.activity

import android.content.Context
import android.provider.Settings

/**
 * Settings.Global.BOOT_COUNT goes up by one on every reboot. Both the step counter and
 * elapsedRealtime restart at 0 on reboot, so values stored with a different boot count are stale.
 */
internal fun Context.bootCount(): Int = Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, -1)
