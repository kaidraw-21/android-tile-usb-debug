package com.snap.tiles.float

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.snap.tiles.R
import com.snap.tiles.data.PrefsManager
import com.snap.tiles.data.TileConfigRepo
import com.snap.tiles.executor.DynamicActionExecutor
import com.snap.tiles.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class FloatingTileService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: FrameLayout? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        addFloatingView()
        Log.d(TAG, "onCreate: floating view added")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_REFRESH -> refreshButtonState()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        overlayView = null
        ioScope.cancel()
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }

    // ── View setup ──────────────────────────────────────────────────────────

    private fun addFloatingView() {
        val density = resources.displayMetrics.density
        val sizePx = (BUTTON_SIZE_DP * density).toInt()
        val (savedX, savedY) = PrefsManager.getFloatPosition()

        val params = WindowManager.LayoutParams(
            sizePx, sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX
            y = savedY
        }
        overlayParams = params

        val view = buildButtonView(sizePx, density)
        overlayView = view
        windowManager.addView(view, params)
        refreshButtonState()
        attachTouchListener(view, params)
    }

    private fun buildButtonView(sizePx: Int, density: Float): FrameLayout {
        val frame = FrameLayout(this)

        val bg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(COLOR_INACTIVE_BG)
        }
        frame.background = bg
        frame.elevation = 8f * density

        val iconPx = (24 * density).toInt()
        val iconRes = FLOAT_ICONS.getOrElse(PrefsManager.getFloatIconIndex()) { FLOAT_ICONS[0] }
        val imageView = ImageView(this).apply {
            setImageResource(iconRes)
            setColorFilter(COLOR_INACTIVE_ICON)
        }
        frame.addView(imageView, FrameLayout.LayoutParams(iconPx, iconPx).apply {
            gravity = Gravity.CENTER
        })
        return frame
    }

    private fun attachTouchListener(view: FrameLayout, params: WindowManager.LayoutParams) {
        var downRawX = 0f
        var downRawY = 0f
        var lastRawX = 0f
        var lastRawY = 0f
        var wasDragging = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX; downRawY = event.rawY
                    lastRawX = event.rawX; lastRawY = event.rawY
                    wasDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - lastRawX
                    val dy = event.rawY - lastRawY
                    if (!wasDragging &&
                        (abs(event.rawX - downRawX) > DRAG_THRESHOLD || abs(event.rawY - downRawY) > DRAG_THRESHOLD)
                    ) {
                        wasDragging = true
                    }
                    if (wasDragging) {
                        params.x += dx.toInt()
                        params.y += dy.toInt()
                        runCatching { windowManager.updateViewLayout(view, params) }
                    }
                    lastRawX = event.rawX; lastRawY = event.rawY
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (wasDragging) {
                        PrefsManager.saveFloatPosition(params.x, params.y)
                    } else {
                        onTileTap()
                    }
                    true
                }
                else -> false
            }
        }
    }

    // ── Tile tap ─────────────────────────────────────────────────────────────

    private fun onTileTap() {
        val tileId = PrefsManager.getFloatTileId()
        Log.d(TAG, "onTileTap: tileId=$tileId")
        ioScope.launch {
            val resolver = contentResolver
            val currentOn = readTileState(tileId, resolver)
            val targetOn = !currentOn
            Log.d(TAG, "onTileTap: currentOn=$currentOn -> targetOn=$targetOn")

            // Optimistic UI flip — same pattern as FixedTileBase
            withContext(Dispatchers.Main) { applyButtonColor(targetOn) }

            // Execute toggle
            when {
                tileId.startsWith("FIXED_") -> {
                    val actionId = tileId.removePrefix("FIXED_")
                    DynamicActionExecutor.setState(actionId, resolver, targetOn)
                    TileConfigRepo.requestRefreshAffectedTiles(applicationContext, setOf(actionId))
                }
                tileId.startsWith("SLOT_") -> {
                    val slot = tileId.removePrefix("SLOT_").toIntOrNull() ?: return@launch
                    val ids = TileConfigRepo.get(slot).actionIds
                    if (ids.isEmpty()) return@launch
                    DynamicActionExecutor.toggleAll(ids, resolver, applicationContext, targetOn)
                }
            }
        }
    }

    // ── State refresh ────────────────────────────────────────────────────────

    fun refreshButtonState() {
        val view = overlayView ?: return
        ioScope.launch {
            val isOn = readTileState(PrefsManager.getFloatTileId(), contentResolver)
            withContext(Dispatchers.Main) { applyButtonColor(isOn) }
        }
    }

    private fun applyButtonColor(isOn: Boolean) {
        val view = overlayView ?: return
        (view.background as? GradientDrawable)?.setColor(if (isOn) COLOR_ACTIVE_BG else COLOR_INACTIVE_BG)
        (view.getChildAt(0) as? ImageView)?.setColorFilter(if (isOn) COLOR_ACTIVE_ICON else COLOR_INACTIVE_ICON)
    }

    private fun readTileState(tileId: String, resolver: ContentResolver): Boolean {
        return when {
            tileId.startsWith("FIXED_") -> {
                DynamicActionExecutor.getState(tileId.removePrefix("FIXED_"), resolver)
            }
            tileId.startsWith("SLOT_") -> {
                val slot = tileId.removePrefix("SLOT_").toIntOrNull() ?: return false
                val ids = TileConfigRepo.get(slot).actionIds
                ids.isNotEmpty() && ids.all { DynamicActionExecutor.getState(it, resolver) }
            }
            else -> false
        }
    }

    // ── Notification ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Floating Tile", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Keeps the floating tile button active"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Floating Tile Active")
            .setContentText("Tap to open Snap Tiles")
            .setSmallIcon(R.drawable.ic_tile_icon)
            .setContentIntent(tapIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val TAG = "FloatingTileService"
        const val CHANNEL_ID = "float_tile_channel"
        const val NOTIF_ID = 2001
        const val BUTTON_SIZE_DP = 56
        const val DRAG_THRESHOLD = 12f
        const val ACTION_REFRESH = "com.snap.tiles.FLOAT_REFRESH"

        // Colors from the app's Stitch theme (hardcoded to avoid TypedValue in service)
        const val COLOR_ACTIVE_BG = 0xFFFBD928.toInt()    // PrimaryContainer
        const val COLOR_ACTIVE_ICON = 0xFF6F5E00.toInt()  // OnPrimaryContainer
        const val COLOR_INACTIVE_BG = 0xFFE8E8E8.toInt()  // SurfaceContainerHigh
        const val COLOR_INACTIVE_ICON = 0xFF4C4733.toInt() // OnSurfaceVariant

        val FLOAT_ICONS = listOf(
            R.drawable.ic_tile_icon,
            R.drawable.ic_tile_usb_debug,
            R.drawable.ic_tile_dev_mode,
            R.drawable.ic_tile_accessibility,
            R.drawable.ic_tile_metal,
            R.drawable.ic_tile_wood,
            R.drawable.ic_tile_water,
            R.drawable.ic_tile_fire,
            R.drawable.ic_tile_earth,
        )

        fun start(context: Context) {
            context.startForegroundService(Intent(context, FloatingTileService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingTileService::class.java))
        }

        fun refresh(context: Context) {
            context.startService(Intent(context, FloatingTileService::class.java).apply {
                action = ACTION_REFRESH
            })
        }
    }
}
