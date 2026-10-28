package com.kartikkhandelwal.wigglebun.ui.pet

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import com.kartikkhandelwal.wigglebun.R
import java.text.BreakIterator

/**
 * A sentence with the pet's name in it ("Bun is getting tired") whose height never depends on the
 * name. If the whole name would make the sentence take more lines than a one-letter name does,
 * only the name is shortened: "Wigglebo… is getting tired". A plain end ellipsis would cut the
 * words that carry the meaning ("Wigglebottom is getting ti…"), and wrapping would make the card
 * grow. The words still wrap when the font is large, as they need to; TalkBack reads the whole
 * sentence with the whole name.
 *
 * It needs the available width (BoxWithConstraints), so don't put it under an IntrinsicSize parent.
 *
 * @param template string resource with %1$s for the name.
 */
@Composable
fun PetNameSentence(
    @StringRes template: Int,
    name: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    val sentence = stringResource(template) // not formatted: still holds %1$s
    val shortened = stringResource(R.string.pet_name_shortened)
    val full = sentence.format(name)
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier.clearAndSetSemantics { text = AnnotatedString(full) }) {
        val maxWidth = constraints.maxWidth
        val shown = remember(sentence, shortened, name, style, maxWidth, maxLines, measurer) {
            fitPetName(measurer, sentence, shortened, name, style, maxWidth, maxLines)
        }
        Text(shown, style = style, color = color, textAlign = textAlign, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * [template] filled with [name], the name cut short with [shortenedTemplate] ("%1$s…") just
 * enough that the sentence takes no more lines in [maxWidth] px than with a one-letter name
 * (and no more than [maxLines]). Cuts fall between characters as the user sees them, so an
 * emoji or accented letter is never split.
 */
internal fun fitPetName(
    measurer: TextMeasurer,
    template: String,
    shortenedTemplate: String,
    name: String,
    style: TextStyle,
    maxWidth: Int,
    maxLines: Int = Int.MAX_VALUE,
): String {
    val full = template.format(name)
    if (maxWidth == Constraints.Infinity) return full
    val ends = characterEnds(name)
    if (ends.size <= 1) return full

    fun lines(text: String) = measurer.measure(text, style, constraints = Constraints(maxWidth = maxWidth)).lineCount
    fun shortenedTo(index: Int) = template.format(shortenedTemplate.format(name.substring(0, ends[index]).trimEnd()))

    val allowed = minOf(lines(shortenedTo(0)), maxLines)
    if (lines(full) <= allowed) return full
    // Longest prefix that still fits; the last end is the whole name, which didn't.
    var low = 0
    var high = ends.size - 2
    while (low < high) {
        val mid = (low + high + 1) / 2
        if (lines(shortenedTo(mid)) <= allowed) low = mid else high = mid - 1
    }
    return shortenedTo(low)
}

/** End index of each character the user sees (grapheme), in order. */
private fun characterEnds(text: String): List<Int> {
    val iterator = BreakIterator.getCharacterInstance().apply { setText(text) }
    return generateSequence { iterator.next().takeIf { it != BreakIterator.DONE } }.toList()
}
