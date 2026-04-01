package com.example.leetgraph

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate

object WidgetStorage {
    fun saveUsername(context: Context, username: String) {
        val cleanUsername = username.trim()
        val prefs = prefs(context)
        val previousUsername = prefs.getString(LeetGraphContract.KEY_USERNAME, "").orEmpty()
        val editor = prefs.edit()

        editor.putString(LeetGraphContract.KEY_USERNAME, cleanUsername)
        editor.remove(LeetGraphContract.KEY_ERROR)

        if (previousUsername != cleanUsername) {
            editor.remove(LeetGraphContract.KEY_GRAPH_JSON)
            editor.remove(LeetGraphContract.KEY_TOTAL_SUBMISSIONS)
            editor.remove(LeetGraphContract.KEY_ACTIVE_DAYS)
            editor.remove(LeetGraphContract.KEY_CURRENT_STREAK)
            editor.remove(LeetGraphContract.KEY_LAST_UPDATED)
        }

        editor.apply()
    }

    fun getUsername(context: Context): String =
        prefs(context).getString(LeetGraphContract.KEY_USERNAME, "").orEmpty()

    fun saveSnapshot(context: Context, snapshot: WidgetSnapshot) {
        val graphJson = JSONObject()
        snapshot.counts.forEach { (date, count) ->
            graphJson.put(date.toString(), count)
        }

        prefs(context).edit()
            .putString(LeetGraphContract.KEY_GRAPH_JSON, graphJson.toString())
            .putInt(LeetGraphContract.KEY_TOTAL_SUBMISSIONS, snapshot.totalSubmissions)
            .putInt(LeetGraphContract.KEY_ACTIVE_DAYS, snapshot.activeDays)
            .putInt(LeetGraphContract.KEY_CURRENT_STREAK, snapshot.currentStreak)
            .putLong(LeetGraphContract.KEY_LAST_UPDATED, snapshot.lastUpdatedMillis)
            .remove(LeetGraphContract.KEY_ERROR)
            .apply()
    }

    fun clearSnapshot(context: Context) {
        prefs(context).edit()
            .remove(LeetGraphContract.KEY_GRAPH_JSON)
            .remove(LeetGraphContract.KEY_TOTAL_SUBMISSIONS)
            .remove(LeetGraphContract.KEY_ACTIVE_DAYS)
            .remove(LeetGraphContract.KEY_CURRENT_STREAK)
            .remove(LeetGraphContract.KEY_LAST_UPDATED)
            .apply()
    }

    fun saveError(context: Context, message: String) {
        prefs(context).edit()
            .putString(LeetGraphContract.KEY_ERROR, message)
            .apply()
    }

    fun getWidgetState(context: Context): StoredWidgetState {
        val prefs = prefs(context)
        val graphJson = prefs.getString(LeetGraphContract.KEY_GRAPH_JSON, null)
        val counts = mutableMapOf<LocalDate, Int>()

        if (!graphJson.isNullOrBlank()) {
            val json = JSONObject(graphJson)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val date = runCatching { LocalDate.parse(key) }.getOrNull() ?: continue
                counts[date] = json.optInt(key, 0)
            }
        }

        val hasSnapshot = counts.isNotEmpty() && prefs.contains(LeetGraphContract.KEY_LAST_UPDATED)
        val snapshot =
            if (!hasSnapshot) {
                null
            } else {
                WidgetSnapshot(
                    counts = counts,
                    totalSubmissions = prefs.getInt(LeetGraphContract.KEY_TOTAL_SUBMISSIONS, 0),
                    activeDays = prefs.getInt(LeetGraphContract.KEY_ACTIVE_DAYS, 0),
                    currentStreak = prefs.getInt(LeetGraphContract.KEY_CURRENT_STREAK, 0),
                    lastUpdatedMillis = prefs.getLong(LeetGraphContract.KEY_LAST_UPDATED, 0L),
                )
            }

        return StoredWidgetState(
            username = prefs.getString(LeetGraphContract.KEY_USERNAME, "").orEmpty(),
            snapshot = snapshot,
            errorMessage = prefs.getString(LeetGraphContract.KEY_ERROR, null),
        )
    }

    fun getStateMap(context: Context): Map<String, Any> {
        val state = getWidgetState(context)
        return buildMap {
            put("username", state.username)
            put("hasSnapshot", state.snapshot != null)
            put("totalSubmissions", state.snapshot?.totalSubmissions ?: 0)
            put("activeDays", state.snapshot?.activeDays ?: 0)
            put("currentStreak", state.snapshot?.currentStreak ?: 0)
            state.snapshot?.let { put("lastUpdatedMillis", it.lastUpdatedMillis) }
            state.errorMessage?.let { put("errorMessage", it) }
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(LeetGraphContract.PREFS_NAME, Context.MODE_PRIVATE)
}

data class WidgetSnapshot(
    val counts: Map<LocalDate, Int>,
    val totalSubmissions: Int,
    val activeDays: Int,
    val currentStreak: Int,
    val lastUpdatedMillis: Long,
)

data class StoredWidgetState(
    val username: String,
    val snapshot: WidgetSnapshot?,
    val errorMessage: String?,
)
