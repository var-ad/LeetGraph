package com.example.leetgraph

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Handler
import android.os.Looper
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : FlutterActivity() {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            LeetGraphContract.CHANNEL_NAME,
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "getWidgetState" -> {
                    result.success(WidgetStorage.getStateMap(applicationContext))
                }

                "saveUsername" -> {
                    val username = call.argument<String>("username")?.trim().orEmpty()
                    if (username.isBlank()) {
                        result.error(
                            "invalid_username",
                            "A LeetCode username is required.",
                            null,
                        )
                        return@setMethodCallHandler
                    }

                    WidgetStorage.saveUsername(applicationContext, username)
                    LeetGraphSyncScheduler.ensurePeriodicRefresh(applicationContext)
                    WidgetRenderer.updateAllWidgets(applicationContext)
                    result.success(null)
                }

                "refreshWidget" -> {
                    executor.execute {
                        val syncResult = LeetGraphSyncRunner.refreshNow(applicationContext)
                        mainHandler.post {
                            result.success(syncResult.toMap())
                        }
                    }
                }

                "requestPinWidget" -> {
                    val manager = AppWidgetManager.getInstance(applicationContext)
                    val componentName =
                        ComponentName(applicationContext, LeetGraphWidgetProvider::class.java)
                    val supported =
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                            manager.isRequestPinAppWidgetSupported

                    if (!supported) {
                        result.success(false)
                        return@setMethodCallHandler
                    }

                    result.success(manager.requestPinAppWidget(componentName, null, null))
                }

                else -> result.notImplemented()
            }
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}
