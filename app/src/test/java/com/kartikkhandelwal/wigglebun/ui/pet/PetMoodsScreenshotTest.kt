package com.kartikkhandelwal.wigglebun.ui.pet

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.github.takahirom.roborazzi.captureRoboImage
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Every collection pet in every mood at 384 dpi, so the 200×210 dp pet is exactly 480×504 px like
 * design/pets/<pet>/widget_png/<pet>_mood_<mood>.png, for a pixel comparison with the art.
 * Reference PNGs: app/src/test/screenshots/pet_<pet>_<mood>.png.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-384dpi", application = Application::class)
class PetMoodsScreenshotTest(private val pet: PetId, private val mood: Mood) {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mood() {
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) { PetMoodPreview(mood, PetSpecs.of(pet)) }
        }
        composeRule.onNodeWithTag("pet").captureRoboImage("pet_${pet.key}_${mood.name.lowercase()}.png")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0} {1}")
        fun cases(): List<Array<Any>> = listOf(PetId.PIP, PetId.TOAST, PetId.NIMBUS).flatMap { pet ->
            Mood.entries.map { mood -> arrayOf<Any>(pet, mood) }
        }
    }
}
