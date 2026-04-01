package com.example.leetgraph

object LeetGraphContract {
    const val CHANNEL_NAME = "com.example.leetgraph/widget"
    const val PREFS_NAME = "leetgraph_widget"
    const val ACTION_REFRESH_WIDGET = "com.example.leetgraph.action.REFRESH_WIDGET"

    const val KEY_USERNAME = "username"
    const val KEY_GRAPH_JSON = "graph_json"
    const val KEY_TOTAL_SUBMISSIONS = "total_submissions"
    const val KEY_ACTIVE_DAYS = "active_days"
    const val KEY_CURRENT_STREAK = "current_streak"
    const val KEY_LAST_UPDATED = "last_updated"
    const val KEY_ERROR = "sync_error"

    const val MAX_WEEKS = 53
    const val MIN_WEEKS = 16
}
