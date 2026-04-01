package com.example.leetgraph

import android.content.Context
import java.io.IOException

object LeetGraphSyncRunner {
    fun refreshNow(context: Context): SyncResult {
        val username = WidgetStorage.getUsername(context).trim()

        if (username.isBlank()) {
            WidgetStorage.saveError(context, "Set a LeetCode username in the app first.")
            WidgetRenderer.updateAllWidgets(context)
            return SyncResult(
                success = false,
                shouldRetry = false,
                message = "Set a LeetCode username in the app first.",
            )
        }

        return try {
            val snapshot = LeetCodeService.fetchSnapshot(username)
            WidgetStorage.saveSnapshot(context, snapshot)
            WidgetRenderer.updateAllWidgets(context)
            SyncResult(
                success = true,
                shouldRetry = false,
                message = "Synced @$username.",
            )
        } catch (_: InvalidUsernameException) {
            WidgetStorage.clearSnapshot(context)
            WidgetStorage.saveError(context, "Could not find @$username on LeetCode.")
            WidgetRenderer.updateAllWidgets(context)
            SyncResult(
                success = false,
                shouldRetry = false,
                message = "Could not find @$username on LeetCode.",
            )
        } catch (_: IOException) {
            WidgetStorage.saveError(context, "LeetCode is unreachable right now. Showing the last saved graph.")
            WidgetRenderer.updateAllWidgets(context)
            SyncResult(
                success = false,
                shouldRetry = true,
                message = "LeetCode is unreachable right now.",
            )
        } catch (_: Exception) {
            WidgetStorage.saveError(context, "Something went wrong while refreshing the widget.")
            WidgetRenderer.updateAllWidgets(context)
            SyncResult(
                success = false,
                shouldRetry = false,
                message = "Something went wrong while refreshing the widget.",
            )
        }
    }
}

data class SyncResult(
    val success: Boolean,
    val shouldRetry: Boolean,
    val message: String,
) {
    fun toMap(): Map<String, Any> =
        mapOf(
            "success" to success,
            "message" to message,
        )
}
