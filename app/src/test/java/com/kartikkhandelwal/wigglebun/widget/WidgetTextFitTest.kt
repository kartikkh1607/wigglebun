package com.kartikkhandelwal.wigglebun.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetTextFitTest {
    @Test
    fun fontScaleDecidesWhatGivesWay() {
        assertEquals(WidgetTextFit.REGULAR, WidgetTextFit.of(0.85f))
        assertEquals(WidgetTextFit.REGULAR, WidgetTextFit.of(1f))
        assertEquals(WidgetTextFit.LARGE, WidgetTextFit.of(1.15f))
        assertEquals(WidgetTextFit.LARGE, WidgetTextFit.of(1.3f))
        assertEquals(WidgetTextFit.HUGE, WidgetTextFit.of(1.8f))
        assertEquals(WidgetTextFit.HUGE, WidgetTextFit.of(2f))
    }
}
