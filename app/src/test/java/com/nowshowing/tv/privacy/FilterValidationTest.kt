package com.nowshowing.tv.privacy

import org.junit.Assert.*
import org.junit.Test

class FilterValidationTest {
    private val valid = "! Title: rules\n" + (1..8).joinToString("\n") { "||ads$it.example^" }
    @Test fun acceptsRuleLists() { assertTrue(validFilterContent(valid)) }
    @Test fun rejectsHtmlEmptyAndTruncatedResponses() {
        assertFalse(validFilterContent("<html>Access denied</html>"))
        assertFalse(validFilterContent("! comments only"))
        assertFalse(validFilterContent("||single.example^"))
        assertFalse(validFilterContent(valid + "\nnot a filter response"))
        assertFalse(validFilterContent(valid.repeat(10000)))
    }
}
