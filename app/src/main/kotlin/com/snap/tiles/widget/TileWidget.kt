package com.snap.tiles.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.snap.tiles.R
import com.snap.tiles.data.PrefsManager
import com.snap.tiles.data.TileConfigRepo
import com.snap.tiles.executor.DynamicActionExecutor
import com.snap.tiles.ui.buildTileIconBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TileWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it) }
    }

    override fun onDeleted(context: Context, widgetIds: IntArray) {
        widgetIds.forEach { PrefsManager.deleteWidgetConfig(it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE -> {
                val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val result = goAsync()
                    scope.launch {
                        try { handleToggle(context, widgetId) }
                        finally { result.finish() }
                    }
                }
            }
            ACTION_CONFIGURE -> {
                val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val tileId = intent.getStringExtra(EXTRA_TILE_ID) ?: return
                    val label = intent.getStringExtra(EXTRA_LABEL) ?: return
                    val iconRes = intent.getIntExtra(EXTRA_ICON_RES, 0).takeIf { it != 0 } ?: return
                    PrefsManager.saveWidgetConfig(widgetId, tileId, label, iconRes)
                    updateWidget(context, AppWidgetManager.getInstance(context), widgetId)
                }
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.snap.tiles.WIDGET_TOGGLE"
        const val ACTION_CONFIGURE = "com.snap.tiles.WIDGET_CONFIGURE"
        const val EXTRA_TILE_ID = "tile_id"
        const val EXTRA_LABEL = "tile_label"
        const val EXTRA_ICON_RES = "tile_icon_res"

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val (tileId, label, iconRes) = PrefsManager.getWidgetConfig(widgetId) ?: return
            val isOn = getTileState(context, tileId)
            val views = buildViews(context, widgetId, tileId, label, iconRes, isOn)
            manager.updateAppWidget(widgetId, views)
        }

        private fun getTileState(context: Context, tileId: String): Boolean {
            return when {
                tileId.startsWith("FIXED_") -> {
                    val actionId = tileId.removePrefix("FIXED_")
                    DynamicActionExecutor.getState(actionId, context.contentResolver)
                }
                tileId.startsWith("SLOT_") -> {
                    val slotIndex = tileId.removePrefix("SLOT_").toIntOrNull() ?: return false
                    val config = TileConfigRepo.get(slotIndex)
                    config.actionIds.isNotEmpty() &&
                        config.actionIds.all { DynamicActionExecutor.getState(it, context.contentResolver) }
                }
                else -> false
            }
        }

        private fun buildViews(
            context: Context, widgetId: Int,
            tileId: String, label: String, iconRes: Int, isOn: Boolean
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_tile)

            // Icon bitmap
            views.setImageViewBitmap(R.id.widget_icon, buildTileIconBitmap(context, iconRes))

            // Label
            views.setTextViewText(R.id.widget_label, label)

            // State indicator
            if (isOn) {
                views.setTextViewText(R.id.widget_state, "● ON")
                views.setTextColor(R.id.widget_state, Color.parseColor("#2E7D32"))
            } else {
                views.setTextViewText(R.id.widget_state, "○ OFF")
                views.setTextColor(R.id.widget_state, Color.parseColor("#9E9E9E"))
            }

            // Click to toggle
            val toggleIntent = Intent(context, TileWidget::class.java).apply {
                action = ACTION_TOGGLE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            val pi = PendingIntent.getBroadcast(
                context, widgetId, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pi)

            return views
        }

        private suspend fun handleToggle(context: Context, widgetId: Int) {
            val (tileId, _, _) = PrefsManager.getWidgetConfig(widgetId) ?: return
            val actionIds = when {
                tileId.startsWith("FIXED_") -> listOf(tileId.removePrefix("FIXED_"))
                tileId.startsWith("SLOT_") -> {
                    val slotIndex = tileId.removePrefix("SLOT_").toIntOrNull() ?: return
                    TileConfigRepo.get(slotIndex).actionIds
                }
                else -> return
            }
            if (actionIds.isEmpty()) return
            val isOn = actionIds.all { DynamicActionExecutor.getState(it, context.contentResolver) }
            DynamicActionExecutor.toggleAll(actionIds, context.contentResolver, context, !isOn)
            // Update widget on main thread
            val manager = AppWidgetManager.getInstance(context)
            updateWidget(context, manager, widgetId)
        }
    }
}
