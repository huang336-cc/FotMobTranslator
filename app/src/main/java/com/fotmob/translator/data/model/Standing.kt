package com.fotmob.translator.data.model

import com.google.gson.annotations.SerializedName

data class Standing(
    @SerializedName("rank")
    val rank: Int,
    @SerializedName("team")
    val team: StandingTeam? = null,
    @SerializedName("played")
    val played: Int = 0,
    @SerializedName("wins")
    val wins: Int = 0,
    @SerializedName("draws")
    val draws: Int = 0,
    @SerializedName("losses")
    val losses: Int = 0,
    @SerializedName("goalsFor")
    val goalsFor: Int = 0,
    @SerializedName("goalsAgainst")
    val goalsAgainst: Int = 0,
    @SerializedName("points")
    val points: Int = 0,
    @SerializedName("goalDifference")
    val goalDifference: Int = 0
) {
    val goalDiffText: String
        get() = when {
            goalDifference > 0 -> "+$goalDifference"
            else -> goalDifference.toString()
        }
}

data class StandingTeam(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("shortName")
    val shortName: String? = null,
    @SerializedName("imageUrl")
    val imageUrl: String? = null
)
