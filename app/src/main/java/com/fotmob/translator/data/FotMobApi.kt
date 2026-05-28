package com.fotmob.translator.data

import com.fotmob.translator.data.model.League
import com.fotmob.translator.data.model.Match
import com.fotmob.translator.data.model.Player
import com.fotmob.translator.data.model.Standing
import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Query

interface FotMobApi {

    @GET("matches")
    suspend fun getMatches(
        @Query("date") date: String
    ): JsonObject

    @GET("matchDetails")
    suspend fun getMatchDetails(
        @Query("matchId") matchId: Long
    ): JsonObject

    @GET("allLeagues")
    suspend fun getAllLeagues(): JsonObject

    @GET("leagues")
    suspend fun getLeagueDetails(
        @Query("id") id: Long
    ): JsonObject

    @GET("tltable")
    suspend fun getLeagueTable(
        @Query("leagueId") leagueId: Long
    ): JsonObject

    @GET("fixtures")
    suspend fun getFixtures(
        @Query("id") id: Long,
        @Query("season") season: String? = null
    ): JsonObject

    @GET("teams")
    suspend fun getTeamInfo(
        @Query("id") id: Long
    ): JsonObject

    @GET("playerData")
    suspend fun getPlayerData(
        @Query("id") id: Long
    ): JsonObject

    @GET("simplePlayerData")
    suspend fun getSimplePlayerData(
        @Query("id") id: Long
    ): JsonObject

    @GET("https://www.fotmob.com/api/topnews/feed")
    suspend fun getNewsFeed(): String
}
