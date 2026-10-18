package com.nowshowing.tv.data.repository

import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class MetadataHealthTest {
    @After fun reset() { MetadataHealth.retry() }
    @Test fun distinguishesQuotaKeyAndNoResults() {
        assertEquals(MetadataFailure.QUOTA, classifyMetadataError("Request limit reached!"))
        assertEquals(MetadataFailure.KEY, classifyMetadataError("Invalid API key!"))
        assertEquals(MetadataFailure.NO_RESULTS, classifyMetadataError("Movie not found!"))
        assertEquals(MetadataFailure.SERVICE, classifyMetadataError("Unexpected upstream error"))
    }
    @Test fun quotaStopsRepeatedRequestsAndExpires() {
        MetadataHealth.retry()
        MetadataHealth.record(MetadataFailure.QUOTA, 1000)
        try { MetadataHealth.check(1001); fail("Quota must stop retries") } catch (error: MetadataUnavailable) {
            assertEquals(MetadataFailure.QUOTA, error.failure)
        }
        MetadataHealth.success(1002)
        assertEquals(MetadataFailure.QUOTA, MetadataHealth.failure.value)
        MetadataHealth.check(901001)
        MetadataHealth.success(901001)
        assertNull(MetadataHealth.failure.value)
    }
    @Test fun noResultsDoesNotBlockLaterSearches() {
        MetadataHealth.retry(); MetadataHealth.record(MetadataFailure.NO_RESULTS, 0)
        MetadataHealth.check(1); assertNull(MetadataHealth.failure.value)
    }
    @Test fun logIsBoundedAndContainsOnlyCategories() {
        repeat(30) { MetadataHealth.retry(); MetadataHealth.record(MetadataFailure.NETWORK, it.toLong()) }
        assertTrue(MetadataHealth.recentErrors().size <= 12)
        assertTrue(MetadataHealth.recentErrors().all { it.matches(Regex("[0-9]+: [A-Z_]+")) })
    }
}
