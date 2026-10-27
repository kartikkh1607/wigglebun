package com.kartikkhandelwal.wigglebun.ui.debug

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Debug-only: the Pet lab exists only in debug builds. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w390dp-h2200dp-xxhdpi", application = Application::class)
class PetLabScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun petLab() {
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) { PetLabPreview() }
        }
        composeRule.onRoot().captureRoboImage("pet_lab.png")
    }
}
