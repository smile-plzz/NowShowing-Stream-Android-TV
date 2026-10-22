package com.nowshowing.tv.data.repository

import org.junit.Assert.*
import org.junit.Test

class CloudCatalogTest {
    @Test fun parsesAndDeduplicatesIdsInEditorialOrder() {
        val data = CloudCatalog.parse("""{"version":1,"sections":{"movies":["tt1375666","tt0133093","tt1375666"],"mood_Happy":["tt0382932"]}}""")
        assertEquals(listOf("tt1375666", "tt0133093"), data["movies"])
        assertEquals(listOf("tt0382932"), data["mood_Happy"])
    }
    @Test fun rejectsMalformedOrUnboundedCatalogs() {
        listOf("null", "{}", "{", """{"version":2,"sections":{"movies":["tt1375666"]}}""",
            """{"version":1,"sections":{"movies":["https://example.com"]}}""",
            """{"version":1,"sections":{"movies":[null]}}""",
            """{"version":1,"sections":{"movies":[]}}""",
            """{"version":1,"sections":{}}""").forEach { json ->
                assertTrue(json, runCatching { CloudCatalog.parse(json) }.isFailure)
            }
        val ids = List(101) { "\"tt1375666\"" }.joinToString(",")
        assertTrue(runCatching { CloudCatalog.parse("{\"version\":1,\"sections\":{\"movies\":[$ids]}}") }.isFailure)
        assertTrue(runCatching { CloudCatalog.parse(" ".repeat(131073)) }.isFailure)
    }
}
