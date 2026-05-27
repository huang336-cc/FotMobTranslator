package com.fotmob.translator.data.model

import com.google.gson.annotations.SerializedName

data class Team(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("shortName")
    val shortName: String? = null,
    @SerializedName("imageUrl")
    val imageUrl: String? = null,
    @SerializedName("country")
    val country: String? = null
) {
    companion object {
        fun empty(): Team = Team(id = 0, name = "Unknown")
    }
}
