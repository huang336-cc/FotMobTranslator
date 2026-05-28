package com.fotmob.translator.data.repository

import android.content.Context
import android.util.LruCache
import com.fotmob.translator.data.FotMobApi
import com.fotmob.translator.data.model.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class FotMobRepository(
    private val api: FotMobApi,
    private val context: Context
) {

    private val gson = Gson()
    private val memoryCache = LruCache<String, String>(100)

    // ==================== Matches ====================

    suspend fun getMatches(date: String): List<Match> = withContext(Dispatchers.IO) {
        try {
            val response = api.getMatches(date)
            parseMatches(response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseMatches(json: JsonObject): List<Match> {
        val matches = mutableListOf<Match>()

        // FotMob returns matches grouped by league/region
        val regions = json.getAsJsonArray("regions") ?: return emptyList()

        for (regionElement in regions) {
            val region = regionElement.asJsonObject
            val leagues = region.getAsJsonArray("leagues") ?: continue

            for (leagueElement in leagues) {
                val leagueObj = leagueElement.asJsonObject
                val leagueInfo = leagueObj.getAsJsonObject("uniqueTournament") ?: continue
                val league = MatchLeague(
                    id = leagueInfo.get("id")?.asLong ?: 0,
                    name = leagueInfo.get("name")?.asString ?: "Unknown",
                    country = leagueInfo.get("country")?.asString,
                    imageUrl = leagueInfo.get("imageUrl")?.asString
                )

                val matchArray = leagueObj.getAsJsonArray("matches") ?: continue
                for (matchElement in matchArray) {
                    val matchObj = matchElement.asJsonObject
                    val match = parseMatch(matchObj, league)
                    matches.add(match)
                }
            }
        }

        return matches
    }

    private fun parseMatch(obj: JsonObject, league: MatchLeague): Match {
        val homeTeamObj = obj.getAsJsonObject("home") ?: obj.getAsJsonObject("homeTeam")
        val awayTeamObj = obj.getAsJsonObject("away") ?: obj.getAsJsonObject("awayTeam")

        val homeTeam = homeTeamObj?.let {
            Team(
                id = it.get("id")?.asLong ?: 0,
                name = it.get("name")?.asString ?: "Unknown",
                shortName = it.get("shortName")?.asString,
                imageUrl = it.get("imageUrl")?.asString
            )
        }

        val awayTeam = awayTeamObj?.let {
            Team(
                id = it.get("id")?.asLong ?: 0,
                name = it.get("name")?.asString ?: "Unknown",
                shortName = it.get("shortName")?.asString,
                imageUrl = it.get("imageUrl")?.asString
            )
        }

        val statusObj = obj.getAsJsonObject("status")
        val status = statusObj?.let {
            MatchStatus(
                finished = it.get("finished")?.asBoolean ?: false,
                started = it.get("started")?.asBoolean ?: false,
                cancelled = it.get("cancelled")?.asBoolean ?: false,
                scoreStr = it.get("scoreStr")?.asString
            )
        }

        val homeScoreObj = obj.getAsJsonObject("homeScore")
        val awayScoreObj = obj.getAsJsonObject("awayScore")

        val homeScore = homeScoreObj?.let {
            MatchScore(
                home = it.get("current")?.asInt,
                away = null
            )
        }

        val awayScore = awayScoreObj?.let {
            MatchScore(
                home = null,
                away = it.get("current")?.asInt
            )
        }

        return Match(
            id = obj.get("id")?.asLong ?: 0,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            status = status,
            startTime = obj.get("startTime")?.asLong,
            league = league,
            homeScore = homeScore,
            awayScore = awayScore,
            matchStatus = status?.scoreStr
        )
    }

    // ==================== Leagues ====================

    suspend fun getAllLeagues(): List<League> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAllLeagues()
            parseAllLeagues(response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseAllLeagues(json: JsonObject): List<League> {
        val leagues = mutableListOf<League>()
        val regions = json.getAsJsonArray("regions") ?: return emptyList()

        for (regionElement in regions) {
            val region = regionElement.asJsonObject
            val tournaments = region.getAsJsonArray("tournaments") ?: continue

            for (tournamentElement in tournaments) {
                val tournament = tournamentElement.asJsonObject
                val league = League(
                    id = tournament.get("id")?.asLong ?: 0,
                    name = tournament.get("name")?.asString ?: "Unknown",
                    country = tournament.get("country")?.asString,
                    countryCode = tournament.get("countryCode")?.asString,
                    imageUrl = tournament.get("imageUrl")?.asString
                )
                leagues.add(league)
            }
        }

        return leagues
    }

    // ==================== Standings ====================

    suspend fun getLeagueTable(leagueId: Long): List<Standing> = withContext(Dispatchers.IO) {
        try {
            val response = api.getLeagueTable(leagueId)
            parseStandings(response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseStandings(json: JsonObject): List<Standing> {
        val standings = mutableListOf<Standing>()
        val tables = json.getAsJsonArray("tables") ?: return emptyList()

        for (tableElement in tables) {
            val table = tableElement.asJsonObject
            val rows = table.getAsJsonArray("rows") ?: continue

            for (rowElement in rows) {
                val row = rowElement.asJsonObject
                val teamObj = row.getAsJsonObject("team")

                val team = teamObj?.let {
                    StandingTeam(
                        id = it.get("id")?.asLong ?: 0,
                        name = it.get("name")?.asString ?: "Unknown",
                        shortName = it.get("shortName")?.asString,
                        imageUrl = it.get("imageUrl")?.asString
                    )
                }

                val standing = Standing(
                    rank = row.get("rank")?.asInt ?: 0,
                    team = team,
                    played = row.get("matches")?.asInt ?: 0,
                    wins = row.get("wins")?.asInt ?: 0,
                    draws = row.get("draws")?.asInt ?: 0,
                    losses = row.get("losses")?.asInt ?: 0,
                    goalsFor = row.get("scoresFor")?.asInt ?: 0,
                    goalsAgainst = row.get("scoresAgainst")?.asInt ?: 0,
                    points = row.get("points")?.asInt ?: 0,
                    goalDifference = (row.get("scoresFor")?.asInt ?: 0) - (row.get("scoresAgainst")?.asInt ?: 0)
                )
                standings.add(standing)
            }
        }

        return standings
    }

    // ==================== Players ====================

    suspend fun getPlayerData(playerId: Long): Player? = withContext(Dispatchers.IO) {
        try {
            val response = api.getPlayerData(playerId)
            parsePlayer(response)
        } catch (e: Exception) {
            null
        }
    }

    private fun parsePlayer(json: JsonObject): Player? {
        val playerObj = json.getAsJsonObject("playerInfo") ?: json.getAsJsonObject("player") ?: return null
        val teamObj = playerObj.getAsJsonObject("team")

        val team = teamObj?.let {
            PlayerTeam(
                id = it.get("id")?.asLong ?: 0,
                name = it.get("name")?.asString ?: "Unknown",
                shortName = it.get("shortName")?.asString,
                imageUrl = it.get("imageUrl")?.asString
            )
        }

        val countryObj = playerObj.getAsJsonObject("country")
        val country = countryObj?.let {
            PlayerCountry(
                name = it.get("name")?.asString,
                code = it.get("code")?.asString,
                imageUrl = it.get("imageUrl")?.asString
            )
        }

        // Parse season stats
        val seasonStats = mutableListOf<SeasonStat>()
        val statsObj = playerObj.getAsJsonObject("seasonStats") ?: json.getAsJsonObject("season")
        if (statsObj != null) {
            val statsArray = statsObj.getAsJsonArray("stats") ?: statsObj.getAsJsonArray("seasonStats")
            if (statsArray != null) {
                for (statElement in statsArray) {
                    val stat = statElement.asJsonObject
                    seasonStats.add(
                        SeasonStat(
                            name = stat.get("name")?.asString,
                            value = stat.get("value")?.asDouble,
                            rank = stat.get("rank")?.asInt
                        )
                    )
                }
            }
        }

        // Extract key stats
        var goals: Int? = null
        var assists: Int? = null
        var appearances: Int? = null
        var rating: Double? = null

        for (stat in seasonStats) {
            when (stat.name?.lowercase()) {
                "goals", "goal" -> goals = stat.value?.toInt()
                "assists", "assist" -> assists = stat.value?.toInt()
                "appearances", "matches played", "matchesplayed" -> appearances = stat.value?.toInt()
                "rating" -> rating = stat.value
            }
        }

        return Player(
            id = playerObj.get("id")?.asLong ?: 0,
            name = playerObj.get("name")?.asString ?: "Unknown",
            shortName = playerObj.get("shortName")?.asString,
            firstName = playerObj.get("firstName")?.asString,
            lastName = playerObj.get("lastName")?.asString,
            playerTeam = team,
            position = playerObj.get("position")?.asString,
            country = country,
            imageUrl = playerObj.get("imageUrl")?.asString,
            dateOfBirthTimestamp = playerObj.get("dateOfBirthTimestamp")?.asLong,
            seasonStats = seasonStats,
            rating = rating,
            goals = goals,
            assists = assists,
            appearances = appearances
        )
    }

    // ==================== Team Players ====================

    suspend fun getTeamPlayers(teamId: Long): List<Player> = withContext(Dispatchers.IO) {
        try {
            val response = api.getTeamInfo(teamId)
            parseTeamPlayers(response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseTeamPlayers(json: JsonObject): List<Player> {
        val players = mutableListOf<Player>()

        // Try multiple possible JSON structures
        val playersArray: JsonArray = try {
            // Structure 1: squad.players
            json.getAsJsonObject("squad")?.getAsJsonArray("players")
        } catch (e: Exception) { null }
            ?: try {
                // Structure 2: squad (array)
                json.getAsJsonArray("squad")
            } catch (e: Exception) { null }
            ?: try {
                // Structure 3: players (direct array)
                json.getAsJsonArray("players")
            } catch (e: Exception) { null }
            ?: return emptyList()

        val teamDetails = json.getAsJsonObject("details")
        val team = teamDetails?.let {
            PlayerTeam(
                id = it.get("id")?.asLong ?: 0,
                name = it.get("name")?.asString ?: "Unknown",
                shortName = it.get("shortName")?.asString,
                imageUrl = it.get("imageUrl")?.asString
            )
        }

        for (playerElement in playersArray) {
            val playerObj = playerElement.asJsonObject

            // Some entries might be objects with a "player" sub-object
            val actualPlayerObj = try {
                playerObj.getAsJsonObject("player") ?: playerObj
            } catch (e: Exception) { playerObj }

            val countryObj = actualPlayerObj.getAsJsonObject("country")
            val country = countryObj?.let {
                PlayerCountry(
                    name = it.get("name")?.asString,
                    code = it.get("code")?.asString,
                    imageUrl = it.get("imageUrl")?.asString
                )
            }

            players.add(
                Player(
                    id = actualPlayerObj.get("id")?.asLong ?: 0,
                    name = actualPlayerObj.get("name")?.asString ?: "Unknown",
                    shortName = actualPlayerObj.get("shortName")?.asString,
                    playerTeam = team,
                    position = actualPlayerObj.get("position")?.asString,
                    country = country,
                    imageUrl = actualPlayerObj.get("imageUrl")?.asString,
                    dateOfBirthTimestamp = actualPlayerObj.get("dateOfBirthTimestamp")?.asLong
                )
            )
        }

        return players
    }

    // ==================== News ====================

    suspend fun getNewsFeed(): List<NewsItem> = withContext(Dispatchers.IO) {
        try {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val request = okhttp3.Request.Builder()
                .url("https://www.fotmob.com/topnews/feed")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36")
                .build()
            val response = client.newCall(request).execute()
            val xml = response.body?.string() ?: return@withContext emptyList()
            parseNewsXml(xml)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseNewsXml(xml: String): List<NewsItem> {
        val newsItems = mutableListOf<NewsItem>()

        try {
            val doc: Document = Jsoup.parse(xml, "", org.jsoup.parser.Parser.xmlParser())
            val entries = doc.select("entry")

            for (entry in entries) {
                val title = entry.select("title").first()?.text() ?: continue
                val summary = entry.select("summary").first()?.text() ?: ""
                val link = entry.select("link[href]").first()?.attr("href") ?: ""
                val published = entry.select("published").first()?.text() ?: ""
                val updated = entry.select("updated").first()?.text() ?: ""

                // Parse date
                val timestamp = parseAtomDate(published.ifEmpty { updated })

                // Try to find thumbnail
                val thumbnailUrl = entry.select("content[type=image]").first()?.attr("src")
                    ?: entry.select("link[rel=enclosure]").first()?.attr("href")

                val source = entry.select("author > name").first()?.text()

                newsItems.add(
                    NewsItem(
                        title = title,
                        summary = summary,
                        link = link,
                        publishedTime = timestamp,
                        thumbnailUrl = thumbnailUrl,
                        source = source
                    )
                )
            }
        } catch (e: Exception) {
            // Return whatever we have parsed so far
        }

        return newsItems
    }

    private fun parseAtomDate(dateStr: String): Long {
        return try {
            val formats = arrayOf(
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssZ",
                "yyyy-MM-dd'T'HH:mm:ss"
            )
            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.US)
                    return sdf.parse(dateStr)?.time ?: 0L
                } catch (_: Exception) {
                    continue
                }
            }
            0L
        } catch (e: Exception) {
            0L
        }
    }

    // ==================== Top Players ====================

    suspend fun getLeagueTopPlayers(leagueId: Long): List<Player> = withContext(Dispatchers.IO) {
        try {
            val response = api.getLeagueDetails(leagueId)
            parseTopPlayers(response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseTopPlayers(json: JsonObject): List<Player> {
        val players = mutableListOf<Player>()
        val topPlayers = json.getAsJsonObject("topPlayers") ?: return emptyList()

        val goalsArray = topPlayers.getAsJsonArray("goals")
        goalsArray?.let { parsePlayerList(it, players) }

        val assistsArray = topPlayers.getAsJsonArray("assists")
        assistsArray?.let { parsePlayerList(it, players) }

        val ratingsArray = topPlayers.getAsJsonArray("rating")
        ratingsArray?.let { parsePlayerList(it, players) }

        return players.distinctBy { it.id }
    }

    private fun parsePlayerList(array: JsonArray, players: MutableList<Player>) {
        for (element in array) {
            val obj = element.asJsonObject
            val playerObj = obj.getAsJsonObject("player") ?: continue

            val teamObj = obj.getAsJsonObject("team")
            val team = teamObj?.let {
                PlayerTeam(
                    id = it.get("id")?.asLong ?: 0,
                    name = it.get("name")?.asString ?: "Unknown",
                    shortName = it.get("shortName")?.asString,
                    imageUrl = it.get("imageUrl")?.asString
                )
            }

            players.add(
                Player(
                    id = playerObj.get("id")?.asLong ?: 0,
                    name = playerObj.get("name")?.asString ?: "Unknown",
                    shortName = playerObj.get("shortName")?.asString,
                    playerTeam = team,
                    imageUrl = playerObj.get("imageUrl")?.asString,
                    goals = obj.get("value")?.asInt,
                    rating = obj.get("value")?.asDouble
                )
            )
        }
    }
}
