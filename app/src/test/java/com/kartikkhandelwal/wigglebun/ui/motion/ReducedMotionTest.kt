package com.kartikkhandelwal.wigglebun.ui.motion

import android.app.Application
import android.os.Looper
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** The system "Remove animations" setting, as [LocalReducedMotion] reads it. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class ReducedMotionTest {

    private val resolver = ApplicationProvider.getApplicationContext<Application>().contentResolver
    private val scaleUri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)

    /** As Settings does: write the value, then tell observers. The observer runs on the main looper. */
    private fun setScale(scale: Float) {
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, scale)
        resolver.notifyChange(scaleUri, null)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @After
    fun restoreTheScale() = setScale(1f)

    @Test
    fun onlyAScaleOfZeroRemovesAnimations() {
        assertFalse("never set: the default 1×", resolver.reducedMotion())
        setScale(0f)
        assertTrue(resolver.reducedMotion())
        // Developer options' 0.5× still animates, just faster.
        setScale(0.5f)
        assertFalse(resolver.reducedMotion())
        setScale(1f)
        assertFalse(resolver.reducedMotion())
    }

    @Test
    fun followsTheSettingWhileCollected() {
        val seen = mutableListOf<Boolean>()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        scope.launch { resolver.reducedMotionFlow().collect { seen += it } }
        try {
            assertEquals("the current value at once", listOf(false), seen)
            setScale(0f)
            setScale(0f) // a repeat notification changes nothing
            setScale(1f)
            assertEquals(listOf(false, true, false), seen)
        } finally {
            scope.cancel()
        }
        // Stopped collecting: the observer is gone, so later changes aren't seen.
        setScale(0f)
        assertEquals(listOf(false, true, false), seen)
    }

    @Test
    fun aNewCollectionReadsTheSettingAgain() {
        // Changed while nothing was collecting, e.g. in system Settings while the app was away.
        setScale(0f)
        val seen = mutableListOf<Boolean>()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        scope.launch { resolver.reducedMotionFlow().collect { seen += it } }
        scope.cancel()
        assertEquals(listOf(true), seen)
    }
}
