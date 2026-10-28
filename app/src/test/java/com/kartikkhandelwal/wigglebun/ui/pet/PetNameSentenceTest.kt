package com.kartikkhandelwal.wigglebun.ui.pet

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTextStyles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w390dp-h844dp-xxhdpi", application = Application::class)
class PetNameSentenceTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val style = WigglebunTextStyles.Chip
    private val template = "%1\$s is getting tired"
    private val shortened = "%1\$s…"

    /** 150 dp at xxhdpi: about the Home chip's text width on a small phone. */
    private val width = 450

    private fun measurer(): TextMeasurer {
        lateinit var measurer: TextMeasurer
        composeRule.setContent { measurer = rememberTextMeasurer() }
        composeRule.waitForIdle()
        return measurer
    }

    private fun TextMeasurer.lines(text: String) = measure(text, style, constraints = Constraints(maxWidth = width)).lineCount

    @Test
    fun aNameThatFitsIsLeftAlone() {
        val measurer = measurer()
        assertEquals("Bun is getting tired", fitPetName(measurer, template, shortened, "Bun", style, width))
    }

    @Test
    fun aLongNameIsShortenedAndTheWordsStay() {
        val measurer = measurer()
        val shown = fitPetName(measurer, template, shortened, TestPetNames.WIDEST, style, width)
        assertTrue(shown, shown.matches(Regex("W+… is getting tired")))
        assertEquals(1, measurer.lines(shown))
        // As much of the name as fits: one more letter would wrap.
        val oneMore = shown.replace("…", "W…")
        assertEquals(2, measurer.lines(oneMore))
    }

    @Test
    fun aCutNeverSplitsAnEmoji() {
        val measurer = measurer()
        val shown = fitPetName(measurer, template, shortened, "Bun" + "🐰".repeat(10), style, width)
        assertTrue(shown, shown.contains("…"))
        shown.forEachIndexed { i, c ->
            if (c.isHighSurrogate()) assertTrue("unpaired at $i in $shown", shown.getOrNull(i + 1)?.isLowSurrogate() == true)
            if (c.isLowSurrogate()) assertTrue("unpaired at $i in $shown", shown.getOrNull(i - 1)?.isHighSurrogate() == true)
        }
    }

    @Test
    fun noWidthLimitShowsTheWholeName() {
        val measurer = measurer()
        assertEquals("${TestPetNames.WIDEST} is getting tired", fitPetName(measurer, template, shortened, TestPetNames.WIDEST, style, Constraints.Infinity))
    }

    /** The card around it keeps its height whatever the name; TalkBack still gets the whole name. */
    @Test
    fun heightDoesNotDependOnTheName() {
        composeRule.setContent {
            Column {
                PetNameSentence(R.string.home_chip_tired, "Bun", style = style, modifier = Modifier.width(150.dp).testTag("short"))
                PetNameSentence(R.string.home_chip_tired, TestPetNames.WIDEST, style = style, modifier = Modifier.width(150.dp).testTag("long"))
            }
        }
        val short = composeRule.onNodeWithTag("short").fetchSemanticsNode().size.height
        val long = composeRule.onNodeWithTag("long").fetchSemanticsNode().size.height
        assertEquals(short, long)
        composeRule.onNodeWithTag("long").assertTextEquals("${TestPetNames.WIDEST} is getting tired")
    }
}
