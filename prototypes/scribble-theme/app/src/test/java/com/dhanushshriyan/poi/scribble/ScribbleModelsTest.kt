package com.dhanushshriyan.poi.scribble

import org.junit.Assert.assertEquals
import org.junit.Test

class ScribbleModelsTest {
    @Test
    fun filterDemoEvents_combinesDayDistanceAndSearch() {
        val result = filterDemoEvents(
            events = demoEvents,
            selectedDay = 26,
            query = "music",
            maxDistanceKm = 5,
        )

        assertEquals(listOf("rooftop-radio"), result.map(DemoEvent::id))
    }

    @Test
    fun nextRadius_cyclesThroughPrototypeChoices() {
        assertEquals(15, nextRadius(5))
        assertEquals(25, nextRadius(15))
        assertEquals(null, nextRadius(25))
        assertEquals(5, nextRadius(null))
    }
}
