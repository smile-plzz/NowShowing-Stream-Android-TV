package com.nowshowing.tv.data.model

import com.google.gson.annotations.SerializedName

data class OmdbSearchResponse(
    @SerializedName("Search") val search: List<OmdbTitleShort>?,
    @SerializedName("totalResults") val totalResults: String?,
    @SerializedName("Response") val response: String,
    @SerializedName("Error") val error: String?
)

data class OmdbTitleShort(
    @SerializedName("Title") val title: String,
    @SerializedName("Year") val year: String,
    @SerializedName("imdbID") val imdbID: String,
    @SerializedName("Type") val type: String,
    @SerializedName("Poster") val poster: String
)

data class OmdbRating(
    @SerializedName("Source") val source: String,
    @SerializedName("Value") val value: String
)

data class OmdbTitleDetail(
    @SerializedName("Title") val title: String,
    @SerializedName("Year") val year: String,
    @SerializedName("Rated") val rated: String?,
    @SerializedName("Released") val released: String?,
    @SerializedName("Runtime") val runtime: String?,
    @SerializedName("Genre") val genre: String?,
    @SerializedName("Director") val director: String?,
    @SerializedName("Writer") val writer: String?,
    @SerializedName("Actors") val actors: String?,
    @SerializedName("Plot") val plot: String?,
    @SerializedName("Language") val language: String?,
    @SerializedName("Country") val country: String?,
    @SerializedName("Awards") val awards: String?,
    @SerializedName("Poster") val poster: String?,
    @SerializedName("Ratings") val ratings: List<OmdbRating>?,
    @SerializedName("Metascore") val metascore: String?,
    @SerializedName("imdbRating") val imdbRating: String?,
    @SerializedName("imdbVotes") val imdbVotes: String?,
    @SerializedName("imdbID") val imdbID: String,
    @SerializedName("Type") val type: String?,
    @SerializedName("totalSeasons") val totalSeasons: String?,
    @SerializedName("Response") val response: String,
    @SerializedName("Error") val error: String?
)

data class OmdbSeasonResponse(
    @SerializedName("Title") val title: String,
    @SerializedName("Season") val season: String,
    @SerializedName("totalSeasons") val totalSeasons: String?,
    @SerializedName("Episodes") val episodes: List<OmdbEpisode>?,
    @SerializedName("Response") val response: String,
    @SerializedName("Error") val error: String?
)

data class OmdbEpisode(
    @SerializedName("Title") val title: String,
    @SerializedName("Released") val released: String?,
    @SerializedName("Episode") val episode: String,
    @SerializedName("imdbRating") val imdbRating: String?,
    @SerializedName("imdbID") val imdbID: String
)
