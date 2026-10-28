package com.kartikkhandelwal.wigglebun.widget

import android.app.Application
import android.os.Looper
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.drawToBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.pet.previewEnergy
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the widget's pet images, res/drawable-nodpi/bun_mood_<mood>.png (480×504, transparent),
 * from PetView itself, so the widget shows exactly the pet the app draws.
 *
 * They are Roborazzi reference images kept next to the app's resources: verifyRoborazziDebug fails
 * when PetView changes and they are stale, and recordRoborazziDebug rewrites them in place.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = Application::class)
class WidgetPetImageTest {

    @Test fun happy() = render(Mood.HAPPY, "happy")

    @Test fun tired() = render(Mood.TIRED, "tired")

    @Test fun stiff() = render(Mood.STIFF, "stiff")

    @Test fun stretch() = render(Mood.STRETCH, "stretch")

    @Test fun celebrate() = render(Mood.CELEBRATE, "celebrate")

    @Test fun sleeping() = render(Mood.SLEEPING, "sleep")

    private fun render(mood: Mood, fileMood: String) {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val view = ComposeView(activity).apply {
            setContent {
                WigglebunTheme {
                    // Glance can't animate, so the still pose: no jump, bob or breath, feet on the
                    // ground. The 200×210 viewBox fills 480×504 px exactly (2.4×).
                    PetView(energy = mood.previewEnergy(), mood = mood, animated = false, modifier = Modifier.fillMaxSize())
                }
            }
        }
        activity.setContentView(view, ViewGroup.LayoutParams(WIDTH_PX, HEIGHT_PX))
        shadowOf(Looper.getMainLooper()).idle()
        // Drawn straight from the view, so no window background: the PNG stays transparent.
        view.drawToBitmap().captureRoboImage(File("src/main/res/drawable-nodpi/bun_mood_$fileMood.png"))
    }

    private companion object {
        const val WIDTH_PX = 480
        const val HEIGHT_PX = 504
    }
}
