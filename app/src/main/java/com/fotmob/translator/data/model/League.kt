package com.fotmob.translator.data.model

import com.google.gson.annotations.SerializedName

data class League(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("country")
    val country: String? = null,
    @SerializedName("countryCode")
    val countryCode: String? = null,
    @SerializedName("imageUrl")
    val imageUrl: String? = null,
    @SerializedName("primaryUniqueTournament")
    val primaryUniqueTournament: PrimaryUniqueTournament? = null,
    @SerializedName("uniqueTournament")
    val uniqueTournament: PrimaryUniqueTournament? = null
)

data class PrimaryUniqueTournament(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("country")
    val country: String? = null,
    @SerializedName("countryCode")
    val countryCode: String? = null,
    @SerializedName("imageUrl")
    val imageUrl: String? = null,
    @SerializedName("season")
    val season: SeasonInfo? = null
)

data class SeasonInfo(
    @SerializedName("year")
    val year: String? = null,
    @SerializedName("startDate")
    val startDate: String? = null,
    @SerializedName("endDate")
    val endDate: String? = null
)
