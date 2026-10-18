package com.nowshowing.tv.data.api

import org.junit.Assert.*
import org.junit.Test

class MetadataCredentialsTest {
    @Test fun orderedDeduplicationAndSeparators() {
        assertEquals(listOf("first123", "second123", "third123"),
            MetadataCredentials.parse(" first123,second123;first123\nthird123 "))
    }
    @Test fun rejectsMalformedAndUnboundedInput() {
        assertNull(MetadataCredentials.parse("first123,wrong!"))
        assertNull(MetadataCredentials.parse((1..6).joinToString(",") { "sample$it" }))
        assertEquals(emptyList<String>(), MetadataCredentials.parse("  "))
    }
    @Test fun staleResponsesCannotRejectNewConfiguration() {
        val selection = MetadataKeySelection()
        val keys = listOf("first123", "second123")
        assertEquals("first123", selection.next(keys, 100_000))
        selection.reject("first123", keys, 99_999)
        assertEquals("first123", selection.next(keys, 100_000))
        selection.reject("first123", keys, 100_000)
        assertEquals("second123", selection.next(keys, 100_000))
    }
}
