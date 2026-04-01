package com.example.leetgraph

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

object WidgetRenderer {
    private const val minVisibleWeeks = 8
    private const val maxVisibleWeeks = 30

    private val levelColors =
        intArrayOf(
            Color.parseColor("#161B22"),
            Color.parseColor("#0E4429"),
            Color.parseColor("#006D32"),
            Color.parseColor("#26A641"),
            Color.parseColor("#39D353"),
        )

    fun updateAllWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, LeetGraphWidgetProvider::class.java)
        renderWidgets(context, manager, manager.getAppWidgetIds(componentName))
    }

    fun renderWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        if (appWidgetIds.isEmpty()) {
            return
        }

        appWidgetIds.forEach { widgetId ->
            renderWidget(context, appWidgetManager, widgetId)
        }
    }

    fun renderWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ) {
        val state = WidgetStorage.getWidgetState(context)
        val size = WidgetSize.from(context, appWidgetManager.getAppWidgetOptions(appWidgetId))
        val views = RemoteViews(context.packageName, R.layout.leet_graph_widget)

        views.setOnClickPendingIntent(R.id.widget_root, createOpenAppIntent(context))

        when {
            state.username.isBlank() -> renderInfoState(
                views = views,
                title = null,
                subtitle = "Add your LeetCode username in the app.",
                graph = createPlaceholderBitmap(size),
            )

            state.snapshot == null -> renderInfoState(
                views = views,
                title = null,
                subtitle = state.errorMessage ?: "Syncing your LeetCode graph...",
                graph = createPlaceholderBitmap(size),
            )

            else -> renderGraphState(
                views = views,
                graph = createGraphBitmap(size, state.snapshot),
            )
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun renderInfoState(
        views: RemoteViews,
        title: String?,
        subtitle: String,
        graph: Bitmap,
    ) {
        views.setViewVisibility(R.id.widget_info_panel, View.VISIBLE)
        views.setViewVisibility(
            R.id.widget_title,
            if (title.isNullOrBlank()) View.GONE else View.VISIBLE,
        )
        views.setTextViewText(R.id.widget_title, title.orEmpty())
        views.setTextViewText(R.id.widget_subtitle, subtitle)
        views.setImageViewBitmap(R.id.widget_graph, graph)
    }

    private fun renderGraphState(
        views: RemoteViews,
        graph: Bitmap,
    ) {
        views.setViewVisibility(R.id.widget_info_panel, View.GONE)
        views.setImageViewBitmap(R.id.widget_graph, graph)
    }

    private fun createOpenAppIntent(context: Context): PendingIntent {
        val intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        return PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createGraphBitmap(
        size: WidgetSize,
        snapshot: WidgetSnapshot,
    ): Bitmap {
        val today = LocalDate.now(ZoneOffset.UTC)
        val width = max(size.graphWidthPx, 1)
        val height = max(size.graphHeightPx, 1)
        val visibleWeeks = resolveVisibleWeeks(size)
        val cellSize = computeCellSize(width, height, visibleWeeks, size.gapPx)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val gridWidth = (visibleWeeks * cellSize) + ((visibleWeeks - 1) * size.gapPx)
        val gridHeight = (7 * cellSize) + (6 * size.gapPx)
        val insetX = max((width - gridWidth) / 2f, 0f)
        val insetY = max((height - gridHeight) / 2f, 0f)
        val radius = min(cellSize / 4.4f, 2.2f * size.density)
        val emptyPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = levelColors[0]
            }

        val maxCount = snapshot.counts.values.maxOrNull() ?: 0
        val latestWeekStart =
            today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val firstWeekStart = latestWeekStart.minusWeeks((visibleWeeks - 1).toLong())

        for (week in 0 until visibleWeeks) {
            val weekStart = firstWeekStart.plusWeeks(week.toLong())
            for (day in 0 until 7) {
                val date = weekStart.plusDays(day.toLong())
                if (date.isAfter(today)) {
                    continue
                }
                val count = snapshot.counts[date] ?: 0
                val fillPaint =
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = levelColors[resolveLevel(count, maxCount)]
                    }
                val left = insetX + week * (cellSize + size.gapPx).toFloat()
                val top = insetY + day * (cellSize + size.gapPx).toFloat()
                val rect = RectF(left, top, left + cellSize, top + cellSize)

                canvas.drawRoundRect(rect, radius, radius, emptyPaint)
                canvas.drawRoundRect(rect, radius, radius, fillPaint)
            }
        }

        return bitmap
    }

    private fun createPlaceholderBitmap(size: WidgetSize): Bitmap {
        val today = LocalDate.now(ZoneOffset.UTC)
        val width = max(size.graphWidthPx, 1)
        val height = max(size.graphHeightPx, 1)
        val visibleWeeks = resolveVisibleWeeks(size)
        val cellSize = computeCellSize(width, height, visibleWeeks, size.gapPx)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val gridWidth = (visibleWeeks * cellSize) + ((visibleWeeks - 1) * size.gapPx)
        val gridHeight = (7 * cellSize) + (6 * size.gapPx)
        val insetX = max((width - gridWidth) / 2f, 0f)
        val insetY = max((height - gridHeight) / 2f, 0f)
        val radius = min(cellSize / 4.4f, 2.2f * size.density)
        val basePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0A0A0A")
            }
        val accentPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#238636")
                alpha = 105
            }

        val latestWeekStart =
            today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val firstWeekStart = latestWeekStart.minusWeeks((visibleWeeks - 1).toLong())

        for (week in 0 until visibleWeeks) {
            val weekStart = firstWeekStart.plusWeeks(week.toLong())
            for (day in 0 until 7) {
                val date = weekStart.plusDays(day.toLong())
                if (date.isAfter(today)) {
                    continue
                }
                val left = insetX + week * (cellSize + size.gapPx).toFloat()
                val top = insetY + day * (cellSize + size.gapPx).toFloat()
                val rect = RectF(left, top, left + cellSize, top + cellSize)
                canvas.drawRoundRect(rect, radius, radius, basePaint)

                if ((week + day) % 13 == 0 || (week == visibleWeeks / 2 && day == 3)) {
                    canvas.drawRoundRect(rect, radius, radius, accentPaint)
                }
            }
        }

        return bitmap
    }

    private fun resolveVisibleWeeks(size: WidgetSize): Int {
        val preferredCellPx =
            when {
                size.widthDp < 170 -> max((7f * size.density).toInt(), 4)
                size.widthDp < 230 -> max((8f * size.density).toInt(), 5)
                size.widthDp < 320 -> max((9f * size.density).toInt(), 6)
                else -> max((10f * size.density).toInt(), 7)
            }
        val maxCellByHeight = max(computeHeightBoundCellSize(size.graphHeightPx, size.gapPx), 1)
        val targetCell = min(preferredCellPx, maxCellByHeight)
        val byWidth = max((size.graphWidthPx + size.gapPx) / (targetCell + size.gapPx), 1)
        return byWidth.coerceIn(minVisibleWeeks, min(maxVisibleWeeks, LeetGraphContract.MAX_WEEKS))
    }

    private fun computeHeightBoundCellSize(height: Int, gap: Int): Int {
        return floor((height - (6 * gap)).toDouble() / 7).toInt()
    }

    private fun computeCellSize(width: Int, height: Int, weeks: Int, gap: Int): Int {
        val widthCell = floor((width - ((weeks - 1) * gap)).toDouble() / weeks).toInt()
        val heightCell = computeHeightBoundCellSize(height, gap)
        return max(min(widthCell, heightCell), 1)
    }

    private fun resolveLevel(count: Int, maxCount: Int): Int {
        if (count <= 0 || maxCount <= 0) {
            return 0
        }

        val first = max(1, maxCount / 4)
        val second = max(2, maxCount / 2)
        val third = max(3, (maxCount * 3) / 4)

        return when {
            count >= third -> 4
            count >= second -> 3
            count >= first -> 2
            else -> 1
        }
    }
}

private data class WidgetSize(
    val widthDp: Int,
    val graphWidthPx: Int,
    val graphHeightPx: Int,
    val density: Float,
    val gapPx: Int,
) {
    companion object {
        fun from(context: Context, options: Bundle?): WidgetSize {
            val widthDp =
                options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                    ?.takeIf { it > 0 }
                    ?: 180
            val heightDp =
                options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
                    ?.takeIf { it > 0 }
                    ?: 120
            val density = context.resources.displayMetrics.density
            val outerPaddingPx = max((6f * density).toInt(), 4)
            val widthPx = max((widthDp * density).toInt(), 140)
            val heightPx = max((heightDp * density).toInt(), 96)

            return WidgetSize(
                widthDp = widthDp,
                graphWidthPx = max(widthPx - (outerPaddingPx * 2), 92),
                graphHeightPx = max(heightPx - (outerPaddingPx * 2), 64),
                density = density,
                gapPx = max((2f * density).toInt(), 1),
            )
        }
    }
}
