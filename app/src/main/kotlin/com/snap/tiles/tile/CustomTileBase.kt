package com.snap.tiles.tile

import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.snap.tiles.data.TileConfigRepo
import com.snap.tiles.executor.DynamicActionExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

abstract class CustomTileBase(private val slotIndex: Int) : TileService() {

    private val tag get() = "CustomTile$slotIndex"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "onCreate()")
    }

    override fun onStartListening() {
        Log.d(tag, "onStartListening()")
        refreshTile()
    }

    override fun onClick() {
        Log.d(tag, "onClick()")
        val currentState = qsTile?.state ?: return
        val targetOn = currentState != Tile.STATE_ACTIVE
        Log.d(tag, "onClick: currentState=$currentState, targetOn=$targetOn")

        // Optimistic UI — flip immediately
        qsTile?.apply {
            state = if (targetOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }

        val actionIds = TileConfigRepo.get(slotIndex).actionIds
        scope.launch {
            DynamicActionExecutor.toggleAll(actionIds, contentResolver, applicationContext, targetOn)
        }
    }

    private fun refreshTile() {
        val config = TileConfigRepo.get(slotIndex)
        val actionIds = config.actionIds
        val allOn = actionIds.isNotEmpty() && actionIds.all {
            DynamicActionExecutor.getState(it, contentResolver)
        }
        // Keep a11y cache fresh while services are active
        if (actionIds.contains("ACCESSIBILITY") && allOn) {
            DynamicActionExecutor.refreshA11yCache(contentResolver)
        }
        Log.d(tag, "refreshTile(label=${config.label}, allOn=$allOn)")
        qsTile?.apply {
            state = if (allOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = config.label
            updateTile()
        }
    }
}

// Five Elements custom tiles
class TileKim : CustomTileBase(1)   // 金 Metal
class TileMoc : CustomTileBase(2)   // 木 Wood
class TileThuy : CustomTileBase(3)  // 水 (Water
class TileHoa : CustomTileBase(4)   // 火 Fire
class TileTho : CustomTileBase(5)   // 土 Earth
