package com.example.leetgraph

import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

object LeetCodeService {
    private const val endpoint = "https://leetcode.com/graphql/"
    private const val calendarQuery = """
        query leetGraphCalendar(${"$"}username: String!, ${"$"}year: Int) {
          matchedUser(username: ${"$"}username) {
            userCalendar(year: ${"$"}year) {
              submissionCalendar
            }
          }
        }
    """

    @Throws(IOException::class, InvalidUsernameException::class)
    fun fetchSnapshot(username: String): WidgetSnapshot {
        val today = LocalDate.now(ZoneOffset.UTC)
        val startDate = today.minusDays((LeetGraphContract.MAX_WEEKS * 7L) - 1L)
        val years = linkedSetOf(startDate.year, today.year)
        val mergedCounts = mutableMapOf<LocalDate, Int>()

        for (year in years) {
            mergedCounts.putAll(fetchYear(username, year))
        }

        val visibleCounts = mutableMapOf<LocalDate, Int>()
        var totalSubmissions = 0
        var activeDays = 0

        var cursor = startDate
        while (!cursor.isAfter(today)) {
            val count = mergedCounts[cursor] ?: 0
            visibleCounts[cursor] = count
            totalSubmissions += count
            if (count > 0) {
                activeDays += 1
            }
            cursor = cursor.plusDays(1)
        }

        var currentStreak = 0
        cursor = today
        while (!cursor.isBefore(startDate)) {
            if ((visibleCounts[cursor] ?: 0) <= 0) {
                break
            }
            currentStreak += 1
            cursor = cursor.minusDays(1)
        }

        return WidgetSnapshot(
            counts = visibleCounts,
            totalSubmissions = totalSubmissions,
            activeDays = activeDays,
            currentStreak = currentStreak,
            lastUpdatedMillis = System.currentTimeMillis(),
        )
    }

    @Throws(IOException::class, InvalidUsernameException::class)
    private fun fetchYear(username: String, year: Int): Map<LocalDate, Int> {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 15000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Origin", "https://leetcode.com")
            setRequestProperty("Referer", "https://leetcode.com/u/$username/")
            setRequestProperty("User-Agent", "LeetGraph Android Widget")
        }

        try {
            val payload = JSONObject()
                .put("query", calendarQuery.trimIndent())
                .put(
                    "variables",
                    JSONObject()
                        .put("username", username)
                        .put("year", year),
                )

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
            }

            val responseCode = connection.responseCode
            val stream =
                if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream ?: connection.inputStream
                }

            val response = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                buildString {
                    var line = reader.readLine()
                    while (line != null) {
                        append(line)
                        line = reader.readLine()
                    }
                }
            }

            if (responseCode !in 200..299) {
                throw IOException("LeetCode request failed with HTTP $responseCode")
            }

            val json = JSONObject(response)
            if (json.has("errors")) {
                val message = json.getJSONArray("errors").optJSONObject(0)?.optString("message")
                throw IOException(message ?: "LeetCode returned an error.")
            }

            val matchedUser = json
                .optJSONObject("data")
                ?.optJSONObject("matchedUser")
                ?: throw InvalidUsernameException(username)

            val submissionCalendar = matchedUser
                .optJSONObject("userCalendar")
                ?.optString("submissionCalendar")
                .orEmpty()

            if (submissionCalendar.isBlank()) {
                return emptyMap()
            }

            val result = mutableMapOf<LocalDate, Int>()
            val calendarJson = JSONObject(submissionCalendar)
            val keys = calendarJson.keys()

            while (keys.hasNext()) {
                val timestampKey = keys.next()
                val epochSeconds = timestampKey.toLongOrNull() ?: continue
                val date =
                    Instant.ofEpochSecond(epochSeconds)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()
                result[date] = calendarJson.optInt(timestampKey, 0)
            }

            return result
        } finally {
            connection.disconnect()
        }
    }
}

class InvalidUsernameException(username: String) :
    IllegalArgumentException("Could not find @$username on LeetCode.")
