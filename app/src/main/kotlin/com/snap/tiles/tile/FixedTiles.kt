package com.snap.tiles.tile

import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.snap.tiles.data.Action
import com.snap.tiles.data.TileConfigRepo
import com.snap.tiles.data.remote.ActionRegistry
import com.snap.tiles.executor.DynamicActionExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Base for fixed single-action tiles.
 * Uses DynamicActionExecutor to read/write settings based on remote JSON config.
 */
abstract class FixedTileBase(
    private val action: Action
) : TileService() {

    private val actionId get() = action.name
    private val tag get() = "FixedTile[$actionId]"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

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

        scope.launch {
            DynamicActionExecutor.setState(actionId, contentResolver, targetOn)
            val affected = mutableSetOf(actionId)
            val remoteAction = ActionRegistry.getAction(actionId)
            if (remoteAction != null) {
                affected.addAll(remoteAction.safeDependencies)
                remoteAction.sideEffects?.safeOnEnable?.forEach { affected.add(it.key) }
                remoteAction.sideEffects?.safeOnDisable?.forEach { affected.add(it.key) }
            }
            mainHandler.post {
                TileConfigRepo.requestRefreshAffectedTiles(applicationContext, affected)
            }
        }
    }

    private fun refreshTile() {
        val isOn = DynamicActionExecutor.getState(actionId, contentResolver)
        if (actionId == "ACCESSIBILITY" && isOn) {
            DynamicActionExecutor.refreshA11yCache(contentResolver)
        }
        // Use remote label if available, fallback to string resource
        val remoteAction = ActionRegistry.getAction(actionId)
        val label = remoteAction?.label ?: getString(action.labelRes)
        Log.d(tag, "refreshTile(isOn=$isOn)")
        qsTile?.apply {
            state = if (isOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            this.label = label
            updateTile()
        }
    }
}

/** Fixed tile: USB Debugging */
class UsbDebugTile : FixedTileBase(Action.USB_DEBUGGING)

/** Fixed tile: Developer Mode */
class DevModeTile : FixedTileBase(Action.DEVELOPER_MODE)

/** Fixed tile: Accessibility Services */
class AccessibilityTile : FixedTileBase(Action.ACCESSIBILITY)
