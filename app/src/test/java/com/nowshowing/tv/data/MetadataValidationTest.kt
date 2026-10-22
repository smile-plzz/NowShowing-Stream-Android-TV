package com.nowshowing.tv.data

import com.google.gson.Gson
import com.nowshowing.tv.data.model.OmdbTitleDetail
import com.nowshowing.tv.data.model.isUsable
import org.junit.Assert.*
import org.junit.Test

class MetadataValidationTest {
    @Test fun gsonMissingRequiredFieldsNeverReachCards() {
        listOf("{}", """{"Response":"True"}""",
            """{"Response":"True","imdbID":"tt1375666","Title":null,"Year":"2010","Type":"movie"}""").forEach {
            assertFalse(Gson().fromJson(it, OmdbTitleDetail::class.java).isUsable())
        }
        assertTrue(Gson().fromJson("""{"Response":"True","imdbID":"tt1375666","Title":"Inception","Year":"2010","Type":"movie"}""", OmdbTitleDetail::class.java).isUsable())
    }
}

