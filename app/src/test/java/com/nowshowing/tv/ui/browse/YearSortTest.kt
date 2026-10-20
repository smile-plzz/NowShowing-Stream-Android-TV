package com.nowshowing.tv.ui.browse

import org.junit.Assert.*
import org.junit.Test

class YearSortTest {
    @Test fun rangesAndOngoingSeriesUseFirstYear() {
        assertEquals(2011, startYear("2011–2019"))
        assertEquals(2011, startYear("2011-2019"))
        assertEquals(2024, startYear("2024–"))
        assertTrue(startYear("2023") > startYear("2011–2019"))
    }
    @Test fun unknownAndMalformedDatesSortLast() {
        listOf("N/A", "", "unknown", "20112019", "123").forEach { assertEquals(0, startYear(it)) }
    }
}
