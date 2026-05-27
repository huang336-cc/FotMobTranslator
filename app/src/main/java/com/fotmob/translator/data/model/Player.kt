package com.fotmob.translator.data.model

import com.google.gson.annotations.SerializedName

data class Player(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("shortName")
    val shortName: String? = null,
    @SerializedName("firstName")
    val firstName: String? = null,
    @SerializedName("lastName")
    val lastName: String? = null,
    @SerializedName("team")
    val playerTeam: PlayerTeam? = null,
    @SerializedName("position")
    val position: String? = null,
    @SerializedName("country")
    val country: PlayerCountry? = null,
    @SerializedName("imageUrl")
    val imageUrl: String? = null,
    @SerializedName("dateOfBirthTimestamp")
    val dateOfBirthTimestamp: Long? = null,
    @SerializedName("seasonStats")
    val seasonStats: List<SeasonStat>? = null,
    @SerializedName("rating")
    val rating: Double? = null,
    @SerializedName("goals")
    val goals: Int? = null,
    @SerializedName("assists")
    val assists: Int? = null,
    @SerializedName("appearances")
    val appearances: Int? = null
) {
    val displayPosition: String
        get() = when (position?.lowercase()) {
            "gk" -> "GK"
            "cb", "lb", "rb", "lwb", "rwb" -> "Def"
            "cm", "cdm", "cam", "lm", "rm", "am" -> "Mid"
            "lw", "rw", "cf", "st" -> "Fwd"
            else -> position ?: "N/A"
        }

    val displayRating: String
        get() = rating?.let { String.format("%.2f", it) } ?: "N/A"
}

data class PlayerTeam(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("shortName")
    val shortName: String? = null,
    @SerializedName("imageUrl")
    val imageUrl: String? = null
)

data class PlayerCountry(
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("code")
    val code: String? = null,
    @SerializedName("imageUrl")
    val imageUrl: String? = null
)

data class SeasonStat(
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("value")
    val value: Double? = null,
    @SerializedName("rank")
    val rank: Int? = null
)
