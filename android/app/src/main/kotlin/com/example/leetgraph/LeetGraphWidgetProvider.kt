package com.example.leetgraph

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle

class LeetGraphWidgetProvider : AppWidgetProvider() {
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        LeetGraphSyncScheduler.ensurePeriodicRefresh(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        LeetGraphSyncScheduler.cancelAll(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        WidgetRenderer.renderWidgets(context, appWidgetManager, appWidgetIds)

        if (WidgetStorage.getUsername(context).isNotBlank()) {
            LeetGraphSyncScheduler.ensurePeriodicRefresh(context)
            LeetGraphSyncScheduler.enqueueImmediateRefresh(context)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        WidgetRenderer.renderWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == LeetGraphContract.ACTION_REFRESH_WIDGET) {
            LeetGraphSyncScheduler.enqueueImmediateRefresh(context)
        }
    }
}
