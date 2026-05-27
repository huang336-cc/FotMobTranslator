package com.fotmob.translator.data.model

import com.google.gson.annotations.SerializedName

data class MatchScore(
    @SerializedName("home")
    val home: Int? = null,
    @SerializedName("away")
    val away: Int? = null
)

data class MatchStatus(
    @SerializedName("finished")
    val finished: Boolean = false,
    @SerializedName("started")
    val started: Boolean = false,
    @SerializedName("cancelled")
    val cancelled: Boolean = false,
    @SerializedName("reason")
    val reason: MatchReason? = null,
    @SerializedName("scoreStr")
    val scoreStr: String? = null
)

data class MatchReason(
    @SerializedName("shortReason")
    val shortReason: String? = null,
    @SerializedName("longReason")
    val longReason: String? = null
)

data class MatchLeague(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("country")
    val country: String? = null,
    @SerializedName("imageUrl")
    val imageUrl: String? = null
)

data class Match(
    @SerializedName("id")
    val id: Long,
    @SerializedName("homeTeam")
    val homeTeam: Team? = null,
    @SerializedName("awayTeam")
    val awayTeam: Team? = null,
    @SerializedName("status")
    val status: MatchStatus? = null,
    @SerializedName("startTime")
    val startTime: Long? = null,
    @SerializedName("league")
    val league: MatchLeague? = null,
    @SerializedName("homeScore")
    val homeScore: MatchScore? = null,
    @SerializedName("awayScore")
    val awayScore: MatchScore? = null,
    @SerializedName("matchStatus")
    val matchStatus: String? = null
) {
    val displayScore: String
        get() {
            val home = homeScore?.home ?: 0
            val away = awayScore?.away ?: 0
            return if (status?.started == true) {
                "$home - $away"
            } else {
                ""
            }
        }

    val matchStateText: String
        get() = when {
            status?.finished == true -> "FT"
            status?.started == true -> matchStatus ?: "Live"
            status?.cancelled == true -> "Cancelled"
            else -> "Not Started"
        }
}
