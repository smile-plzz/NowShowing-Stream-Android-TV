package com.nowshowing.tv.data.api

import com.nowshowing.tv.data.model.OmdbSeasonResponse
import com.nowshowing.tv.data.model.OmdbSearchResponse
import com.nowshowing.tv.data.model.OmdbTitleDetail
import retrofit2.http.GET
import retrofit2.http.Query

interface OmdbApiService {

    @GET("/")
    suspend fun searchTitles(
        @Query("apikey") apiKey: String,
        @Query("s") query: String,
        @Query("page") page: Int = 1,
        @Query("type") type: String? = null
    ): OmdbSearchResponse

    @GET("/")
    suspend fun getDetails(
        @Query("apikey") apiKey: String,
        @Query("i") imdbId: String,
        @Query("plot") plot: String = "full"
    ): OmdbTitleDetail

    @GET("/")
    suspend fun getDetailsByTitle(
        @Query("apikey") apiKey: String,
        @Query("t") title: String,
        @Query("type") type: String? = null,
        @Query("plot") plot: String = "full"
    ): OmdbTitleDetail

    @GET("/")
    suspend fun getSeason(
        @Query("apikey") apiKey: String,
        @Query("i") imdbId: String,
        @Query("Season") season: Int
    ): OmdbSeasonResponse
}
